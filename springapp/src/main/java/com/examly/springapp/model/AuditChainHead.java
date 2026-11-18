package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * Audit Chain Head entity representing the singleton anchor row for SHA-256 hash chaining.
 * Maps to 'audit_chain_head' defined in Flyway V3.
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "audit_chain_head")
public class AuditChainHead {

    @Id
    private Long id;

    @Column(name = "current_hash", nullable = false, length = 64)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currentHash;

    @Column(name = "last_sequence", nullable = false)
    private Long lastSequence;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AuditChainHead() {
    }

    public AuditChainHead(Long id, String currentHash, Long lastSequence, LocalDateTime updatedAt) {
        this.id = id;
        this.currentHash = currentHash;
        this.lastSequence = lastSequence != null ? lastSequence : 0L;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public Long getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(Long lastSequence) {
        this.lastSequence = lastSequence;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

