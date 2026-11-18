import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import axios from 'axios';

import api, { getCookie, formatApiError } from './services/api';
import ticketService from './services/ticketService';
import analyticsService from './services/analyticsService';
import * as AuthModule from './services/AuthContext';
import { PreferencesProvider, usePreferences } from './services/PreferencesContext';
import { ToastProvider } from './services/ToastContext';

import TicketDetail from './components/tickets/TicketDetail';
import EmployeeDashboard from './components/dashboard/EmployeeDashboard';
import GlobalSearchModal from './components/common/GlobalSearchModal';
import AppLayout from './components/layout/AppLayout';
import Navbar from './components/layout/Navbar';
import ServiceCatalog from './components/catalog/ServiceCatalog';
import MyTickets from './components/tickets/MyTickets';
import NotificationsPage from './components/notifications/NotificationsPage';
import NotificationBell from './components/notifications/NotificationBell';
import TicketComments from './components/tickets/TicketComments';
import LoginModal from './components/LoginModal';
import notificationService from './services/notificationService';

const baseAuth = {
  currentUser: {
    id: 10,
    username: 'john_doe',
    fullName: 'John Doe',
    department: 'IT',
    designation: 'Senior Engineer',
    employeeId: 'EMP-100',
    roles: ['ROLE_EMPLOYEE']
  },
  isAuthenticated: true,
  loading: false,
  isAdmin: false,
  isResolver: false,
  isApprover: false,
  isAuditor: false,
  canViewAnalytics: false,
  department: 'IT',
  login: jest.fn(),
  register: jest.fn(),
  logout: jest.fn().mockResolvedValue(undefined),
  refreshUser: jest.fn()
};

const renderWithProviders = (ui, { auth = baseAuth, initialEntries = ['/'] } = {}) => {
  jest.spyOn(AuthModule, 'useAuth').mockReturnValue({ ...baseAuth, ...auth });
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <PreferencesProvider>
        <ToastProvider>
          {ui}
        </ToastProvider>
      </PreferencesProvider>
    </MemoryRouter>
  );
};

describe('P9.6.1 Critical Frontend Workflow & Branch Coverage Suite', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
    window.localStorage.clear();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  // =========================================================================
  // 1. API CLIENT, CSRF, 401 SILENT REFRESH QUEUE & RFC 7807 ERROR FORMATTING
  // =========================================================================
  describe('API client interceptors & RFC 7807 error parser', () => {
    test('attaches X-XSRF-TOKEN on mutating requests and skips GET requests', () => {
      document.cookie = 'XSRF-TOKEN=csrf-abc-123; path=/';
      expect(getCookie('XSRF-TOKEN')).toBe('csrf-abc-123');
      expect(getCookie('MISSING_COOKIE')).toBeNull();

      const reqHandler = api.interceptors.request.handlers[0].fulfilled;
      const postCfg = reqHandler({ method: 'post', headers: {} });
      expect(postCfg.headers['X-XSRF-TOKEN']).toBe('csrf-abc-123');

      const getCfg = reqHandler({ method: 'get', headers: {} });
      expect(getCfg.headers['X-XSRF-TOKEN']).toBeUndefined();
    });

    test('formatApiError handles network errors, RFC 7807 maps/arrays, 403/409/400 defaults & string payloads', () => {
      expect(formatApiError({ message: 'ECONNREFUSED' })).toMatchObject({
        title: 'Network Error',
        status: 0,
        detail: 'ECONNREFUSED'
      });

      const mapped = formatApiError({
        response: { status: 400, data: { errors: { email: 'invalid email', username: 'required' } } }
      });
      expect(mapped.title).toBe('Bad Request');
      expect(mapped.errors).toEqual([
        { field: 'email', message: 'invalid email' },
        { field: 'username', message: 'required' }
      ]);

      const conflict = formatApiError({
        response: { status: 409, data: { detail: 'Stale version', errors: [{ field: 'version', message: 'mismatch' }] } }
      });
      expect(conflict.title).toBe('Conflict');
      expect(conflict.errors).toHaveLength(1);

      const forbidden = formatApiError({ response: { status: 403, data: {} } });
      expect(forbidden.detail).toMatch(/Access Denied/i);

      const plainText = formatApiError({ response: { status: 502, data: 'Bad Gateway' } });
      expect(plainText).toMatchObject({ title: 'Error (502)', detail: 'Bad Gateway', status: 502 });
    });

    test('401 response triggers silent /api/auth/refresh and dispatches session-expired when refresh fails', async () => {
      const respRejected = api.interceptors.response.handlers[0].rejected;
      const postSpy = jest.spyOn(axios, 'post').mockRejectedValueOnce({
        response: { status: 401, data: { detail: 'Refresh token expired' } }
      });
      const expiredListener = jest.fn();
      window.addEventListener('session-expired', expiredListener);

      await expect(
        respRejected({
          response: { status: 401, data: { detail: 'Expired' } },
          config: { url: '/v1/tickets/my', headers: {} }
        })
      ).rejects.toMatchObject({ status: 401, detail: 'Refresh token expired' });

      expect(postSpy).toHaveBeenCalledWith('/api/auth/refresh', {}, { withCredentials: true });
      expect(expiredListener).toHaveBeenCalledTimes(1);
      window.removeEventListener('session-expired', expiredListener);
    });

    test('401 response retries original request after successful silent token refresh', async () => {
      const respRejected = api.interceptors.response.handlers[0].rejected;
      jest.spyOn(axios, 'post').mockResolvedValueOnce({ status: 200, data: {} });
      const origAdapter = api.defaults.adapter;
      api.defaults.adapter = jest.fn().mockResolvedValueOnce({ status: 200, data: { recovered: true }, headers: {}, config: {} });

      const retried = await respRejected({
        response: { status: 401, data: { detail: 'Expired' } },
        config: { url: '/v1/catalog', method: 'get', headers: {} }
      });
      expect(retried.data).toEqual({ recovered: true });
      api.defaults.adapter = origAdapter;
    });
  });

  // =========================================================================
  // 2. SERVICE LAYER CONTRACT TESTS (ticketService & analyticsService)
  // =========================================================================
  describe('ticketService & analyticsService REST contract methods', () => {
    test('ticketService calls exact REST endpoints with encoded params and bodies', async () => {
      const getSpy = jest.spyOn(api, 'get').mockResolvedValue({ data: { ok: true } });
      const postSpy = jest.spyOn(api, 'post').mockResolvedValue({ data: { ok: true } });

      await ticketService.getCatalogItemById(42);
      await ticketService.submitTicket({ catalogItemId: 1, department: 'IT', priority: 'HIGH', description: 'Need laptop' });
      await ticketService.getTicketById(7);
      await ticketService.assignTicket(7, 99);
      await ticketService.updateTicketStatus(7, 'IN_PROGRESS', 'Working on it', 2);
      await ticketService.cancelTicket(7, 'Duplicate request', 2);
      await ticketService.reopenTicket(7, 'Still broken after fix', 3);
      await ticketService.getComments(7);
      await ticketService.addComment(7, 'Internal triage note', true);
      await ticketService.getDepartmentQueue('IT & Cloud', { page: 0 });
      await ticketService.getPendingApprovals();
      await ticketService.getApprovalHistory(7);
      await ticketService.decideApproval(11, 'APPROVED', 'Approved by manager');
      await ticketService.getAuditLogs(0, 20);
      await ticketService.verifyAuditChain();

      expect(getSpy).toHaveBeenCalledWith('/v1/catalog/42');
      expect(getSpy).toHaveBeenCalledWith('/v1/departments/IT%20%26%20Cloud/queue', { params: { page: 0 } });
      expect(postSpy).toHaveBeenCalledWith('/v1/tickets/7/status', { status: 'IN_PROGRESS', reason: 'Working on it', version: 2 });
      expect(postSpy).toHaveBeenCalledWith('/v1/tickets/7/cancel', { reason: 'Duplicate request', version: 2 });
      expect(postSpy).toHaveBeenCalledWith('/v1/tickets/7/reopen', { reason: 'Still broken after fix', version: 3 });
      expect(postSpy).toHaveBeenCalledWith('/v1/tickets/7/comments', { commentText: 'Internal triage note', isInternal: true });
      expect(postSpy).toHaveBeenCalledWith('/v1/approvals/11/decide', { decision: 'APPROVED', reason: 'Approved by manager' });
    });

    test('analyticsService invokes all 9 analytics REST endpoints', async () => {
      const getSpy = jest.spyOn(api, 'get').mockResolvedValue({ data: { ok: true } });
      const params = { startDate: '2026-10-01', endDate: '2026-11-01' };

      await analyticsService.getDashboardBundle(params);
      await analyticsService.getOverview(params);
      await analyticsService.getStatusDistribution(params);
      await analyticsService.getPriorityDistribution(params);
      await analyticsService.getTrends(params);
      await analyticsService.getDepartmentPerformance(params);
      await analyticsService.getServicePopularity(params);
      await analyticsService.getSlaCompliance(params);
      await analyticsService.getApprovalsOverview(params);

      expect(getSpy).toHaveBeenCalledTimes(9);
      expect(getSpy).toHaveBeenCalledWith('/v1/analytics/sla/compliance', { params });
      expect(getSpy).toHaveBeenCalledWith('/v1/analytics/approvals/overview', { params });
    });
  });

  // =========================================================================
  // 3. TICKET DETAIL: LIFECYCLE TRANSITIONS, CANCEL/REOPEN, SELF-ASSIGN, 409
  // =========================================================================
  describe('TicketDetail full lifecycle & error handling', () => {
    const mountDetail = (authOverride = {}) =>
      renderWithProviders(
        <Routes>
          <Route path="/tickets/:id" element={<TicketDetail />} />
        </Routes>,
        { auth: { ...baseAuth, ...authOverride }, initialEntries: ['/tickets/55'] }
      );

    test('shows error alert with Go Back when ticket load fails', async () => {
      jest.spyOn(ticketService, 'getTicketById').mockRejectedValueOnce({
        title: 'Access Denied',
        detail: 'You do not have permission to view this ticket.'
      });

      mountDetail();
      expect(await screen.findByText(/You do not have permission to view this ticket/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Go Back/i })).toBeInTheDocument();
    });

    test('requester can cancel an ASSIGNED ticket with reason and sees overdue SLA indicator', async () => {
      const ticket = {
        id: 55,
        ticketNumber: 'TKT-2026-00055',
        status: 'ASSIGNED',
        priority: 'HIGH',
        department: 'IT',
        catalogItemTitle: 'VPN Access',
        description: 'Need VPN access for client deployment',
        requesterUsername: 'john_doe',
        assigneeUsername: 'it_support',
        slaTargetDate: '2020-01-01T10:00:00',
        totalPausedMinutes: 30,
        version: 2
      };
      jest.spyOn(ticketService, 'getTicketById').mockResolvedValue(ticket);
      jest.spyOn(ticketService, 'getComments').mockResolvedValue([]);
      const cancelSpy = jest.spyOn(ticketService, 'cancelTicket').mockResolvedValue({ ...ticket, status: 'CANCELLED' });

      mountDetail();
      expect(await screen.findByText('TKT-2026-00055')).toBeInTheDocument();
      expect(screen.getByText(/\(Overdue\)/i)).toBeInTheDocument();
      expect(screen.getByText(/30 minutes/i)).toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: /Cancel Ticket/i }));
      expect(screen.getByRole('dialog')).toBeInTheDocument();

      // Empty reason validation guard
      fireEvent.submit(screen.getByRole('dialog').querySelector('form'));
      expect(await screen.findByText(/A reason must be provided/i)).toBeInTheDocument();

      fireEvent.change(screen.getByLabelText(/Reason \/ Diagnostic Notes/i), {
        target: { value: 'No longer needed after network migration' }
      });
      fireEvent.click(screen.getByRole('button', { name: /Confirm Action/i }));

      await waitFor(() =>
        expect(cancelSpy).toHaveBeenCalledWith(55, 'No longer needed after network migration', 2)
      );
    });

    test('requester can reopen RESOLVED ticket and IT specialist can self-assign & transition statuses', async () => {
      const resolvedTicket = {
        id: 55,
        ticketNumber: 'TKT-2026-00055',
        status: 'RESOLVED',
        priority: 'MEDIUM',
        department: 'IT',
        description: 'Laptop keyboard malfunction',
        requesterUsername: 'john_doe',
        assigneeUsername: 'it_support',
        slaTargetDate: '2027-01-01T10:00:00',
        totalPausedMinutes: 0,
        version: 4
      };
      jest.spyOn(ticketService, 'getTicketById').mockResolvedValue(resolvedTicket);
      jest.spyOn(ticketService, 'getComments').mockResolvedValue([]);
      const reopenSpy = jest.spyOn(ticketService, 'reopenTicket').mockResolvedValue({ ...resolvedTicket, status: 'IN_PROGRESS' });

      const { unmount } = mountDetail();
      expect(await screen.findByRole('button', { name: /Reopen Ticket/i })).toBeInTheDocument();
      fireEvent.click(screen.getByRole('button', { name: /Reopen Ticket/i }));
      fireEvent.change(screen.getByLabelText(/Reason \/ Diagnostic Notes/i), {
        target: { value: 'Issue reoccurred after reboot' }
      });
      fireEvent.click(screen.getByRole('button', { name: /Confirm Action/i }));
      await waitFor(() => expect(reopenSpy).toHaveBeenCalledWith(55, 'Issue reoccurred after reboot', 4));
      unmount();

      // Specialist workflow: unassigned IN_PROGRESS -> Put On Hold / Mark Resolved + Self-Assign
      const inProgressTicket = {
        ...resolvedTicket,
        status: 'IN_PROGRESS',
        assigneeUsername: null,
        version: 5
      };
      jest.spyOn(ticketService, 'getTicketById').mockResolvedValue(inProgressTicket);
      const assignSpy = jest.spyOn(ticketService, 'assignTicket').mockResolvedValue({ ...inProgressTicket, assigneeUsername: 'it_support' });
      const statusSpy = jest.spyOn(ticketService, 'updateTicketStatus').mockRejectedValueOnce({
        title: 'Conflict',
        detail: 'Ticket was modified concurrently (409).'
      });

      mountDetail({
        currentUser: { id: 22, username: 'it_support', department: 'IT', roles: ['ROLE_IT_SUPPORT'] },
        isResolver: true
      });

      const assignBtn = await screen.findByRole('button', { name: /Assign to Me/i });
      fireEvent.click(assignBtn);
      await waitFor(() => expect(assignSpy).toHaveBeenCalledWith(55, 22));

      fireEvent.click(await screen.findByRole('button', { name: /Put On Hold/i }));
      fireEvent.click(await screen.findByRole('button', { name: /Confirm Action/i }));
      await waitFor(() =>
        expect(statusSpy).toHaveBeenCalledWith(
          55,
          'ON_HOLD',
          'Awaiting requester clarification or external dependency.',
          5
        )
      );
      expect(await screen.findByText(/Ticket was modified concurrently/i)).toBeInTheDocument();
    });

    test('specialist sees Resume Progress on ON_HOLD and Admin sees Close Ticket on RESOLVED', async () => {
      const onHoldTicket = {
        id: 55,
        ticketNumber: 'TKT-2026-00055',
        status: 'ON_HOLD',
        priority: 'LOW',
        department: 'IT',
        description: 'Waiting on vendor',
        requesterUsername: 'john_doe',
        assigneeUsername: 'admin',
        version: 6
      };
      jest.spyOn(ticketService, 'getTicketById').mockResolvedValue(onHoldTicket);
      jest.spyOn(ticketService, 'getComments').mockResolvedValue([]);
      const statusSpy = jest.spyOn(ticketService, 'updateTicketStatus').mockResolvedValue({ ...onHoldTicket, status: 'IN_PROGRESS' });

      const { unmount } = mountDetail({
        currentUser: { id: 1, username: 'admin', department: 'IT', roles: ['ROLE_ADMIN'] },
        isAdmin: true
      });
      fireEvent.click(await screen.findByRole('button', { name: /Resume Progress/i }));
      fireEvent.keyDown(window, { key: 'Escape' });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: /Resume Progress/i }));
      fireEvent.click(screen.getByRole('button', { name: /Confirm Action/i }));
      await waitFor(() => expect(statusSpy).toHaveBeenCalledWith(55, 'IN_PROGRESS', 'Resuming resolution activity.', 6));
      unmount();

      jest.spyOn(ticketService, 'getTicketById').mockResolvedValue({ ...onHoldTicket, status: 'RESOLVED' });
      mountDetail({
        currentUser: { id: 1, username: 'admin', department: 'IT', roles: ['ROLE_ADMIN'] },
        isAdmin: true
      });
      expect(await screen.findByRole('button', { name: /Close Ticket/i })).toBeInTheDocument();
    });
  });

  // =========================================================================
  // 4. EMPLOYEE DASHBOARD: UNAUTHENTICATED, AUTHENTICATED, ROLE SHORTCUTS
  // =========================================================================
  describe('EmployeeDashboard unauthenticated & role-aware authenticated states', () => {
    test('renders guest portal hero and opens login modal when unauthenticated', () => {
      const onOpenLogin = jest.fn();
      renderWithProviders(<EmployeeDashboard onOpenLogin={onOpenLogin} />, {
        auth: { ...baseAuth, isAuthenticated: false, currentUser: null }
      });

      expect(screen.getByText(/Welcome to Your Employee Service Portal/i)).toBeInTheDocument();
      fireEvent.click(screen.getByRole('button', { name: /Sign In to Workspace/i }));
      expect(onOpenLogin).toHaveBeenCalledTimes(1);
    });

    test('loads KPIs, role shortcuts, recent tickets, featured services & notification previews', async () => {
      jest.spyOn(ticketService, 'getMyTickets').mockResolvedValue({
        totalElements: 3,
        content: [
          { id: 101, ticketNumber: 'TKT-101', status: 'IN_PROGRESS', priority: 'HIGH', description: 'Docker access' },
          { id: 102, ticketNumber: 'TKT-102', status: 'CLOSED', priority: 'LOW', description: 'Monitor stand' }
        ]
      });
      jest.spyOn(ticketService, 'getCatalogItems').mockResolvedValue({
        content: [{ id: 1, title: 'VPN Token', department: 'IT', slaTargetHours: 8 }]
      });
      jest.spyOn(notificationService, 'getMyNotifications').mockResolvedValue({
        content: [
          { id: 1, title: 'Approval Needed', message: 'Review TKT-200', relatedEntityType: 'APPROVAL', isRead: false },
          { id: 2, title: 'Ticket Updated', message: 'TKT-101 in progress', ticketId: 101, isRead: true }
        ]
      });
      jest.spyOn(notificationService, 'getUnreadCount').mockResolvedValue({ unreadCount: 1 });

      renderWithProviders(<EmployeeDashboard />, {
        auth: { ...baseAuth, isApprover: true, isResolver: true, department: 'IT' }
      });

      expect(await screen.findByText('TKT-101')).toBeInTheDocument();
      expect(screen.getByText(/Pending Approvals/i)).toBeInTheDocument();
      expect(screen.getByText(/Review Approvals/i)).toBeInTheDocument();
      expect(screen.getAllByText(/IT Queue/i).length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText(/VPN Token/i)).toBeInTheDocument();
      expect(screen.getByText(/Approval Needed/i)).toBeInTheDocument();

      // Quick Request opens CreateTicketModal
      fireEvent.click(screen.getByRole('button', { name: /^Request$/i }));
      expect(await screen.findByRole('dialog')).toBeInTheDocument();
    });
  });

  // =========================================================================
  // 5. GLOBAL SEARCH MODAL, NAVBAR, APPLAYOUT & PREFERENCES CONTEXT
  // =========================================================================
  describe('GlobalSearchModal, Navbar, AppLayout & PreferencesContext', () => {
    test('GlobalSearchModal filters workspaces, catalog & tickets across all roles and handles Escape', async () => {
      jest.spyOn(ticketService, 'getCatalogItems').mockResolvedValue({
        content: [{ id: 9, title: 'MacBook Pro Provisioning', department: 'IT', category: 'Hardware', slaTargetHours: 48 }]
      });
      jest.spyOn(ticketService, 'getMyTickets').mockResolvedValue({
        content: [{ id: 88, ticketNumber: 'TKT-2026-00088', description: 'VPN cert renewal' }]
      });

      const onClose = jest.fn();
      renderWithProviders(<GlobalSearchModal isOpen onClose={onClose} />, {
        auth: {
          ...baseAuth,
          isAdmin: true,
          isApprover: true,
          isResolver: true,
          isAuditor: true,
          canViewAnalytics: true
        }
      });

      expect(await screen.findByText('MacBook Pro Provisioning')).toBeInTheDocument();
      expect(screen.getByText('TKT-2026-00088')).toBeInTheDocument();
      expect(screen.getByText('Cryptographic Audit Ledger')).toBeInTheDocument();

      const input = screen.getByLabelText(/Search workspaces, services, and recent tickets/i);
      fireEvent.change(input, { target: { value: 'nonexistent-xyz-query' } });
      expect(screen.getByText(/No matching workspaces/i)).toBeInTheDocument();

      fireEvent.change(input, { target: { value: 'MacBook' } });
      fireEvent.click(screen.getByText('MacBook Pro Provisioning'));
      expect(onClose).toHaveBeenCalledTimes(1);
    });

    test('AppLayout handles Ctrl+K search toggle, Navbar theme cycling, profile menu & Sign Out', async () => {
      jest.spyOn(notificationService, 'getUnreadCount').mockResolvedValue({ unreadCount: 0 });
      jest.spyOn(ticketService, 'getCatalogItems').mockResolvedValue({ content: [] });
      jest.spyOn(ticketService, 'getMyTickets').mockResolvedValue({ content: [] });
      const logoutSpy = jest.fn().mockResolvedValue(undefined);

      renderWithProviders(
        <AppLayout onOpenLogin={jest.fn()}>
          <div>Workspace Content</div>
        </AppLayout>,
        { auth: { ...baseAuth, logout: logoutSpy } }
      );

      // Ctrl+K opens quick search
      fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
      expect(await screen.findByRole('dialog', { name: /Quick search EPAMS workspace/i })).toBeInTheDocument();
      fireEvent.keyDown(window, { key: 'Escape' });

      // Theme cycling button (system -> light -> dark -> system)
      const themeBtn = screen.getByRole('button', { name: /Switch theme/i });
      fireEvent.click(themeBtn);
      expect(document.documentElement.getAttribute('data-theme')).toBe('light');
      fireEvent.click(themeBtn);
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

      // Profile dropdown & Sign Out
      fireEvent.click(screen.getByRole('button', { name: /Account and profile menu/i }));
      expect(screen.getByRole('menu', { name: /Employee profile menu/i })).toBeInTheDocument();
      fireEvent.click(screen.getByRole('menuitem', { name: /Sign Out/i }));
      await waitFor(() => expect(logoutSpy).toHaveBeenCalledTimes(1));
    });

    test('PreferencesProvider recovers from corrupt localStorage and supports reset & sidebar toggle', () => {
      window.localStorage.setItem('epams.preferences', '{broken-json');
      const Probe = () => {
        const p = usePreferences();
        return (
          <div>
            <span data-testid="pref-state">{`${p.theme}|${p.density}|${p.sidebarCollapsed}`}</span>
            <button type="button" onClick={() => p.updatePreference('density', 'compact')}>Compact</button>
            <button type="button" onClick={p.toggleSidebarCollapsed}>Collapse</button>
            <button type="button" onClick={p.resetPreferences}>Reset</button>
          </div>
        );
      };

      render(
        <PreferencesProvider>
          <Probe />
        </PreferencesProvider>
      );

      expect(screen.getByTestId('pref-state')).toHaveTextContent('system|comfortable|false');
      fireEvent.click(screen.getByText('Compact'));
      fireEvent.click(screen.getByText('Collapse'));
      expect(screen.getByTestId('pref-state')).toHaveTextContent('system|compact|true');
      fireEvent.click(screen.getByText('Reset'));
      expect(screen.getByTestId('pref-state')).toHaveTextContent('system|comfortable|false');
    });

    test('Navbar renders Sign In / Register button when unauthenticated', () => {
      const onOpenLogin = jest.fn();
      renderWithProviders(<Navbar onOpenLogin={onOpenLogin} />, {
        auth: { ...baseAuth, isAuthenticated: false, currentUser: null }
      });
      fireEvent.click(screen.getByRole('button', { name: /Sign In \/ Register/i }));
      expect(onOpenLogin).toHaveBeenCalledTimes(1);
    });
  });

  // =========================================================================
  // 6. SERVICE CATALOG, MY TICKETS, NOTIFICATIONS PAGE & BELL
  // =========================================================================
  describe('ServiceCatalog, MyTickets & Notifications workflows', () => {
    test('ServiceCatalog filters by department/category/search, resets empty state, and blocks guest request', async () => {
      jest.spyOn(ticketService, 'getCatalogItems').mockResolvedValue({
        totalPages: 2,
        totalElements: 12,
        content: [
          {
            id: 1,
            itemCode: 'IT-001',
            title: 'Dev Laptop',
            department: 'IT',
            category: 'Hardware',
            description: 'MacBook Pro 16',
            estimatedCost: 2200,
            slaTargetHours: 48,
            requiresManagerApproval: true,
            requiresSecondaryApproval: true
          },
          {
            id: 2,
            itemCode: 'HR-001',
            title: 'Employment Letter',
            department: 'HR',
            category: 'Documents',
            description: 'Official HR verification letter',
            estimatedCost: 0,
            slaTargetHours: 24
          }
        ]
      });

      renderWithProviders(<ServiceCatalog />, {
        auth: { ...baseAuth, isAuthenticated: false, currentUser: null }
      });

      expect(await screen.findByText('Dev Laptop')).toBeInTheDocument();
      expect(screen.getByText('Manager Approval')).toBeInTheDocument();
      expect(screen.getByText('Secondary Approval')).toBeInTheDocument();

      // Category chip filter
      fireEvent.click(screen.getByRole('button', { name: 'Documents' }));
      expect(screen.queryByText('Dev Laptop')).not.toBeInTheDocument();
      expect(screen.getByText('Employment Letter')).toBeInTheDocument();

      // Search producing empty state + Clear Filters
      fireEvent.change(screen.getByLabelText(/Search service catalog/i), { target: { value: 'zzzzz' } });
      expect(screen.getByText(/No catalog items found/i)).toBeInTheDocument();
      fireEvent.click(screen.getByRole('button', { name: /Clear Filters/i }));
      expect(screen.getByText('Dev Laptop')).toBeInTheDocument();

      // Guest Request Service click shows Authentication Required alert
      fireEvent.click(screen.getAllByRole('button', { name: /Request Service/i })[0]);
      expect(screen.getByText(/Please sign in to request services/i)).toBeInTheDocument();
    });

    test('MyTickets supports Table/Card toggle, priority sort, status filter & empty state', async () => {
      jest.spyOn(ticketService, 'getMyTickets').mockResolvedValue({
        totalPages: 1,
        totalElements: 2,
        content: [
          { id: 1, ticketNumber: 'TKT-001', department: 'IT', description: 'Low priority fix', priority: 'LOW', status: 'SUBMITTED', createdAt: '2026-11-01T10:00:00' },
          { id: 2, ticketNumber: 'TKT-002', department: 'HR', description: 'Critical payroll issue', priority: 'CRITICAL', status: 'IN_PROGRESS', createdAt: '2026-11-10T10:00:00' }
        ]
      });

      renderWithProviders(<MyTickets />);
      expect(await screen.findByText('TKT-001')).toBeInTheDocument();

      // Switch to Card view & sort by Highest Priority
      fireEvent.click(screen.getByRole('button', { name: /Card view/i }));
      fireEvent.change(screen.getByLabelText(/Sort tickets/i), { target: { value: 'priority' } });
      expect(screen.getByText('Critical payroll issue')).toBeInTheDocument();

      // Filter by status & search empty state
      fireEvent.change(screen.getByLabelText(/Filter by ticket status/i), { target: { value: 'RESOLVED' } });
      expect(screen.getByText(/No tickets on this page match your selected filters/i)).toBeInTheDocument();
    });

    test('NotificationsPage filters unread/eventType, marks read & Mark All Read, and NotificationBell opens/closes cleanly', async () => {
      const sampleNotifs = [
        { id: 10, alertType: 'APPROVAL_REQUIRED', title: 'Approve Laptop', message: 'Stage 1 pending', isRead: false, relatedEntityType: 'APPROVAL', createdAt: '2026-11-18T10:00:00' },
        { id: 11, alertType: 'TICKET_RESOLVED', title: 'Ticket Resolved', message: 'TKT-001 resolved', isRead: true, ticketId: 1, createdAt: '2026-11-18T09:00:00' }
      ];
      const getNotifsSpy = jest.spyOn(notificationService, 'getMyNotifications').mockResolvedValue({
        content: sampleNotifs,
        totalPages: 1,
        totalElements: 2
      });
      jest.spyOn(notificationService, 'getUnreadCount').mockResolvedValue({ unreadCount: 1 });
      const markReadSpy = jest.spyOn(notificationService, 'markAsRead').mockResolvedValue({ id: 10, isRead: true, readAt: '2026-11-18T11:00:00' });
      const markAllSpy = jest.spyOn(notificationService, 'markAllAsRead').mockResolvedValue({ updatedCount: 1 });

      const { unmount } = renderWithProviders(<NotificationsPage />);
      expect(await screen.findByText('Approve Laptop')).toBeInTheDocument();

      fireEvent.click(screen.getByLabelText(/Unread only/i));
      await waitFor(() =>
        expect(getNotifsSpy).toHaveBeenCalledWith(expect.objectContaining({ unreadOnly: true }))
      );

      fireEvent.change(screen.getByLabelText(/Filter by event type/i), { target: { value: 'APPROVAL_REQUIRED' } });
      await waitFor(() =>
        expect(getNotifsSpy).toHaveBeenCalledWith(expect.objectContaining({ eventType: 'APPROVAL_REQUIRED' }))
      );

      fireEvent.click(screen.getByRole('button', { name: /Mark All Read/i }));
      await waitFor(() => expect(markAllSpy).toHaveBeenCalledTimes(1));
      expect(await screen.findByText(/All unread notifications have been marked as read/i)).toBeInTheDocument();
      unmount();

      // NotificationBell dropdown interaction
      renderWithProviders(<NotificationBell />);
      const bellBtn = await screen.findByRole('button', { name: /Notifications, 1 unread/i });
      fireEvent.click(bellBtn);
      expect(await screen.findByText('Approve Laptop')).toBeInTheDocument();
      fireEvent.click(screen.getByText('Approve Laptop'));
      await waitFor(() => expect(markReadSpy).toHaveBeenCalledWith(10));
    });

    test('TicketComments hides Internal Note toggle from employees and submits isInternal=true for specialists', async () => {
      jest.spyOn(ticketService, 'getComments').mockResolvedValue([
        { id: 1, authorUsername: 'john_doe', commentText: 'Public update', isInternal: false, createdAt: '2026-11-18T10:00:00' },
        { id: 2, authorUsername: 'it_support', commentText: 'Internal root cause', isInternal: true, createdAt: '2026-11-18T10:05:00' }
      ]);
      const addSpy = jest.spyOn(ticketService, 'addComment').mockResolvedValue({ id: 3 });

      const { unmount } = renderWithProviders(<TicketComments ticketId={55} />);
      expect(await screen.findByText('Public update')).toBeInTheDocument();
      expect(screen.queryByText(/Internal Specialist Note/i)).not.toBeInTheDocument();
      unmount();

      renderWithProviders(<TicketComments ticketId={55} />, { auth: { ...baseAuth, isResolver: true } });
      expect(await screen.findByText(/Internal Specialist Note/i)).toBeInTheDocument();
      fireEvent.change(screen.getByLabelText(/Add comment/i), { target: { value: 'Checked switch port logs' } });
      fireEvent.click(screen.getByRole('checkbox'));
      fireEvent.click(screen.getByRole('button', { name: /Post Comment/i }));
      await waitFor(() => expect(addSpy).toHaveBeenCalledWith(55, 'Checked switch port logs', true));
    });

    test('QA-DEF-007 regression: LoginModal keeps role=alert visible after submitting resets to false on failed login', async () => {
      const failingLogin = jest.fn().mockRejectedValue({
        title: 'Authentication Failed',
        detail: 'Invalid credentials provided.'
      });

      renderWithProviders(<LoginModal isOpen onClose={jest.fn()} />, {
        auth: { ...baseAuth, isAuthenticated: false, currentUser: null, login: failingLogin }
      });

      fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'john_doe' } });
      fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'wrongpass' } });
      fireEvent.click(screen.getByRole('button', { name: /^Sign In$/i }));

      const alertEl = await screen.findByRole('alert');
      expect(alertEl).toHaveTextContent(/Invalid credentials provided/i);
      // Confirm button re-enabled AND alert still remains visible (not wiped by useEffect)
      await waitFor(() => expect(screen.getByRole('button', { name: /^Sign In$/i })).not.toBeDisabled());
      expect(screen.getByRole('alert')).toHaveTextContent(/Invalid credentials provided/i);
    });
  });
});
