# EPAMS 2.0 — Formal R1 User Decision Record

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1.1 — Customer Validation Execution (Decision Governance Record)  
**Decision Date:** 2026-11-18  
**Git Baseline:** `6205bd5a9bc06a6dafbb259f73e8215325537f2e` (`main`)  
**Authority:** Explicit user review and approval of R1 strategic decisions.  
**Implementation Authorization:** **NONE.** No R2 architecture freeze, no feature coding, no schema changes, and no P10 deployment are authorized by this record.

---

## 1. Summary of Formal User Decisions

| Decision ID | Topic | Approved Status | Implementation Authorized? | Validation Required Before R2? |
| :--- | :--- | :---: | :---: | :---: |
| **`R1-DEC-001`** | Product Positioning (`EPAMS — Lightweight, Self-Hosted, Governance-First Employee Service & Workflow Platform`) | **`APPROVED — PROVISIONAL`** | No | **Yes** (subject to R1.1 customer validation) |
| **`R1-DEC-002`** | Target Segment (`100`–`1,000` employees; initial sweet spot `150`–`750`) | **`APPROVED — HYPOTHESIS TO VALIDATE`** | No | **Yes** (demand, authority, budget, readiness unvalidated) |
| **`R1-DEC-003`** | Top 3 Strategic Initiatives (Priority 1–3) | **`APPROVED — PROVISIONAL`** | No | **Yes** (strategic priorities only; no coding authorized) |
| **`R1-DEC-007`** | Mandatory Customer Validation Gate Before Full R2 | **`APPROVED`** | N/A (Governance Gate) | **Mandatory Gate** — cannot be bypassed |

*Note on Other R1 Decisions (`R1-DEC-004`, `R1-DEC-005`, `R1-DEC-006`, `R1-DEC-008`):* The user did **not** issue explicit approval on workflow engine implementation architecture (`R1-DEC-004`), formal non-goal rejections (`R1-DEC-005`), Vite/monolith architectural confirmation (`R1-DEC-006`, though Vite migration is noted as a separate engineering maintenance activity under `R1-DEC-003`), or commercial packaging (`R1-DEC-008`). Those decisions remain **`PROPOSED`** or **`DEFERRED`** in [R1-DECISION-REGISTER.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-strategy/R1-DECISION-REGISTER.md) and are NOT silently promoted.

---

## 2. Detailed Decision Records & Roadmap Implications

### 2.1 `R1-DEC-001` — Product Positioning (`APPROVED — PROVISIONAL`)
- **Approved Positioning:** **EPAMS — Lightweight, Self-Hosted, Governance-First Employee Service & Workflow Platform.**
- **Core Positioning Hypothesis:** A self-hosted platform for managing employee services and cross-department workflows, with transparent governance and simpler deployment.
- **Explicit Distinction (Strategy vs. Evidence):** This positioning is **approved provisionally as the hypothesis to test** during R1.1, NOT established as validated product-market fit. Marketing claims such as "deploy in minutes" or "zero compliance risk" are strictly prohibited in customer concept testing.
- **Implications:** All R1.1 concept tests, competitor switching questions, and buyer evaluations will test whether self-hosted simplicity + transparent governance + cross-department workflows resonate enough to drive switching or adoption. If customer evidence contradicts self-hosted demand, positioning will be revised prior to R2.

### 2.2 `R1-DEC-002` — Target Segment (`APPROVED — HYPOTHESIS TO VALIDATE`)
- **Approved Hypothesis:**
  - Organisations with **`100`–`1,000` employees** (initial sweet spot **`150`–`750` employees**).
  - Potential focus on **privacy-sensitive organisations**.
  - Potential buyers: **Head of IT**, **Systems Administrator**, **VP Operations**.
  - Potential co-sponsors: **HR Operations** and **Information Security**.
- **Unvalidated Questions Required in R1.1:** Does this segment possess sufficient (1) recurring pain intensity, (2) buying authority across IT/HR/Ops, (3) allocated software budget, and (4) self-hosted adoption readiness?
- **Implications:** Participant recruitment (`r1.1-participant-recruitment-plan.md`) must stratify organisations across `100–149`, `150–300`, `301–750`, and `751–1,000` employee bands and across all 6 stakeholder roles to empirically test whether the `150–750` band is the true sweet spot.

### 2.3 `R1-DEC-003` — Strategic Initiatives (`APPROVED — PROVISIONAL`)
- **Approved Provisional Strategic Priorities (Zero Coding Authorized):**
  1. **Priority 1 — Self-Hosted Adoption Foundation & Knowledge-First Self-Service:** Service Catalog administration, Knowledge Base, role-scoped knowledge access, search and self-service discovery, OIDC SSO, secure self-hosted packaging, and Vite migration tracked strictly as a **separate engineering maintenance activity**.
  2. **Priority 2 — Configurable Cross-Department Workflow Engine:** Declarative workflow definitions, conditions and controlled transitions, allowlisted actions, approval orchestration, dry-run simulation, versioning and version-pinned execution, durable execution history.
  3. **Priority 3 — Employee Lifecycle Orchestration:** Onboarding, department transfers, offboarding, cross-department task dependencies, approval-gated access revocation, blocker and progress visibility.
- **Implications:** Neutral concept-testing cards (`r1.1-solution-concept-testing.md`) will evaluate buyer/user demand, must-have vs. nice-to-have scope, and adoption barriers for all three initiatives without claiming any of them are already implemented.

### 2.4 `R1-DEC-007` — Mandatory Validation Gate (`APPROVED`)
- **Approved Governance Rule:** **Actual customer validation must occur before full R2 architecture and SRS work.**
- **Implications:** R2 (`docs/epams-2.0/` SRS, HLD, LLD, database design) is **strictly blocked** until real customer interviews (`18`–`24` target) and supporting survey data (`50`–`100` secondary target) are collected, logged in `r1.1-validation-evidence-register.md`, scored honestly against `r1.1-hypothesis-validation-scorecard.md`, and explicitly reviewed against `r1.1-r2-entry-criteria.md` for user sign-off.
