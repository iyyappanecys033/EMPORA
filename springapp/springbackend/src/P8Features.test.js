import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import { AuthProvider } from './services/AuthContext';
import api from './services/api';
import notificationService from './services/notificationService';
import analyticsService from './services/analyticsService';
import NotificationBell from './components/notifications/NotificationBell';
import NotificationsPage from './components/notifications/NotificationsPage';
import AnalyticsDashboard from './components/analytics/AnalyticsDashboard';

describe('P8 Notification & Analytics Service Contracts', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  test('notificationService calls exact P8 backend notification endpoints', async () => {
    const getSpy = jest.spyOn(api, 'get')
      .mockResolvedValueOnce({ data: { content: [], totalElements: 0 } })
      .mockResolvedValueOnce({ data: { unreadCount: 4 } });
    const putSpy = jest.spyOn(api, 'put')
      .mockResolvedValueOnce({ data: { id: 10, isRead: true } })
      .mockResolvedValueOnce({ data: { updatedCount: 4, unreadCount: 0 } });

    await notificationService.getMyNotifications({ page: 0, size: 15, unreadOnly: true });
    expect(getSpy).toHaveBeenCalledWith('/v1/notifications/my', {
      params: { page: 0, size: 15, unreadOnly: true }
    });

    const countRes = await notificationService.getUnreadCount();
    expect(getSpy).toHaveBeenCalledWith('/v1/notifications/my/unread-count');
    expect(countRes.unreadCount).toBe(4);

    await notificationService.markAsRead(10);
    expect(putSpy).toHaveBeenCalledWith('/v1/notifications/10/read');

    await notificationService.markAllAsRead();
    expect(putSpy).toHaveBeenCalledWith('/v1/notifications/my/read-all');
  });

  test('analyticsService calls exact P8 backend analytics endpoints', async () => {
    const getSpy = jest.spyOn(api, 'get')
      .mockResolvedValueOnce({ data: { overview: { periodTotalTickets: 12 } } })
      .mockResolvedValueOnce({ data: { compliancePercentage: 92.5 } });

    await analyticsService.getDashboardBundle({ startDate: '2026-11-01', endDate: '2026-11-17' });
    expect(getSpy).toHaveBeenCalledWith('/v1/analytics/dashboard', {
      params: { startDate: '2026-11-01', endDate: '2026-11-17' }
    });

    await analyticsService.getSlaCompliance({ department: 'IT' });
    expect(getSpy).toHaveBeenCalledWith('/v1/analytics/sla/compliance', {
      params: { department: 'IT' }
    });
  });
});

describe('P8 Frontend Notification Experience', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  test('NotificationBell shows unread badge, opens popover, and supports mark all read', async () => {
    jest.spyOn(api, 'get').mockImplementation((url) => {
      if (url === '/users/me') {
        return Promise.resolve({
          data: { id: 1, username: 'alice', roles: ['ROLE_EMPLOYEE'], department: 'Engineering' }
        });
      }
      if (url === '/v1/notifications/my/unread-count') {
        return Promise.resolve({ data: { unreadCount: 2 } });
      }
      if (url === '/v1/notifications/my') {
        return Promise.resolve({
          data: {
            content: [
              {
                id: 101,
                eventType: 'TICKET_RESOLVED',
                title: 'Ticket Resolved: TKT-2026-000101',
                message: 'Your service ticket has been resolved.',
                ticketId: 55,
                isRead: false,
                createdAt: '2026-11-17T10:00:00'
              }
            ],
            totalElements: 1
          }
        });
      }
      return Promise.resolve({ data: {} });
    });

    const markAllSpy = jest.spyOn(notificationService, 'markAllAsRead').mockResolvedValueOnce({
      updatedCount: 2,
      unreadCount: 0
    });

    render(
      <BrowserRouter>
        <AuthProvider>
          <NotificationBell />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('unread-badge')).toHaveTextContent('2');
    });

    fireEvent.click(screen.getByRole('button', { name: /Notifications/i }));

    await waitFor(() => {
      expect(screen.getByText('Ticket Resolved: TKT-2026-000101')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Mark all read/i }));

    await waitFor(() => {
      expect(markAllSpy).toHaveBeenCalled();
      expect(screen.queryByTestId('unread-badge')).not.toBeInTheDocument();
    });
  });

  test('NotificationsPage lists alerts and allows marking an individual notification as read', async () => {
    jest.spyOn(notificationService, 'getMyNotifications').mockResolvedValueOnce({
      content: [
        {
          id: 201,
          eventType: 'APPROVAL_REQUIRED',
          title: 'Approval Required for Ticket TKT-2026-000201',
          message: 'Please review the workstation request.',
          relatedEntityType: 'APPROVAL',
          relatedEntityId: 12,
          isRead: false,
          createdAt: '2026-11-17T12:00:00'
        }
      ],
      totalPages: 1,
      totalElements: 1
    });
    jest.spyOn(notificationService, 'getUnreadCount').mockResolvedValueOnce({ unreadCount: 1 });
    const markReadSpy = jest.spyOn(notificationService, 'markAsRead').mockResolvedValueOnce({
      id: 201,
      isRead: true,
      readAt: '2026-11-17T12:05:00'
    });

    render(
      <BrowserRouter>
        <NotificationsPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Approval Required for Ticket TKT-2026-000201')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /^Mark Read$/i }));

    await waitFor(() => {
      expect(markReadSpy).toHaveBeenCalledWith(201);
    });
  });
});

describe('P8 Frontend Business Analytics Dashboard', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  test('AnalyticsDashboard renders KPI cards, 5 priority tiers, department table, and top services', async () => {
    jest.spyOn(api, 'get').mockResolvedValueOnce({
      data: { id: 1, username: 'admin', roles: ['ROLE_ADMIN'], department: 'Operations' }
    });

    jest.spyOn(analyticsService, 'getDashboardBundle').mockResolvedValueOnce({
      overview: {
        periodTotalTickets: 25,
        openTicketsSnapshot: 8,
        resolvedTicketsInPeriod: 15,
        closedTicketsInPeriod: 10,
        cancelledOrRejectedInPeriod: 2,
        overdueOpenTicketsSnapshot: 1,
        slaComplianceRate: 88.5,
        averageResolutionHours: 5.25,
        pendingApprovalsSnapshot: 3,
        scopedDepartment: 'ALL',
        startDate: '2026-10-19',
        endDate: '2026-11-17',
        timezone: 'Asia/Kolkata'
      },
      statusDistribution: [
        { status: 'RESOLVED', count: 15, percentage: 60.0 },
        { status: 'IN_PROGRESS', count: 8, percentage: 32.0 }
      ],
      priorityDistribution: [
        { priority: 'LOW', count: 5, percentage: 20.0 },
        { priority: 'MEDIUM', count: 10, percentage: 40.0 },
        { priority: 'HIGH', count: 5, percentage: 20.0 },
        { priority: 'URGENT', count: 3, percentage: 12.0 },
        { priority: 'CRITICAL', count: 2, percentage: 8.0 }
      ],
      trends: [
        { date: '2026-11-16', submittedCount: 4, resolvedCount: 3 },
        { date: '2026-11-17', submittedCount: 6, resolvedCount: 5 }
      ],
      departmentPerformance: [
        { department: 'IT', totalSubmittedInPeriod: 18, openBacklogSnapshot: 6, resolvedInPeriod: 11, overdueOpenSnapshot: 1, slaComplianceRate: 90.0 }
      ],
      servicePopularity: [
        { catalogItemId: 1, itemCode: 'IT-LAPTOP', title: 'Developer Laptop', department: 'IT', category: 'Hardware', requestCount: 12 }
      ],
      slaCompliance: {
        evaluatedTickets: 16,
        metSlaCount: 14,
        breachedSlaCount: 2,
        activeOverdueCount: 1,
        compliancePercentage: 88.5,
        averageResolutionHours: 5.25
      },
      approvalsOverview: {
        pendingSnapshot: 3,
        stage1PendingSnapshot: 2,
        stage2PendingSnapshot: 1,
        overduePendingSnapshot: 1,
        approvedInPeriod: 14,
        rejectedInPeriod: 2
      }
    });

    render(
      <BrowserRouter>
        <AuthProvider>
          <AnalyticsDashboard />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Operational Analytics & Service Insights')).toBeInTheDocument();
      expect(screen.getByText('25')).toBeInTheDocument();
      expect(screen.getAllByText('88.5%').length).toBeGreaterThan(0);
      expect(screen.getByText('URGENT')).toBeInTheDocument();
      expect(screen.getByText('Developer Laptop')).toBeInTheDocument();
    });
  });

  test('AnalyticsDashboard locks department selector for specialist and displays N/A when SLA is null', async () => {
    jest.spyOn(api, 'get').mockResolvedValueOnce({
      data: { id: 2, username: 'it_dave', roles: ['ROLE_IT_SUPPORT'], department: 'IT' }
    });

    jest.spyOn(analyticsService, 'getDashboardBundle').mockResolvedValueOnce({
      overview: {
        periodTotalTickets: 0,
        openTicketsSnapshot: 0,
        resolvedTicketsInPeriod: 0,
        closedTicketsInPeriod: 0,
        cancelledOrRejectedInPeriod: 0,
        overdueOpenTicketsSnapshot: 0,
        slaComplianceRate: null,
        averageResolutionHours: null,
        pendingApprovalsSnapshot: 0,
        scopedDepartment: 'IT',
        startDate: '2026-10-19',
        endDate: '2026-11-17',
        timezone: 'Asia/Kolkata'
      },
      statusDistribution: [],
      priorityDistribution: [],
      trends: [],
      departmentPerformance: [],
      servicePopularity: [],
      slaCompliance: { evaluatedTickets: 0, metSlaCount: 0, breachedSlaCount: 0, activeOverdueCount: 0, compliancePercentage: null, averageResolutionHours: null },
      approvalsOverview: { pendingSnapshot: 0, stage1PendingSnapshot: 0, stage2PendingSnapshot: 0, overduePendingSnapshot: 0, approvedInPeriod: 0, rejectedInPeriod: 0 }
    });

    render(
      <BrowserRouter>
        <AuthProvider>
          <AnalyticsDashboard />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      const deptInput = screen.getByLabelText(/Department Scope/i);
      expect(deptInput).toBeDisabled();
      expect(deptInput.value).toBe('IT');
      expect(screen.getAllByText('N/A').length).toBeGreaterThan(0);
    });
  });

  test('AnalyticsDashboard renders RFC 7807 error banner when backend rejects unauthorized scope', async () => {
    jest.spyOn(api, 'get').mockResolvedValueOnce({
      data: { id: 3, username: 'emp_charlie', roles: ['ROLE_EMPLOYEE'], department: 'Finance' }
    });

    jest.spyOn(analyticsService, 'getDashboardBundle').mockRejectedValueOnce({
      title: 'Forbidden',
      detail: 'Operational analytics require Administrator, Auditor, Manager, or Department Specialist privileges.',
      status: 403,
      errors: []
    });

    render(
      <BrowserRouter>
        <AuthProvider>
          <AnalyticsDashboard />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Operational analytics require Administrator/i)).toBeInTheDocument();
    });
  });
});

