package com.examly.springapp.service;

import com.examly.springapp.dto.AuditVerificationReport;
import com.examly.springapp.model.AuditChainHead;
import com.examly.springapp.model.AuditLog;
import com.examly.springapp.repository.AuditChainHeadRepository;
import com.examly.springapp.repository.AuditLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Transactional SHA-256 cryptographic hash-chain engine guaranteeing non-repudiation
 * and tamper-evident logging for EPAMS lifecycle and security mutations.
 */
@Service
public class AuditChainService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    public static final Long GENESIS_SEQUENCE = 0L;
    public static final Long HEAD_ROW_ID = 1L;

    private final AuditLogRepo auditLogRepo;
    private final AuditChainHeadRepository auditChainHeadRepository;
    private final Clock clock;

    @Value("${epams.audit.allow-chain-reset:false}")
    private boolean allowChainReset;

    @Autowired
    public AuditChainService(AuditLogRepo auditLogRepo, 
                             AuditChainHeadRepository auditChainHeadRepository, 
                             Clock clock) {
        this.auditLogRepo = auditLogRepo;
        this.auditChainHeadRepository = auditChainHeadRepository;
        this.clock = clock;
    }

    /**
     * Synchronously records a cryptographically chained audit event within the active transaction.
     * Uses pessimistic row locking (SELECT ... FOR UPDATE) on the singleton audit_chain_head row (id=1)
     * to eliminate concurrency races and ensure contiguous, monotonic sequence allocation.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public AuditLog recordChainedEvent(String actorUsername, String action, String targetUsername, String details) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        // 1. Acquire exclusive lock on singleton chain head row
        AuditChainHead head = auditChainHeadRepository.findByIdForUpdate(HEAD_ROW_ID)
                .orElseGet(() -> {
                    AuditChainHead genesis = new AuditChainHead(HEAD_ROW_ID, GENESIS_HASH, GENESIS_SEQUENCE, now);
                    return auditChainHeadRepository.saveAndFlush(genesis);
                });

        Long nextSequence = head.getLastSequence() + 1;
        String prevHash = head.getCurrentHash();

        // 2. Compute canonical SHA-256 hash for entry n
        String currentHash = computeHash(prevHash, nextSequence, actorUsername, action, targetUsername, details, now);

        // 3. Persist audit log record
        AuditLog entry = new AuditLog(actorUsername, action, targetUsername, details, nextSequence, prevHash, currentHash, now);
        AuditLog savedEntry = auditLogRepo.saveAndFlush(entry);

        // 4. Update chain head state
        head.setLastSequence(nextSequence);
        head.setCurrentHash(currentHash);
        head.setUpdatedAt(now);
        auditChainHeadRepository.saveAndFlush(head);

        return savedEntry;
    }

    /**
     * Resets the cryptographic audit chain back to genesis state.
     * Strictly gated behind epams.audit.allow-chain-reset=true (test-only property)
     * so production runtime can NEVER destructively erase audit history.
     */
    @Transactional
    public void resetChainToGenesis() {
        if (!allowChainReset) {
            throw new UnsupportedOperationException(
                    "Destructive audit chain reset is strictly prohibited outside authorized test environments.");
        }
        AuditChainHead head = auditChainHeadRepository.findById(HEAD_ROW_ID).orElse(null);
        if (head != null) {
            head.setLastSequence(GENESIS_SEQUENCE);
            head.setCurrentHash(GENESIS_HASH);
            head.setUpdatedAt(LocalDateTime.now(clock));
            auditChainHeadRepository.saveAndFlush(head);
        }
        auditLogRepo.deleteBySequenceNumberIsNotNull();
    }

    /**
     * Verifies the complete cryptographic integrity of the hash chain from genesis to head.
     * Detects sequence gaps, duplicate sequences, altered payloads, and broken parent linkages.
     */
    @Transactional(readOnly = true)
    public AuditVerificationReport verifyChain() {
        List<AuditLog> chainedLogs = auditLogRepo.findBySequenceNumberIsNotNullOrderBySequenceNumberAsc();
        List<AuditLog> unchainedLogs = auditLogRepo.findBySequenceNumberIsNullOrderByTimestampAsc();
        AuditChainHead head = auditChainHeadRepository.findById(HEAD_ROW_ID).orElse(null);

        List<String> violations = new ArrayList<>();

        if (head == null) {
            violations.add("CRITICAL: Audit chain head (id=1) is missing from database.");
            return new AuditVerificationReport(false, chainedLogs.size(), unchainedLogs.size(), 0L, null, violations);
        }

        String expectedPrevHash = GENESIS_HASH;
        long expectedSequence = 1L;

        for (AuditLog log : chainedLogs) {
            Long seq = log.getSequenceNumber();

            // 1. Verify monotonic gapless sequence
            if (seq == null || seq != expectedSequence) {
                violations.add(String.format("Sequence gap or duplicate detected! Expected sequence [%d] but found [%s] on record ID [%d].",
                        expectedSequence, seq, log.getId()));
            }

            // 2. Verify previous hash linkage
            if (!expectedPrevHash.equalsIgnoreCase(log.getPreviousHash())) {
                violations.add(String.format("Hash linkage broken at sequence [%d]. Expected prevHash [%s] but found [%s].",
                        seq, expectedPrevHash, log.getPreviousHash()));
            }

            // 3. Verify entry hash integrity
            String computedHash = computeHash(log.getPreviousHash(), log.getSequenceNumber(),
                    log.getActorUsername(), log.getAction(), log.getTargetUsername(), log.getDetails(), log.getTimestamp());

            if (!computedHash.equalsIgnoreCase(log.getCurrentHash())) {
                violations.add(String.format("Tamper detected at sequence [%d] (Record ID [%d])! Recomputed hash [%s] does not match stored hash [%s].",
                        seq, log.getId(), computedHash, log.getCurrentHash()));
            }

            expectedPrevHash = log.getCurrentHash();
            expectedSequence++;
        }

        long finalChainedCount = chainedLogs.size();

        // 4. Verify chain head consistency
        if (head.getLastSequence() != finalChainedCount) {
            violations.add(String.format("Chain head sequence mismatch! Head reports lastSequence [%d] but verified records count is [%d].",
                    head.getLastSequence(), finalChainedCount));
        }

        if (finalChainedCount > 0 && !head.getCurrentHash().equalsIgnoreCase(expectedPrevHash)) {
            violations.add(String.format("Chain head hash mismatch! Head reports currentHash [%s] but final block hash is [%s].",
                    head.getCurrentHash(), expectedPrevHash));
        }

        boolean isValid = violations.isEmpty();
        return new AuditVerificationReport(isValid, finalChainedCount, unchainedLogs.size(),
                head.getLastSequence(), head.getCurrentHash(), violations);
    }

    /**
     * Deterministic canonical SHA-256 hash generator over canonical block components.
     */
    public static String computeHash(String prevHash, Long sequence, String actor, String action,
                                     String target, String details, LocalDateTime timestamp) {
        String canonical = String.format("%s|%d|%s|%s|%s|%s|%s",
                prevHash != null ? prevHash.trim().toLowerCase() : "",
                sequence != null ? sequence : 0L,
                actor != null ? actor.trim() : "",
                action != null ? action.trim() : "",
                target != null ? target.trim() : "",
                details != null ? details.trim() : "",
                timestamp != null ? timestamp.truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString() : "");

        return sha256Hex(canonical);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in runtime", e);
        }
    }
}

