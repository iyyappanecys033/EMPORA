package com.examly.springapp.repository;

import com.examly.springapp.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc();
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    List<AuditLog> findByActorUsernameOrderByTimestampDesc(String actorUsername);
    List<AuditLog> findByTargetUsernameOrderByTimestampDesc(String targetUsername);
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    Optional<AuditLog> findBySequenceNumber(Long sequenceNumber);
    List<AuditLog> findBySequenceNumberIsNotNullOrderBySequenceNumberAsc();
    List<AuditLog> findBySequenceNumberIsNullOrderByTimestampAsc();
    Optional<AuditLog> findTopBySequenceNumberIsNotNullOrderBySequenceNumberDesc();

    @org.springframework.transaction.annotation.Transactional
    void deleteBySequenceNumberIsNull();

    @org.springframework.transaction.annotation.Transactional
    void deleteBySequenceNumberIsNotNull();
}
