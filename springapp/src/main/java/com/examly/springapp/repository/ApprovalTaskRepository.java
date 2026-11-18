package com.examly.springapp.repository;

import com.examly.springapp.model.ApprovalStatus;
import com.examly.springapp.model.ApprovalTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, Long> {

    List<ApprovalTask> findByTicketIdOrderByApprovalStageAsc(Long ticketId);

    Optional<ApprovalTask> findByTicketIdAndApprovalStage(Long ticketId, Integer approvalStage);

    Optional<ApprovalTask> findByTicketIdAndStatus(Long ticketId, ApprovalStatus status);

    List<ApprovalTask> findByApproverIdAndStatusOrderByCreatedAtDesc(Long approverId, ApprovalStatus status);

    @Query("SELECT a FROM ApprovalTask a WHERE a.approver.username = :username AND a.status = :status ORDER BY a.createdAt DESC")
    List<ApprovalTask> findPendingByApproverUsername(@Param("username") String username, @Param("status") ApprovalStatus status);

    boolean existsByTicketIdAndApprovalStage(Long ticketId, Integer approvalStage);

    // =========================================================================
    // P8 Operational Analytics — Approval Aggregation Queries
    // =========================================================================

    @Query("SELECT a.status, COUNT(a) FROM ApprovalTask a JOIN a.ticket t " +
           "WHERE (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "AND a.createdAt >= :start AND a.createdAt <= :end " +
           "GROUP BY a.status")
    List<Object[]> countGroupedByStatusInPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("dept") String dept);

    @Query("SELECT a.approvalStage, COUNT(a) FROM ApprovalTask a JOIN a.ticket t " +
           "WHERE a.status = :status " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "GROUP BY a.approvalStage")
    List<Object[]> countCurrentByStatusGroupedByStage(
            @Param("status") ApprovalStatus status,
            @Param("dept") String dept);

    @Query("SELECT COUNT(a) FROM ApprovalTask a JOIN a.ticket t " +
           "WHERE a.status = :status " +
           "AND a.createdAt < :overdueThreshold " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept))")
    long countOverduePendingApprovals(
            @Param("status") ApprovalStatus status,
            @Param("overdueThreshold") LocalDateTime overdueThreshold,
            @Param("dept") String dept);
}
