package com.examly.springapp.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditVerificationReport {

    private boolean valid;
    private long chainedRecordsCount;
    private long unchainedLegacyCount;
    private long headSequence;
    private String headHash;
    private List<String> violations = new ArrayList<>();
    private LocalDateTime verifiedAt;

    public AuditVerificationReport() {
        this.verifiedAt = LocalDateTime.now();
    }

    public AuditVerificationReport(boolean valid, long chainedRecordsCount, long unchainedLegacyCount,
                                   long headSequence, String headHash, List<String> violations) {
        this.valid = valid;
        this.chainedRecordsCount = chainedRecordsCount;
        this.unchainedLegacyCount = unchainedLegacyCount;
        this.headSequence = headSequence;
        this.headHash = headHash;
        this.violations = violations != null ? violations : new ArrayList<>();
        this.verifiedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public long getChainedRecordsCount() {
        return chainedRecordsCount;
    }

    public void setChainedRecordsCount(long chainedRecordsCount) {
        this.chainedRecordsCount = chainedRecordsCount;
    }

    public long getUnchainedLegacyCount() {
        return unchainedLegacyCount;
    }

    public void setUnchainedLegacyCount(long unchainedLegacyCount) {
        this.unchainedLegacyCount = unchainedLegacyCount;
    }

    public long getHeadSequence() {
        return headSequence;
    }

    public void setHeadSequence(long headSequence) {
        this.headSequence = headSequence;
    }

    public String getHeadHash() {
        return headHash;
    }

    public void setHeadHash(String headHash) {
        this.headHash = headHash;
    }

    public List<String> getViolations() {
        return violations;
    }

    public void setViolations(List<String> violations) {
        this.violations = violations;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}

