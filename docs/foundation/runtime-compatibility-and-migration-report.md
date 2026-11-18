# EPAMS — Runtime Compatibility & Controlled Migration Report (Stage B)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Task:** Final Foundation Readiness — Stage B (Java 25 + Spring Boot `3.5.16` Alignment)  
**Date:** 2026-11-18  
**Migration Status:** **`COMPLETED & VERIFIED`**  

---

## 1. Compatibility Decision Summary

- **Previous Unendorsed Baseline:** Java `25.0.2` LTS + Spring Boot `3.4.12` (officially certified only for Java 17–23; shipped Byte Buddy `1.15.11` predating JDK 25 class-file v69 support).
- **New Officially Aligned Baseline:** Java `25.0.2` LTS + **Spring Boot `3.5.16`** (official Spring Boot 3.5.x documentation certifies Java 17 through Java 25, `17 <= Java <= 25`, upgrading Byte Buddy to `1.17.8`, Spring Framework to `6.2.19`, Spring Security to `6.5.11`, Hibernate ORM to `6.6.53.Final`, Flyway to `11.7.2`, and Tomcat to `10.1.55`).
- **Spring Boot 4.x Disposition:** Intentionally **not** migrated to Spring Boot 4.x in this foundation task (would require a separate breaking-change migration plan across Spring Framework 7 / Jakarta / Security 7 APIs).
- **Recommended P10 Container Base Image:** `eclipse-temurin:25-jre-alpine` (or `eclipse-temurin:25-jre` matching local `Temurin-25.0.2+10-LTS`).

---

## 2. Verified Component Compatibility Matrix (`springapp/pom.xml`)

| Component | Existing Version (`3.4.12`) | Migrated Version (`3.5.16`) | Compatibility Evidence | Action |
| :--- | :--- | :--- | :--- | :--- |
| **Java Runtime / Compiler** | OpenJDK `25.0.2` (`--release 25`) | OpenJDK `25.0.2` (`--release 25`) | Retains Java 25 LTS across compile, test, and runtime. | Retained |
| **Spring Boot Parent** | `3.4.12` | **`3.5.16`** | Official Spring Boot 3.5.x release line supports Java 17–25; zero manual BOM overrides needed. | Upgraded |
| **Spring Framework** | `6.2.14` | **`6.2.19`** | Spring Boot 3.5.16 managed; full Java 25 bytecode/ASM compatibility. | Aligned via BOM |
| **Spring Security** | `6.4.13` | **`6.5.11`** | Spring Boot 3.5.16 managed; stateless JWT, `SpaCsrfTokenRequestHandler`, `NullAuthenticatedSessionStrategy` & RBAC verified green. *(Minor deprecation notice in `SecurityConfig.java` noted for future cleanup.)* | Aligned via BOM |
| **Hibernate ORM** | `6.6.36.Final` | **`6.6.53.Final`** | Spring Boot 3.5.16 managed; `ddl-auto=validate`, JPQL projections, `@Version`, `FOR UPDATE` verified green. | Aligned via BOM |
| **Flyway** | `10.20.1` | **`11.7.2`** | Spring Boot 3.5.16 managed; historical `V1`–`V4` migrations preserved unmodified and validated green (`FlywayMigrationTests` 8/8). | Aligned via BOM |
| **Byte Buddy** | `1.15.11` | **`1.17.8`** | **Closes JDK 25 bytecode gap** (official JDK 25 class-file v69 support). | Aligned via BOM |
| **Apache Tomcat** | `10.1.49` | **`10.1.55`** | Live `RANDOM_PORT` HTTP E2E & startup health probe verified green. | Aligned via BOM |
| **JUnit Jupiter / Mockito** | `5.11.4` / `5.14.2` | **`5.12.2` / `5.17.0`** | Full 208-test suite green without attach warnings. | Aligned via BOM |
| **JJWT** | `0.11.5` | `0.11.5` | Pure Java HMAC-SHA256; all `JwtAuthenticationTests` (11/11) & live cookie flows pass. | Retained |
| **Jackson** | `2.18.x` | **`2.21.4`** | Managed by Spring Boot 3.5.16; RFC 7807 ProblemDetail & DTO serialization verified green. | Aligned via BOM |

---

## 3. Post-Migration Verification Evidence (`2026-11-18`)

| Verification Check | Exact Command | Exit Code | Result vs Baseline |
| :--- | :--- | :---: | :--- |
| **Maven Dependency Resolution** (`task-3887`) | `mvn dependency:resolve "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml` | `0` | All `3.5.16` managed artifacts resolved cleanly (`6.2 s`) |
| **Backend Clean Compile & Full Test Suite** (`task-3889`) | `mvn clean test -o "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml` | `0` | **208 / 208 passing** across **18 classes** (`0` fail, `0` err, `0` skip, `01:57 min`) — exact parity with 208-test baseline |
| **Frontend Jest Suite** (`task-3896`) | `Set-Location "springapp/springbackend"; npm test -- --watchAll=false` | `0` | **34 / 34 passing** across **3 suites** (`9.77 s`) — exact parity with 34-test baseline |
| **Frontend Production Build** (`task-3897`) | `Set-Location "springapp/springbackend"; npm run build` | `0` | **Compiled successfully** (`0` errors, `0` warnings; `93.97 kB` main + lazy chunks) |
| **Live JAR Startup & Health Check** (`task-3904`) | `java -jar springapp/target/springapp-0.0.1-SNAPSHOT.jar` + `GET /api/v1/health` | `0` | Booted cleanly on Spring Boot `3.5.16` + Java `25.0.2`; `/api/v1/health` returned HTTP `200` |

