# Deployment Architecture & Container Topology
## Employee Lifecycle and Service Management Platform (EPAMS)

**Document ID:** EPAMS-HLD-DEP-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved Architecture Baseline  
**Author:** Principal Software Architect & DevOps Lead  

---

## 1. Overview & Deployment Strategy

EPAMS follows an **immutable container deployment pattern** designed for simplicity, reproducibility, and high security in mid-market environments (100–1,000 users). The entire platform—comprising the Spring Boot backend and the production-optimized React Single Page Application (SPA)—is compiled and assembled via a **multi-stage Docker build** into a minimal, self-contained runtime image.

### Technology Runtime Policy
* **Preferred Java Baseline:** Java 25 LTS (OpenJDK 64-Bit Server VM Temurin-25.0.2+10).
* **Container Base Image:** `eclipse-temurin:25-jre-alpine` (or `eclipse-temurin:25-jre-noble` for glibc dependencies).
* **Process Execution:** Unprivileged non-root service account (`spring:spring`, UID/GID 1001:1001).
* **Persistence Tier:** MySQL 8.0+ InnoDB container with persistent Docker named volumes and automated health checking.
* **Testing / Development Mail Relay:** Mailpit container (SMTP port 1025, Web UI port 8025) for safe local notification verification (`ADR-009`, `NFR-013`).

---

## 2. Container Topology Diagram

```mermaid
graph TD
    subgraph Client Tier
        UserBrowser["User Web Browser (Chrome / Edge / Firefox)"]
    end

    subgraph Host Network Boundary [Host Ingress - Port 80 / 443 / 8080]
        Proxy["Reverse Proxy / Nginx / ALB (Optional TLS Termination)"]
    end

    subgraph Docker Internal Bridge Network [epams-net: 172.28.0.0/16]
        subgraph App Container [epams-app:8080]
            direction TB
            TemurinJVM["Eclipse Adoptium Temurin 25 JRE"]
            SpringBootApp["EPAMS Spring Boot Executable JAR"]
            StaticAssets["Bundled React SPA (HTML/JS/CSS)"]
            TemurinJVM --- SpringBootApp
            SpringBootApp --- StaticAssets
        end

        subgraph Database Container [epams-mysql:3306]
            direction TB
            MySQLDaemon["MySQL 8.0+ Server (InnoDB)"]
            AppDB[("Database: epams_db")]
            MySQLDaemon --- AppDB
        end

        subgraph Mail Testing Container [epams-mailpit:1025 / 8025]
            direction TB
            MailpitServer["Mailpit SMTP Server (:1025)"]
            MailpitWeb["Mailpit Web UI (:8025)"]
            MailpitServer --- MailpitWeb
        end
    end

    subgraph Host Persistent Storage
        MySQLDataVolume[("Named Volume: mysql_data (/var/lib/mysql)")]
        AppLogsVolume[("Named Volume: app_logs (/var/log/epams)")]
    end

    UserBrowser -->|HTTPS :443 / HTTP :8080| Proxy
    Proxy -->|HTTP :8080| AppContainer
    AppContainer -->|JDBC :3306 (epams_app user)| DatabaseContainer
    AppContainer -->|SMTP :1025 (Transactional Alerts)| MailTestingContainer
    UserBrowser -.->|Debug Web UI :8025 (Dev Only)| MailTestingContainer

    DatabaseContainer --- MySQLDataVolume
    AppContainer --- AppLogsVolume
```

---

## 3. Multi-Stage Dockerfile Architecture

To maintain zero compiler tools, zero package managers, and minimal attack surface in production, EPAMS utilizes a **3-stage Docker build pipeline**:

```dockerfile
# ==============================================================================
# STAGE 1: Frontend Asset Compilation (Node.js 20 LTS)
# ==============================================================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

# Leverage layer caching for dependencies
COPY springbackend/package.json springbackend/package-lock.json ./
RUN npm ci --prefer-offline --no-audit

# Copy React source code and build production bundle
COPY springbackend/ ./
ENV NODE_ENV=production
RUN npm run build

# ==============================================================================
# STAGE 2: Java Backend Compilation & Packaging (Maven 3.9 + Java 25 LTS)
# ==============================================================================
FROM maven:3.9-eclipse-temurin-25-noble AS backend-builder
WORKDIR /app/backend

# Leverage layer caching for Maven dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy Spring Boot backend source code
COPY src ./src

# Inject compiled React assets from Stage 1 into Spring Boot static resources
COPY --from=frontend-builder /app/frontend/build ./src/main/resources/static/

# Package executable Fat JAR (skipping unit tests during image build)
RUN mvn clean package -DskipTests -B

# ==============================================================================
# STAGE 3: Final Production Runtime Image (Eclipse Temurin 25 JRE Minimal)
# ==============================================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

# Install curl for container health check
RUN apk add --no-cache curl tzdata && rm -rf /var/cache/apk/*

# Set container timezone to UTC
ENV TZ=UTC

# Create unprivileged service user and group
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring -h /home/spring

# Create log directory with proper permissions
RUN mkdir -p /var/log/epams && chown -R spring:spring /var/log/epams

WORKDIR /app

# Copy executable JAR from backend-builder stage
COPY --from=backend-builder --chown=spring:spring /app/backend/target/springapp-0.0.1-SNAPSHOT.jar app.jar

# Switch to unprivileged execution user
USER spring:spring

# Expose Spring Boot HTTP port
EXPOSE 8080

# Configure JVM memory limits for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=mysql"

# Healthcheck probing the application's database-aware health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 4. Docker Compose Multi-Environment Configuration

### 4.1 Production-Ready Compose Topology (`docker-compose.yml`)

```yaml
version: '3.8'

networks:
  epams-network:
    driver: bridge

volumes:
  mysql_data:
    driver: local
  app_logs:
    driver: local

services:
  epams-mysql:
    image: mysql:8.0.36
    container_name: epams-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:?DB_ROOT_PASSWORD is required}
      MYSQL_DATABASE: ${DB_NAME:-epams_db}
      MYSQL_USER: ${DB_USERNAME:-epams_app}
      MYSQL_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}
      TZ: UTC
    ports:
      - "127.0.0.1:3306:3306" # Bound to localhost to prevent direct public WAN exposure
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql/init-privileges.sql:/docker-entrypoint-initdb.d/init-privileges.sql:ro
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+00:00
      - --innodb-buffer-pool-size=256M
      - --max-connections=150
    networks:
      - epams-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  epams-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: epams-app
    restart: unless-stopped
    depends_on:
      epams-mysql:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: mysql
      DB_HOST: epams-mysql
      DB_PORT: 3306
      DB_NAME: ${DB_NAME:-epams_db}
      DB_USERNAME: ${DB_USERNAME:-epams_app}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET:?JWT_SECRET must be configured via environment}
      JWT_EXPIRATION: ${JWT_EXPIRATION:-900000}
      JWT_REFRESH_EXPIRATION: ${JWT_REFRESH_EXPIRATION:-604800000}
      COOKIE_SECURE: ${COOKIE_SECURE:-true}
      COOKIE_SAME_SITE: ${COOKIE_SAME_SITE:-Lax}
      SPRING_MAIL_HOST: ${SMTP_HOST:-epams-mailpit}
      SPRING_MAIL_PORT: ${SMTP_PORT:-1025}
      APP_WORKFLOW_APPROVAL_FINANCIAL_THRESHOLD: ${FINANCIAL_THRESHOLD:-10000}
    ports:
      - "8080:8080"
    volumes:
      - app_logs:/var/log/epams
    networks:
      - epams-network
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 1024M
        reservations:
          cpus: '0.5'
          memory: 512M

  epams-mailpit:
    image: axllent/mailpit:v1.18
    container_name: epams-mailpit
    restart: unless-stopped
    ports:
      - "127.0.0.1:8025:8025" # Web UI accessible only on host loopback
    networks:
      - epams-network
    profiles:
      - dev
      - staging
```

---

## 5. Environment Profiles & Configuration Matrix

| Configuration Variable | Dev Profile (H2 / Local) | Staging Profile (MySQL / Mailpit) | Production Profile (MySQL / SMTP) | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `dev` | `staging,mysql` | `prod,mysql` | Selects active Spring configuration. |
| `spring.datasource.url` | `jdbc:h2:mem:springboot_db` | `jdbc:mysql://epams-mysql:3306/epams_db` | `jdbc:mysql://db-cluster:3306/epams_db` | JDBC connection string with UTF-8 & UTC. |
| `spring.flyway.enabled` | `false` (or test migrations) | `true` | `true` | Automated migration execution on startup. |
| `spring.jpa.hibernate.ddl-auto` | `update` (local testing) | `validate` | `validate` | Prohibits Hibernate from altering tables. |
| `jwt.secret` | Ephemeral Base64 secret | Injected via `.env` | Managed via Vault / Cloud KMS | HMAC-SHA256 signing secret key. |
| `app.security.cookie.secure` | `false` (plain HTTP) | `true` (HTTPS) | `true` (HTTPS mandatory) | Enforces Secure flag on Refresh Token cookie. |
| `app.security.cookie.same-site` | `Lax` | `Lax` | `Strict` / `Lax` | CSRF cookie defense policy. |
| `spring.mail.host` | `localhost` / Mock | `epams-mailpit` | Corporate Relay (`smtp.office365.com`) | Target SMTP relay hostname. |

---

## 6. JVM Performance & Resource Sizing

For the targeted 100–1,000 employee workload (~200–500 service tickets/day, peak concurrency of 30–50 concurrent requests):

1. **JVM Garbage Collector:** Java 25 Default G1GC (`-XX:+UseG1GC`) with low pause time target (`-XX:MaxGCPauseMillis=200`).
2. **Container Sizing:**
   * Minimum RAM Reservation: `512 MB`.
   * Container Maximum RAM Limit: `1024 MB`.
   * Max JVM Heap Allocation: `75% of 1024 MB = 768 MB` (configured via `-XX:MaxRAMPercentage=75.0`).
   * Non-Heap / Metaspace: `~160 MB` for classes and Spring reflection metadata.
   * Native Stack & Buffer Pool: `~96 MB`.
3. **Database Connection Pool (HikariCP):**
   * Pool Size: `maximum-pool-size: 10`, `minimum-idle: 5`.
   * Connection Timeout: `20,000 ms`.
   * Idle Timeout: `300,000 ms`.
   * Max Lifetime: `1,200,000 ms`.

---

## 7. Graceful Shutdown & Health Probes

To prevent in-flight ticket mutations and cryptographic hash updates from corrupting during rolling container deployments:
* **Graceful Shutdown Window:** Configured via `server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase=30s`.
* **Shutdown Sequence:**
  1. Container orchestrator (Docker Compose / Kubernetes) signals `SIGTERM`.
  2. Spring Boot stops accepting new incoming HTTP connections on port 8080.
  3. Liveness / Readiness probe transitions to `OUT_OF_SERVICE`.
  4. Active HTTP request threads and local ACID database transactions are allowed up to 30 seconds to commit cleanly.
  5. HikariCP closes database connections cleanly.
  6. Process exits with code 0.

