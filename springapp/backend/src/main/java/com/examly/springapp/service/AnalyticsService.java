package com.examly.springapp.service;

import com.examly.springapp.dto.analytics.*;
import com.examly.springapp.exception.UnauthorizedScopeException;
import com.examly.springapp.model.ApprovalStatus;
import com.examly.springapp.model.Profile;
import com.examly.springapp.model.TicketPriority;
import com.examly.springapp.model.TicketStatus;
import com.examly.springapp.repository.ApprovalTaskRepository;
import com.examly.springapp.repository.ProfileRepo;
import com.examly.springapp.repository.ServiceTicketRepository;
import com.examly.springapp.security.TicketSecurityEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Operational Analytics Engine for EPAMS.
 * All aggregations use database-side GROUP BY or lightweight scalar projections
 * and enforce strict organizational role & department boundaries.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int MAX_DATE_RANGE_DAYS = 366;
    private static final int APPROVAL_OVERDUE_HOURS = 24;

    private static final EnumSet<TicketStatus> OPEN_STATUSES = EnumSet.of(
            TicketStatus.DRAFT,
            TicketStatus.SUBMITTED,
            TicketStatus.PENDING_APPROVAL,
            TicketStatus.PENDING_SECONDARY_APPROVAL,
            TicketStatus.ASSIGNED,
            TicketStatus.IN_PROGRESS,
            TicketStatus.ON_HOLD
    );

    /**
     * Active SLA clock statuses: excludes ON_HOLD (paused clock), DRAFT, and terminal states.
     */
    private static final EnumSet<TicketStatus> ACTIVE_CLOCK_STATUSES = EnumSet.of(
            TicketStatus.SUBMITTED,
            TicketStatus.PENDING_APPROVAL,
            TicketStatus.PENDING_SECONDARY_APPROVAL,
            TicketStatus.ASSIGNED,
            TicketStatus.IN_PROGRESS
    );

    private final ServiceTicketRepository ticketRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final TicketSecurityEvaluator securityEvaluator;
    private final ProfileRepo profileRepo;
    private final Clock clock;

    @Autowired
    public AnalyticsService(ServiceTicketRepository ticketRepository,
                            ApprovalTaskRepository approvalTaskRepository,
                            TicketSecurityEvaluator securityEvaluator,
                            ProfileRepo profileRepo,
                            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.securityEvaluator = securityEvaluator;
        this.profileRepo = profileRepo;
        this.clock = clock;
    }

    /**
     * Optimized single-pass dashboard aggregation:
     * Resolves department authorization scope once and shares the scalar period projection
     * across overview, trends, SLA compliance, and department metrics—eliminating
     * redundant role lookups and repeated ticket scans.
     */
    public AnalyticsDashboardBundleDTO getDashboardBundle(LocalDate startDate, LocalDate endDate,
                                                          String department, TicketPriority priority, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);
        LocalDateTime now = LocalDateTime.now(clock);

        // Single projection fetch shared across overview, SLA, trends, and department metrics
        List<Object[]> periodRows = ticketRepository.findAnalyticsProjectionInPeriod(
                window.startDateTime(), window.endDateTime(), scopedDept, priority);
        List<LocalDateTime> resolvedTimestamps = ticketRepository.findResolvedTimestampsInPeriod(
                window.startDateTime(), window.endDateTime(), scopedDept, priority);
        List<Object[]> openBacklogRows = ticketRepository.countOpenBacklogByDepartment(OPEN_STATUSES, scopedDept);
        List<Object[]> overdueOpenRows = ticketRepository.countOverdueOpenByDepartment(ACTIVE_CLOCK_STATUSES, now, scopedDept);

        // Single pass over periodRows
        long periodTotal = periodRows.size();
        long resolvedOnlyCount = 0;
        long closedCount = 0;
        long cancelledOrRejected = 0;
        long evaluatedSla = 0;
        long metSla = 0;
        long breachedSla = 0;
        long activeOverdue = 0;
        long resMinsSum = 0;
        long resMinsCount = 0;

        Map<LocalDate, Long> submittedByDay = new HashMap<>();
        Map<TicketStatus, Long> statusCounts = new EnumMap<>(TicketStatus.class);
        Map<String, long[]> deptStats = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Object[] row : periodRows) {
            String dept = (String) row[0];
            TicketStatus status = (TicketStatus) row[1];
            LocalDateTime createdAt = (LocalDateTime) row[2];
            LocalDateTime resolvedAt = (LocalDateTime) row[3];
            LocalDateTime slaTarget = (LocalDateTime) row[4];
            Integer pausedMins = row[5] != null ? ((Number) row[5]).intValue() : 0;

            statusCounts.merge(status, 1L, Long::sum);
            if (createdAt != null) {
                submittedByDay.merge(createdAt.toLocalDate(), 1L, Long::sum);
            }

            long[] dBucket = deptStats.computeIfAbsent(dept, k -> new long[6]);
            dBucket[0]++;

            if (status == TicketStatus.RESOLVED) {
                resolvedOnlyCount++;
                dBucket[1]++;
            } else if (status == TicketStatus.CLOSED) {
                closedCount++;
                dBucket[1]++;
            } else if (status == TicketStatus.CANCELLED || status == TicketStatus.REJECTED) {
                cancelledOrRejected++;
                continue;
            }

            if ((status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) && resolvedAt != null) {
                if (slaTarget != null) {
                    evaluatedSla++;
                    dBucket[2]++;
                    if (!resolvedAt.isAfter(slaTarget)) {
                        metSla++;
                        dBucket[3]++;
                    } else {
                        breachedSla++;
                    }
                }
                if (createdAt != null) {
                    long elapsed = Math.max(0L, Duration.between(createdAt, resolvedAt).toMinutes() - pausedMins);
                    resMinsSum += elapsed;
                    resMinsCount++;
                }
            } else if (ACTIVE_CLOCK_STATUSES.contains(status) && slaTarget != null && now.isAfter(slaTarget)) {
                evaluatedSla++;
                breachedSla++;
                activeOverdue++;
                dBucket[2]++;
            }
        }

        long openSnapshotTotal = 0;
        for (Object[] row : openBacklogRows) {
            String dept = (String) row[0];
            long count = ((Number) row[1]).longValue();
            openSnapshotTotal += count;
            deptStats.computeIfAbsent(dept, k -> new long[6])[4] = count;
        }

        long overdueSnapshotTotal = 0;
        for (Object[] row : overdueOpenRows) {
            String dept = (String) row[0];
            long count = ((Number) row[1]).longValue();
            overdueSnapshotTotal += count;
            deptStats.computeIfAbsent(dept, k -> new long[6])[5] = count;
        }

        // Approvals aggregation (reused for overview & approvals panel)
        ApprovalOverviewDTO approvalsOverview = computeApprovalsOverview(window, scopedDept, now);

        Double slaRate = evaluatedSla > 0 ? round2((metSla * 100.0) / evaluatedSla) : null;
        Double avgResHours = resMinsCount > 0 ? round2((resMinsSum / (double) resMinsCount) / 60.0) : null;

        AnalyticsOverviewDTO overview = new AnalyticsOverviewDTO(
                periodTotal,
                openSnapshotTotal,
                resolvedOnlyCount + closedCount,
                closedCount,
                cancelledOrRejected,
                overdueSnapshotTotal,
                slaRate,
                avgResHours,
                approvalsOverview.pendingSnapshot(),
                scopedDept != null ? scopedDept : "ALL",
                window.startDate(),
                window.endDate(),
                clock.getZone().getId()
        );

        SlaComplianceDTO slaCompliance = new SlaComplianceDTO(
                evaluatedSla, metSla, breachedSla, activeOverdue, slaRate, avgResHours);

        // Status distribution computed directly from single pass
        List<StatusDistributionDTO> statusDistribution = new ArrayList<>();
        for (Map.Entry<TicketStatus, Long> e : statusCounts.entrySet()) {
            double pct = periodTotal > 0 ? round2((e.getValue() * 100.0) / periodTotal) : 0.0;
            statusDistribution.add(new StatusDistributionDTO(e.getKey().name(), e.getValue(), pct));
        }
        statusDistribution.sort(Comparator.comparingLong(StatusDistributionDTO::count).reversed());

        // Priority distribution via DB GROUP BY
        List<PriorityDistributionDTO> priorityDistribution = computePriorityDistribution(window, scopedDept, null);

        // Daily trends
        Map<LocalDate, Long> resolvedByDay = new HashMap<>();
        for (LocalDateTime r : resolvedTimestamps) {
            if (r != null) resolvedByDay.merge(r.toLocalDate(), 1L, Long::sum);
        }
        List<TicketTrendPointDTO> trends = new ArrayList<>();
        LocalDate cursor = window.startDate();
        while (!cursor.isAfter(window.endDate())) {
            trends.add(new TicketTrendPointDTO(cursor,
                    submittedByDay.getOrDefault(cursor, 0L),
                    resolvedByDay.getOrDefault(cursor, 0L)));
            cursor = cursor.plusDays(1);
        }

        // Department performance
        List<DepartmentPerformanceDTO> departmentPerformance = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : deptStats.entrySet()) {
            long[] b = entry.getValue();
            Double dRate = b[2] > 0 ? round2((b[3] * 100.0) / b[2]) : null;
            departmentPerformance.add(new DepartmentPerformanceDTO(entry.getKey(), b[0], b[4], b[1], b[5], dRate));
        }
        departmentPerformance.sort(Comparator.comparingLong(DepartmentPerformanceDTO::openBacklogSnapshot).reversed()
                .thenComparing(Comparator.comparingLong(DepartmentPerformanceDTO::totalSubmittedInPeriod).reversed()));

        // Service popularity via DB GROUP BY
        List<ServicePopularityDTO> servicePopularity = computeServicePopularity(window, scopedDept, null, 8);

        return new AnalyticsDashboardBundleDTO(
                overview, statusDistribution, priorityDistribution, trends,
                departmentPerformance, servicePopularity, slaCompliance, approvalsOverview);
    }

    public AnalyticsOverviewDTO getOverview(LocalDate startDate, LocalDate endDate,
                                            String department, TicketPriority priority, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);
        LocalDateTime now = LocalDateTime.now(clock);

        List<Object[]> rows = ticketRepository.findAnalyticsProjectionInPeriod(
                window.startDateTime(), window.endDateTime(), scopedDept, priority);

        long periodTotal = rows.size();
        long resolvedInPeriod = 0;
        long closedInPeriod = 0;
        long cancelledOrRejected = 0;

        long evaluatedSla = 0;
        long metSla = 0;
        long resolutionMinutesSum = 0;
        long resolutionMinutesCount = 0;

        for (Object[] row : rows) {
            TicketStatus status = (TicketStatus) row[1];
            LocalDateTime createdAt = (LocalDateTime) row[2];
            LocalDateTime resolvedAt = (LocalDateTime) row[3];
            LocalDateTime slaTarget = (LocalDateTime) row[4];
            Integer pausedMinutes = row[5] != null ? ((Number) row[5]).intValue() : 0;

            if (status == TicketStatus.RESOLVED) {
                resolvedInPeriod++;
            } else if (status == TicketStatus.CLOSED) {
                closedInPeriod++;
            } else if (status == TicketStatus.CANCELLED || status == TicketStatus.REJECTED) {
                cancelledOrRejected++;
                continue;
            }

            if ((status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) && resolvedAt != null) {
                if (slaTarget != null) {
                    evaluatedSla++;
                    if (!resolvedAt.isAfter(slaTarget)) {
                        metSla++;
                    }
                }
                if (createdAt != null) {
                    long elapsedMins = Math.max(0L, Duration.between(createdAt, resolvedAt).toMinutes() - pausedMinutes);
                    resolutionMinutesSum += elapsedMins;
                    resolutionMinutesCount++;
                }
            } else if (ACTIVE_CLOCK_STATUSES.contains(status) && slaTarget != null && now.isAfter(slaTarget)) {
                evaluatedSla++;
            }
        }

        long openSnapshot = ticketRepository.countCurrentOpenSnapshot(OPEN_STATUSES, scopedDept);
        long overdueSnapshot = ticketRepository.countCurrentOverdueOpenSnapshot(ACTIVE_CLOCK_STATUSES, now, scopedDept);

        List<Object[]> pendingByStage = approvalTaskRepository.countCurrentByStatusGroupedByStage(
                ApprovalStatus.PENDING, scopedDept);
        long pendingApprovalsSnapshot = pendingByStage.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        Double slaComplianceRate = evaluatedSla > 0 ? round2((metSla * 100.0) / evaluatedSla) : null;
        Double avgResHours = resolutionMinutesCount > 0
                ? round2((resolutionMinutesSum / (double) resolutionMinutesCount) / 60.0)
                : null;

        return new AnalyticsOverviewDTO(
                periodTotal,
                openSnapshot,
                resolvedInPeriod + closedInPeriod,
                closedInPeriod,
                cancelledOrRejected,
                overdueSnapshot,
                slaComplianceRate,
                avgResHours,
                pendingApprovalsSnapshot,
                scopedDept != null ? scopedDept : "ALL",
                window.startDate(),
                window.endDate(),
                clock.getZone().getId()
        );
    }

    public List<StatusDistributionDTO> getStatusDistribution(LocalDate startDate, LocalDate endDate,
                                                             String department, TicketPriority priority, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);

        List<Object[]> grouped = ticketRepository.countGroupedByStatus(
                window.startDateTime(), window.endDateTime(), scopedDept, priority);

        long total = grouped.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<StatusDistributionDTO> result = new ArrayList<>();
        for (Object[] row : grouped) {
            TicketStatus status = (TicketStatus) row[0];
            long count = ((Number) row[1]).longValue();
            double pct = total > 0 ? round2((count * 100.0) / total) : 0.0;
            result.add(new StatusDistributionDTO(status.name(), count, pct));
        }
        result.sort(Comparator.comparingLong(StatusDistributionDTO::count).reversed());
        return result;
    }

    public List<PriorityDistributionDTO> getPriorityDistribution(LocalDate startDate, LocalDate endDate,
                                                                 String department, TicketStatus status, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);
        return computePriorityDistribution(window, scopedDept, status);
    }

    private List<PriorityDistributionDTO> computePriorityDistribution(DateWindow window, String scopedDept, TicketStatus status) {
        List<Object[]> grouped = ticketRepository.countGroupedByPriority(
                window.startDateTime(), window.endDateTime(), scopedDept, status);

        Map<TicketPriority, Long> countMap = new EnumMap<>(TicketPriority.class);
        for (TicketPriority p : TicketPriority.values()) {
            countMap.put(p, 0L);
        }
        long total = 0;
        for (Object[] row : grouped) {
            TicketPriority p = (TicketPriority) row[0];
            long c = ((Number) row[1]).longValue();
            countMap.put(p, c);
            total += c;
        }

        List<PriorityDistributionDTO> result = new ArrayList<>();
        for (TicketPriority p : TicketPriority.values()) {
            long count = countMap.get(p);
            double pct = total > 0 ? round2((count * 100.0) / total) : 0.0;
            result.add(new PriorityDistributionDTO(p.name(), count, pct));
        }
        return result;
    }

    public List<TicketTrendPointDTO> getTicketTrends(LocalDate startDate, LocalDate endDate,
                                                     String department, TicketPriority priority, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);

        List<Object[]> createdRows = ticketRepository.findAnalyticsProjectionInPeriod(
                window.startDateTime(), window.endDateTime(), scopedDept, priority);
        List<LocalDateTime> resolvedTimestamps = ticketRepository.findResolvedTimestampsInPeriod(
                window.startDateTime(), window.endDateTime(), scopedDept, priority);

        Map<LocalDate, Long> submittedByDay = new HashMap<>();
        for (Object[] row : createdRows) {
            LocalDateTime createdAt = (LocalDateTime) row[2];
            if (createdAt != null) {
                LocalDate day = createdAt.toLocalDate();
                submittedByDay.merge(day, 1L, Long::sum);
            }
        }

        Map<LocalDate, Long> resolvedByDay = new HashMap<>();
        for (LocalDateTime resolvedAt : resolvedTimestamps) {
            if (resolvedAt != null) {
                LocalDate day = resolvedAt.toLocalDate();
                resolvedByDay.merge(day, 1L, Long::sum);
            }
        }

        List<TicketTrendPointDTO> points = new ArrayList<>();
        LocalDate cursor = window.startDate();
        while (!cursor.isAfter(window.endDate())) {
            points.add(new TicketTrendPointDTO(
                    cursor,
                    submittedByDay.getOrDefault(cursor, 0L),
                    resolvedByDay.getOrDefault(cursor, 0L)
            ));
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    public List<DepartmentPerformanceDTO> getDepartmentPerformance(LocalDate startDate, LocalDate endDate,
                                                                   String department, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);
        LocalDateTime now = LocalDateTime.now(clock);

        List<Object[]> periodRows = ticketRepository.findAnalyticsProjectionInPeriod(
                window.startDateTime(), window.endDateTime(), scopedDept, null);
        List<Object[]> openBacklogRows = ticketRepository.countOpenBacklogByDepartment(OPEN_STATUSES, scopedDept);
        List<Object[]> overdueRows = ticketRepository.countOverdueOpenByDepartment(ACTIVE_CLOCK_STATUSES, now, scopedDept);

        Map<String, long[]> stats = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // [0]=totalSubmitted, [1]=resolved, [2]=evaluatedSla, [3]=metSla, [4]=openBacklog, [5]=overdueOpen

        for (Object[] row : periodRows) {
            String dept = (String) row[0];
            TicketStatus status = (TicketStatus) row[1];
            LocalDateTime resolvedAt = (LocalDateTime) row[3];
            LocalDateTime slaTarget = (LocalDateTime) row[4];

            long[] bucket = stats.computeIfAbsent(dept, k -> new long[6]);
            bucket[0]++;
            if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
                bucket[1]++;
                if (resolvedAt != null && slaTarget != null) {
                    bucket[2]++;
                    if (!resolvedAt.isAfter(slaTarget)) {
                        bucket[3]++;
                    }
                }
            } else if (ACTIVE_CLOCK_STATUSES.contains(status) && slaTarget != null && now.isAfter(slaTarget)) {
                bucket[2]++;
            }
        }

        for (Object[] row : openBacklogRows) {
            String dept = (String) row[0];
            long count = ((Number) row[1]).longValue();
            stats.computeIfAbsent(dept, k -> new long[6])[4] = count;
        }

        for (Object[] row : overdueRows) {
            String dept = (String) row[0];
            long count = ((Number) row[1]).longValue();
            stats.computeIfAbsent(dept, k -> new long[6])[5] = count;
        }

        List<DepartmentPerformanceDTO> list = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : stats.entrySet()) {
            long[] b = entry.getValue();
            Double slaRate = b[2] > 0 ? round2((b[3] * 100.0) / b[2]) : null;
            list.add(new DepartmentPerformanceDTO(entry.getKey(), b[0], b[4], b[1], b[5], slaRate));
        }
        list.sort(Comparator.comparingLong(DepartmentPerformanceDTO::openBacklogSnapshot).reversed()
                .thenComparing(Comparator.comparingLong(DepartmentPerformanceDTO::totalSubmittedInPeriod).reversed()));
        return list;
    }

    public List<ServicePopularityDTO> getServicePopularity(LocalDate startDate, LocalDate endDate,
                                                           String department, String category, int limit, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);
        return computeServicePopularity(window, scopedDept, category, limit);
    }

    private List<ServicePopularityDTO> computeServicePopularity(DateWindow window, String scopedDept, String category, int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 25);
        String normalizedCategory = (category != null && !category.isBlank()) ? category.trim() : null;

        List<Object[]> rows = ticketRepository.findServicePopularity(
                window.startDateTime(), window.endDateTime(), scopedDept, normalizedCategory,
                PageRequest.of(0, boundedLimit));

        List<ServicePopularityDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ServicePopularityDTO(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    ((Number) row[5]).longValue()
            ));
        }
        return result;
    }

    public SlaComplianceDTO getSlaCompliance(LocalDate startDate, LocalDate endDate,
                                             String department, TicketPriority priority, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);
        LocalDateTime now = LocalDateTime.now(clock);

        List<Object[]> rows = ticketRepository.findAnalyticsProjectionInPeriod(
                window.startDateTime(), window.endDateTime(), scopedDept, priority);

        long evaluated = 0;
        long met = 0;
        long breached = 0;
        long activeOverdue = 0;
        long resolutionMinutesSum = 0;
        long resolutionMinutesCount = 0;

        for (Object[] row : rows) {
            TicketStatus status = (TicketStatus) row[1];
            LocalDateTime createdAt = (LocalDateTime) row[2];
            LocalDateTime resolvedAt = (LocalDateTime) row[3];
            LocalDateTime slaTarget = (LocalDateTime) row[4];
            Integer pausedMinutes = row[5] != null ? ((Number) row[5]).intValue() : 0;

            if (status == TicketStatus.CANCELLED || status == TicketStatus.REJECTED) {
                continue;
            }

            if ((status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) && resolvedAt != null) {
                if (slaTarget != null) {
                    evaluated++;
                    if (!resolvedAt.isAfter(slaTarget)) {
                        met++;
                    } else {
                        breached++;
                    }
                }
                if (createdAt != null) {
                    long elapsed = Math.max(0L, Duration.between(createdAt, resolvedAt).toMinutes() - pausedMinutes);
                    resolutionMinutesSum += elapsed;
                    resolutionMinutesCount++;
                }
            } else if (ACTIVE_CLOCK_STATUSES.contains(status) && slaTarget != null && now.isAfter(slaTarget)) {
                evaluated++;
                breached++;
                activeOverdue++;
            }
        }

        Double pct = evaluated > 0 ? round2((met * 100.0) / evaluated) : null;
        Double avgHours = resolutionMinutesCount > 0
                ? round2((resolutionMinutesSum / (double) resolutionMinutesCount) / 60.0)
                : null;

        return new SlaComplianceDTO(evaluated, met, breached, activeOverdue, pct, avgHours);
    }

    public ApprovalOverviewDTO getApprovalsOverview(LocalDate startDate, LocalDate endDate,
                                                    String department, String username) {
        String scopedDept = resolveAuthorizedDepartmentScope(department, username);
        DateWindow window = resolveDateWindow(startDate, endDate);
        return computeApprovalsOverview(window, scopedDept, LocalDateTime.now(clock));
    }

    private ApprovalOverviewDTO computeApprovalsOverview(DateWindow window, String scopedDept, LocalDateTime now) {
        List<Object[]> pendingByStage = approvalTaskRepository.countCurrentByStatusGroupedByStage(
                ApprovalStatus.PENDING, scopedDept);
        long stage1Pending = 0;
        long stage2Pending = 0;
        long totalPending = 0;
        for (Object[] row : pendingByStage) {
            int stage = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            totalPending += count;
            if (stage == 1) stage1Pending = count;
            else if (stage == 2) stage2Pending = count;
        }

        long overduePending = approvalTaskRepository.countOverduePendingApprovals(
                ApprovalStatus.PENDING, now.minusHours(APPROVAL_OVERDUE_HOURS), scopedDept);

        List<Object[]> periodDecisions = approvalTaskRepository.countGroupedByStatusInPeriod(
                window.startDateTime(), window.endDateTime(), scopedDept);
        long approvedInPeriod = 0;
        long rejectedInPeriod = 0;
        for (Object[] row : periodDecisions) {
            ApprovalStatus st = (ApprovalStatus) row[0];
            long c = ((Number) row[1]).longValue();
            if (st == ApprovalStatus.APPROVED) approvedInPeriod = c;
            else if (st == ApprovalStatus.REJECTED) rejectedInPeriod = c;
        }

        return new ApprovalOverviewDTO(
                totalPending, stage1Pending, stage2Pending, overduePending, approvedInPeriod, rejectedInPeriod);
    }

    /**
     * Authoritative department scope enforcement:
     * - ADMIN / AUDITOR: organization-wide access (optional department filter).
     * - MANAGER / DEPT_HEAD / Specialist resolvers: strictly locked to their own profile department.
     * - Standard EMPLOYEE: rejected with 403 Forbidden.
     */
    public String resolveAuthorizedDepartmentScope(String requestedDepartment, String username) {
        if (username == null || username.isBlank()) {
            throw new UnauthorizedScopeException("Authenticated user identity is required for analytics.");
        }

        String normalizedReq = (requestedDepartment != null && !requestedDepartment.isBlank()
                && !"ALL".equalsIgnoreCase(requestedDepartment.trim()))
                ? requestedDepartment.trim()
                : null;

        if (securityEvaluator.isAdmin(username) || securityEvaluator.isAuditor(username)) {
            return normalizedReq;
        }

        boolean isManagerOrSpecialist = securityEvaluator.hasRole(username, "ROLE_MANAGER")
                || securityEvaluator.hasRole(username, "ROLE_HR_MANAGER")
                || securityEvaluator.isResolverRole(username);

        if (!isManagerOrSpecialist) {
            throw new UnauthorizedScopeException(
                    "Operational analytics require Administrator, Auditor, Manager, or Department Specialist privileges.");
        }

        Optional<Profile> profileOpt = profileRepo.findByUserUsername(username);
        if (profileOpt.isEmpty() || profileOpt.get().getDepartment() == null || profileOpt.get().getDepartment().isBlank()) {
            throw new UnauthorizedScopeException(
                    "Department scope could not be resolved from your employee profile.");
        }

        String userDept = profileOpt.get().getDepartment().trim();
        if (normalizedReq != null && !normalizedReq.equalsIgnoreCase(userDept)) {
            throw new UnauthorizedScopeException(
                    "Cross-department analytics access is forbidden. Your scope is restricted to department: " + userDept);
        }

        return userDept;
    }

    private DateWindow resolveDateWindow(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveEnd = endDate != null ? endDate : today;
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(29);

        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new IllegalArgumentException("startDate (" + effectiveStart + ") must not be after endDate (" + effectiveEnd + ").");
        }
        long days = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd);
        if (days > MAX_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException("Analytics date range cannot exceed " + MAX_DATE_RANGE_DAYS + " days.");
        }

        return new DateWindow(
                effectiveStart,
                effectiveEnd,
                effectiveStart.atStartOfDay(),
                effectiveEnd.atTime(LocalTime.MAX)
        );
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private record DateWindow(LocalDate startDate, LocalDate endDate,
                              LocalDateTime startDateTime, LocalDateTime endDateTime) {}
}
