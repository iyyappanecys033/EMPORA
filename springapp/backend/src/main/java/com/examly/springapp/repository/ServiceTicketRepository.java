package com.examly.springapp.repository;

import com.examly.springapp.model.ServiceTicket;
import com.examly.springapp.model.TicketPriority;
import com.examly.springapp.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ServiceTicket entity.
 * Includes database-side aggregation queries for P8 Operational Analytics.
 */
@Repository
public interface ServiceTicketRepository extends JpaRepository<ServiceTicket, Long> {

    Optional<ServiceTicket> findByTicketNumber(String ticketNumber);

    boolean existsByTicketNumber(String ticketNumber);

    Page<ServiceTicket> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

    Page<ServiceTicket> findByAssigneeIdOrderByCreatedAtDesc(Long assigneeId, Pageable pageable);

    Page<ServiceTicket> findByDepartmentOrderByCreatedAtDesc(String department, Pageable pageable);

    Page<ServiceTicket> findByDepartmentAndStatusOrderByCreatedAtDesc(String department, TicketStatus status, Pageable pageable);

    Page<ServiceTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    List<ServiceTicket> findByStatus(TicketStatus status);

    @Query("SELECT t FROM ServiceTicket t LEFT JOIN FETCH t.assignee WHERE t.status IN :statuses")
    List<ServiceTicket> findByStatusIn(@Param("statuses") Collection<TicketStatus> statuses);

    long countByStatus(TicketStatus status);

    long countByDepartmentAndStatus(String department, TicketStatus status);

    long countByRequesterIdAndStatus(Long requesterId, TicketStatus status);

    // =========================================================================
    // P8 Operational Analytics — Database-Side Aggregation Queries
    // =========================================================================

    @Query("SELECT t.status, COUNT(t) FROM ServiceTicket t " +
           "WHERE t.createdAt >= :start AND t.createdAt <= :end " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "GROUP BY t.status")
    List<Object[]> countGroupedByStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("dept") String dept,
            @Param("priority") TicketPriority priority);

    @Query("SELECT t.priority, COUNT(t) FROM ServiceTicket t " +
           "WHERE t.createdAt >= :start AND t.createdAt <= :end " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "GROUP BY t.priority")
    List<Object[]> countGroupedByPriority(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("dept") String dept,
            @Param("status") TicketStatus status);

    @Query("SELECT COUNT(t) FROM ServiceTicket t " +
           "WHERE t.status IN :openStatuses " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept))")
    long countCurrentOpenSnapshot(
            @Param("openStatuses") Collection<TicketStatus> openStatuses,
            @Param("dept") String dept);

    @Query("SELECT COUNT(t) FROM ServiceTicket t " +
           "WHERE t.status IN :activeClockStatuses " +
           "AND t.slaTargetDate < :now " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept))")
    long countCurrentOverdueOpenSnapshot(
            @Param("activeClockStatuses") Collection<TicketStatus> activeClockStatuses,
            @Param("now") LocalDateTime now,
            @Param("dept") String dept);

    @Query("SELECT t.department, COUNT(t) FROM ServiceTicket t " +
           "WHERE t.status IN :openStatuses " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "GROUP BY t.department")
    List<Object[]> countOpenBacklogByDepartment(
            @Param("openStatuses") Collection<TicketStatus> openStatuses,
            @Param("dept") String dept);

    @Query("SELECT t.department, COUNT(t) FROM ServiceTicket t " +
           "WHERE t.status IN :activeClockStatuses " +
           "AND t.slaTargetDate < :now " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "GROUP BY t.department")
    List<Object[]> countOverdueOpenByDepartment(
            @Param("activeClockStatuses") Collection<TicketStatus> activeClockStatuses,
            @Param("now") LocalDateTime now,
            @Param("dept") String dept);

    @Query("SELECT c.id, c.itemCode, c.title, c.department, c.category, COUNT(t) " +
           "FROM ServiceTicket t JOIN t.catalogItem c " +
           "WHERE t.createdAt >= :start AND t.createdAt <= :end " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "AND (:category IS NULL OR LOWER(c.category) = LOWER(:category)) " +
           "GROUP BY c.id, c.itemCode, c.title, c.department, c.category " +
           "ORDER BY COUNT(t) DESC, c.title ASC")
    List<Object[]> findServicePopularity(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("dept") String dept,
            @Param("category") String category,
            Pageable pageable);

    /**
     * Lightweight scalar projection for SLA compliance, resolution time, department metrics,
     * and daily volume trends—avoids hydrating full entity graphs or causing N+1 queries.
     */
    @Query("SELECT t.department, t.status, t.createdAt, t.resolvedAt, t.slaTargetDate, t.totalPausedMinutes " +
           "FROM ServiceTicket t " +
           "WHERE t.createdAt >= :start AND t.createdAt <= :end " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "AND (:priority IS NULL OR t.priority = :priority)")
    List<Object[]> findAnalyticsProjectionInPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("dept") String dept,
            @Param("priority") TicketPriority priority);

    @Query("SELECT t.resolvedAt FROM ServiceTicket t " +
           "WHERE t.resolvedAt IS NOT NULL " +
           "AND t.resolvedAt >= :start AND t.resolvedAt <= :end " +
           "AND (:dept IS NULL OR LOWER(t.department) = LOWER(:dept)) " +
           "AND (:priority IS NULL OR t.priority = :priority)")
    List<LocalDateTime> findResolvedTimestampsInPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("dept") String dept,
            @Param("priority") TicketPriority priority);
}
