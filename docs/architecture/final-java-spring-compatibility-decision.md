# EPAMS — Final Java 25 & Spring Boot Compatibility Decision (`COMPAT-01`)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Finding ID:** `COMPAT-01`  
**Date:** 2026-11-18  
**Decision:** **Controlled Temporary Baseline Documented — Major Framework Upgrade Scheduled Post-Freeze (Not Performed Unrelatedly in P7–P9)**  

---

## 1. Verified Component Compatibility Matrix

Extracted directly from `springapp/pom.xml`, `spring-boot-dependencies-3.4.12.pom`, and local JDK `25.0.2`:

| Component | Current Verified Version | Candidate Java-25-Certified Line | Compatibility Evidence & Risk Analysis |
| :--- | :--- | :--- | :--- |
| **Java Runtime / Compiler** | OpenJDK `25.0.2` (`--release 25`) | OpenJDK `25 LTS` (or `21 LTS` fallback) | Compiles all 126 main + 18 test classes cleanly with `javac --release 25`. |
| **Spring Boot** | `3.4.12` | `3.5.x` (Java 25 support added in 3.5.x+) / `4.0.x` | Spring Boot `3.4.x` officially declares Java 17–23 support. **Not** claimed officially compatible with Java 25 merely because local tests pass. |
| **Spring Framework** | `6.2.14` | `6.2.x` / `7.0.x` | ASM/bytecode scanning works on Java 25 locally; official JDK 25 support finalized in newer 6.2.x/7.x maintenance lines. |
| **Spring Security** | `6.4.13` | `6.5.x` / `7.0.x` | Stateless JWT, `SpaCsrfTokenRequestHandler`, `NullAuthenticatedSessionStrategy`, and `@EnableMethodSecurity` verified green. |
| **Hibernate ORM** | `6.6.36.Final` | `6.6.x` / `7.x` | `ddl-auto=validate`, JPQL projections, `@Version`, and `FOR UPDATE` pass locally. |
| **Byte Buddy** | `1.15.11` | `1.17.x+` | **Key Technical Caveat:** Byte Buddy `1.15.11` predates official JDK 25 class-file version 69 support (added in `1.17.x`); works for current Hibernate proxies/Mockito mocks locally but risks proxy generation warnings/failures if advanced JDK 25 bytecode features are introduced. |
| **Flyway** | `10.20.1` | `10.20+` / `11.x` | V1–V4 migrations verified cleanly on H2; live MySQL 8+ remains `BLOCKED` (`BLK-01`). |
| **JJWT** | `0.11.5` | `0.12.x` | Pure Java HMAC-SHA256 signing/verification; no JDK 25 reflection breaks observed. |
| **Tomcat / JUnit / Mockito** | `10.1.49` / `5.11.4` / `5.14.2` | Managed by Spring Boot `3.5.x+` | Live `RANDOM_PORT` Tomcat & 208 JUnit tests pass locally. |

---

## 2. Engineering Decision & Rationale

1. **Do NOT claim Java 25 + Spring Boot 3.4.12 is officially supported.** It is explicitly recorded as an **unendorsed temporary runtime combination**.
2. **Do NOT perform a blind major Spring Boot upgrade in this offline freeze:** Upgrading to Spring Boot `3.5.x` / `4.0.x` requires online Maven artifact resolution (blocked by sandbox) and coordinated re-verification of Spring Security 6.5/7, Hibernate, and Byte Buddy `1.17.x+`.
3. **Controlled Upgrade Path (Scheduled Pre-Production / P10+):**
   - **Option A (Recommended for Java 25 LTS):** Upgrade `spring-boot-starter-parent` to the latest `3.5.x` maintenance release (bringing Byte Buddy `1.17.x+` and official Java 25 support) on an isolated upgrade branch and re-run the full 208-test backend + 34-test frontend + Playwright + MySQL 8+ suites.
   - **Option B (Conservative Production Fallback):** Pin container base image in P10 to `eclipse-temurin:21-jre` (`<java.version>21</java.version>`) if deploying on Spring Boot `3.4.12`, giving immediate 100% official vendor support with zero code changes.
