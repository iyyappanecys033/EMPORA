package com.examly.springapp.repository;

import com.examly.springapp.model.AuditChainHead;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditChainHeadRepository extends JpaRepository<AuditChainHead, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM AuditChainHead h WHERE h.id = :id")
    Optional<AuditChainHead> findByIdForUpdate(@Param("id") Long id);
}

