# ADR-016: Audit Log Hash-Chain Schema Strategy & Legacy Coexistence

## Status
**Approved Architectural Decision** (Phase 2 / Task 07B.1)

## Context
EPAMS requires tamper-evident, non-repudiable audit trails across all authentication, profile lifecycle, and service management events. In `ADR-011` and `audit-architecture.md`, a cryptographic hash chaining scheme was approved:
$$H_n = \text{SHA256}(H_{n-1} \parallel \text{Seq}_n \parallel \text{PayloadHash}_n)$$

However, the pre-existing system already possessed an operational `audit_log` table storing historical audit records (`id`, `actor_username`, `action`, `target_username`, `details`, `timestamp`) generated prior to the introduction of cryptographic verification.

We must establish how the approved cryptographic audit architecture is represented in the relational database without:
1. Destroying or retroactively modifying existing legacy audit records.
2. Fabricating historical hashes or claiming legacy records are cryptographically verified.
3. Breaking the existing 99 IAM regression tests and `AuditLog.java` JPA operations.

---

## Evaluation of Architectural Options

### Option A: Extend Existing `audit_log` Table with Nullable Chaining Fields (SELECTED)
- In migration `V3__approvals_and_audit_chain.sql`, alter `audit_log` to add:
  - `sequence_number BIGINT NULL` (with `uk_audit_log_sequence UNIQUE`)
  - `previous_hash CHAR(64) NULL`
  - `current_hash CHAR(64) NULL` (with `idx_audit_log_curr_hash`)
- **Legacy Record Treatment:** Pre-existing audit records retain `NULL` for `sequence_number`, `previous_hash`, and `current_hash`. Because in relational SQL standard and MySQL `NULL != NULL`, multiple rows can safely hold `NULL` in a unique column without violating the unique constraint.
- **Forensic Truth:** Legacy events are truthfully documented as unchained historical entries recorded before hash-chain enablement. No synthetic hashes are fabricated.
- **Unified Querying:** Security auditors and compliance officers query a single table (`audit_log`) for all historical and cryptographically chained events.
- **JPA & Test Compatibility:** Existing JPA `AuditLog.java` entity maps the legacy fields. Hibernate `ddl-auto=validate` ignores additional nullable table columns, allowing all 24 existing `AuditLogTests` to pass without modification.

### Option B: Introduce Separate `audit_event` Table
- Retains `audit_log` as a frozen legacy log and introduces a new table `audit_event` strictly for hash-chained events.
- **Drawbacks:** Requires application code, repositories, and UI log viewers to query and paginate across two distinct tables with different schemas; complicates privilege management (`epams_app` permissions); diverges from approved `audit-architecture.md` which specifically references `audit_log`.

---

## Decision

We formally adopt **Option A**: Extending the existing `audit_log` table in `V3__approvals_and_audit_chain.sql`.

### Technical Specifications:

1. **Legacy Record Treatment:**
   - Existing legacy records in `audit_log` are preserved 100% untouched.
   - They retain `NULL` for `sequence_number`, `previous_hash`, and `current_hash`.
   - The integrity verification engine treats entries with `sequence_number IS NULL` as legacy unverified records, beginning the mathematical chain validation exclusively from `sequence_number = 1`.

2. **New Event Sequence Allocation:**
   - New audit events acquire their sequence number from the singleton state anchor table `audit_chain_head` (`id = 1`).
   - The allocating transaction executes:
     `SELECT current_hash, last_sequence FROM audit_chain_head WHERE id = 1 FOR UPDATE;`
   - Next sequence = `last_sequence + 1`.

3. **Previous / Current Hash Storage:**
   - `previous_hash`: Exactly 64 hexadecimal characters (`CHAR(64)`), referencing `audit_chain_head.current_hash`.
   - `current_hash`: Exactly 64 hexadecimal characters (`CHAR(64)`), computed as `SHA256(previous_hash + sequence_number + payload_hash)`.

4. **Genesis Anchor Relationship:**
   - `audit_chain_head` is seeded in `V3` with `id = 1`, `last_sequence = 0`, and genesis hash:
     `0000000000000000000000000000000000000000000000000000000000000000` (64 zeros).
   - Event 1 binds to this genesis hash as its `previous_hash`.

5. **Transactional Consistency Requirements:**
   - Direct synchronous persistence inline with domain mutations (in the exact same ACID transactional boundary).
   - If audit insert or hash computation fails, the entire business transaction rolls back.

6. **Indexes & Constraints:**
   - `CONSTRAINT uk_audit_log_sequence UNIQUE (sequence_number)`: Enforces gapless, fork-free sequencing.
   - `CREATE INDEX idx_audit_log_curr_hash ON audit_log (current_hash)`: Accelerates forensic verification lookups.
   - `CREATE INDEX idx_audit_target_user ON audit_log (target_username)` and `idx_audit_action` preserved from `V1`.

---

## Consequences

- **Positive:** Complete preservation of all historical data; zero test regressions; exact compliance with `audit-architecture.md`; mathematical tamper evidence for all new events.
- **Negative:** Schema contains nullable columns for legacy rows, requiring verification queries to filter on `sequence_number IS NOT NULL` when calculating hash continuity.

