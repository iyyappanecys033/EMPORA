package com.examly.springapp.repository;

import com.examly.springapp.model.InAppAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InAppAlertRepository extends JpaRepository<InAppAlert, Long> {

    List<InAppAlert> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    Page<InAppAlert> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    List<InAppAlert> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    List<InAppAlert> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    Optional<InAppAlert> findByIdAndRecipientId(Long id, Long recipientId);

    boolean existsByRecipientIdAndDedupKey(Long recipientId, String dedupKey);

    @Query(value = "SELECT a FROM InAppAlert a " +
           "LEFT JOIN FETCH a.ticket t " +
           "WHERE a.recipient.id = :recipientId " +
           "AND (:isRead IS NULL OR a.isRead = :isRead) " +
           "AND (:alertType IS NULL OR a.alertType = :alertType) " +
           "ORDER BY a.createdAt DESC, a.id DESC",
           countQuery = "SELECT COUNT(a) FROM InAppAlert a " +
           "WHERE a.recipient.id = :recipientId " +
           "AND (:isRead IS NULL OR a.isRead = :isRead) " +
           "AND (:alertType IS NULL OR a.alertType = :alertType)")
    Page<InAppAlert> findFilteredByRecipient(
            @Param("recipientId") Long recipientId,
            @Param("isRead") Boolean isRead,
            @Param("alertType") String alertType,
            Pageable pageable);

    @Modifying
    @Query("UPDATE InAppAlert a SET a.isRead = true, a.readAt = :readAt " +
           "WHERE a.recipient.id = :recipientId AND a.isRead = false")
    int markAllAsReadForRecipient(@Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);
}
