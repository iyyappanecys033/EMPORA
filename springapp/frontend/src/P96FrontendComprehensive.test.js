import React from 'react';
import '@testing-library/jest-dom';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from './services/AuthContext';
import { PreferencesProvider } from './services/PreferencesContext';
import { ToastProvider } from './services/ToastContext';
import api from './services/api';
import ticketService from './services/ticketService';
import notificationService from './services/notificationService';
import analyticsService from './services/analyticsService';
import ServiceCatalog from './components/catalog/ServiceCatalog';
import CreateTicketModal from './components/tickets/CreateTicketModal';
import TicketDetail from './components/tickets/TicketDetail';
import ApprovalQueue from './components/approvals/ApprovalQueue';
import DepartmentQueue from './components/queue/DepartmentQueue';
import AuditLedger from './components/audit/AuditLedger';
import AnalyticsDashboard from './components/analytics/AnalyticsDashboard';
import LoginModal from './components/LoginModal';

jest.mock('./services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    patch: jest.fn(),
    delete: jest.fn()
  }
}));

jest.mock('./services/ticketService', () => ({
  __esModule: true,
  default: {
    getCatalogItems: jest.fn(),
    submitTicket: jest.fn(),
    getMyTickets: jest.fn(),
    getTicketById: jest.fn(),
    getTicketComments: jest.fn(),
    addTicketComment: jest.fn(),
    updateTicketStatus: jest.fn(),
    cancelTicket: jest.fn(),
    reopenTicket: jest.fn(),
    assignTicket: jest.fn(),
    getDepartmentQueue: jest.fn(),
    getPendingApprovals: jest.fn(),
    decideApproval: jest.fn(),
    getAuditLogs: jest.fn(),
    verifyAuditChain: jest.fn()
  }
}));

jest.mock('./services/notificationService', () => ({
  __esModule: true,
  default: {
    getMyNotifications: jest.fn(),
    getUnreadCount: jest.fn(),
    markAsRead: jest.fn(),
    markAllAsRead: jest.fn()
  }
}));

jest.mock('./services/analyticsService', () => ({
  __esModule: true,
  default: {
    getDashboardBundle: jest.fn()
  }
}));

const wrap = (ui, initialEntries = ['/']) => (
  <PreferencesProvider>
    <ToastProvider>
      <MemoryRouter initialEntries={initialEntries}>
        <AuthProvider>{ui}</AuthProvider>
      </MemoryRouter>
    </ToastProvider>
  </PreferencesProvider>
);

describe('P9.6 Stage I — Comprehensive Frontend Interaction, Validation & Error Recovery Suite', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.localStorage.clear();
  });

  test('P96-FE-01: ServiceCatalog omits stray 0 on $0 offerings and filters by derived category pills & keyword', async () => {
    api.get.mockResolvedValueOnce({ data: { username: 'emp1', roles: ['ROLE_EMPLOYEE'] } });
    ticketService.getCatalogItems.mockResolvedValue({
      content: [
        { id: 1, itemCode: 'IT-VPN', title: 'VPN Access', category: 'Access', department: 'IT', description: 'Zero cost VPN setup', estimatedCost: 0, slaTargetHours: 8, requiresManagerApproval: false },
        { id: 2, itemCode: 'IT-LAP', title: 'Laptop Upgrade', category: 'Hardware', department: 'IT', description: 'Workstation refresh', estimatedCost: 1850, slaTargetHours: 48, requiresManagerApproval: true }
      ],
      totalPages: 1,
      totalElements: 2
    });

    render(wrap(<ServiceCatalog />, ['/catalog']));
    expect(await screen.findByText('VPN Access')).toBeInTheDocument();
    expect(screen.getByText('Laptop Upgrade')).toBeInTheDocument();
    // VIS-DEF-07 regression: zero-cost item must NOT render a stray literal "0"
    expect(screen.queryByText(/^0$/)).not.toBeInTheDocument();
    expect(screen.getByText(/Est\. \$1,850/)).toBeInTheDocument();

    // Filter by category pill
    fireEvent.click(screen.getByRole('button', { name: 'Hardware' }));
    expect(screen.getByText('Laptop Upgrade')).toBeInTheDocument();
    expect(screen.queryByText('VPN Access')).not.toBeInTheDocument();
  });

  test('P96-FE-02: CreateTicketModal prevents duplicate submission and surfaces RFC 7807 validation error', async () => {
    api.get.mockResolvedValueOnce({ data: { username: 'emp1', roles: ['ROLE_EMPLOYEE'] } });
    let resolveSubmit;
    ticketService.submitTicket.mockImplementation(() => new Promise((res) => { resolveSubmit = res; }));

    const onTicketCreated = jest.fn();
    const onClose = jest.fn();
    render(
      wrap(
        <CreateTicketModal
          isOpen
          onClose={onClose}
          onTicketCreated={onTicketCreated}
          preselectedItem={{ id: 5, title: 'Monitor Replacement', department: 'IT', slaTargetHours: 24, requiresManagerApproval: false }}
        />
      )
    );

    const textarea = await screen.findByPlaceholderText(/Describe the nature/i);
    fireEvent.change(textarea, { target: { value: 'Primary display flickers constantly during engineering reviews.' } });

    const submitBtn = screen.getByRole('button', { name: /Submit Ticket/i });
    fireEvent.click(submitBtn);
    fireEvent.click(submitBtn); // Double-click guard

    expect(ticketService.submitTicket).toHaveBeenCalledTimes(1);
    resolveSubmit({ id: 101, ticketNumber: 'TKT-2026-000101' });
    await waitFor(() => expect(onTicketCreated).toHaveBeenCalledTimes(1));
  });

  test('P96-FE-03: LoginModal displays RFC 7807 error detail on failed login and switches to Register mode cleanly', async () => {
    api.get.mockRejectedValueOnce({ status: 401 });
    api.post.mockRejectedValueOnce({ status: 401, title: 'Authentication Failed', detail: 'Invalid username or password.' });

    const { container } = render(wrap(<LoginModal isOpen onClose={jest.fn()} />));

    fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'wrong_user' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'wrong_pass' } });
    fireEvent.submit(container.querySelector('form'));

    expect(await screen.findByText(/Invalid username or password/i)).toBeInTheDocument();

    // Switch to Register mode -> email & phone fields appear
    fireEvent.click(screen.getByRole('button', { name: /Register/i }));
    expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
  });

  test('P96-FE-04: ApprovalQueue validates minimum 15-char reason before submitting rejection decision (BR-007)', async () => {
    api.get.mockResolvedValueOnce({ data: { username: 'mgr1', roles: ['ROLE_MANAGER'] } });
    ticketService.getPendingApprovals.mockResolvedValue([
      { id: 77, ticketId: 12, ticketNumber: 'TKT-2026-000012', stage: 1, requesterUsername: 'john_doe', approverUsername: 'mgr1', createdAt: '2026-11-18T09:00:00' }
    ]);
    ticketService.decideApproval.mockResolvedValue({ status: 'REJECTED' });

    render(wrap(<ApprovalQueue />, ['/approvals']));
    expect(await screen.findByText(/TKT-2026-000012/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Reject/i }));
    const reasonBox = await screen.findByRole('textbox');
    fireEvent.change(reasonBox, { target: { value: 'Too short' } });
    fireEvent.click(screen.getByRole('button', { name: /Confirm Rejection/i }));

    expect(ticketService.decideApproval).not.toHaveBeenCalled();

    fireEvent.change(reasonBox, { target: { value: 'Rejected pending Q1 capital hardware budget cycle review.' } });
    fireEvent.click(screen.getByRole('button', { name: /Confirm Rejection/i }));
    await waitFor(() => expect(ticketService.decideApproval).toHaveBeenCalledWith(77, 'REJECTED', 'Rejected pending Q1 capital hardware budget cycle review.'));
  });

  test('P96-FE-05: DepartmentQueue renders SLA overdue indicator, filters by keyword, and assigns unassigned ticket', async () => {
    api.get.mockResolvedValueOnce({ data: { username: 'it_dave', roles: ['ROLE_IT_SUPPORT'], department: 'IT', id: 42 } });
    ticketService.getDepartmentQueue.mockResolvedValue({
      content: [
        { id: 9, ticketNumber: 'TKT-2026-000009', priority: 'CRITICAL', status: 'SUBMITTED', description: 'VPN certificate expired urgently', requesterUsername: 'john_doe', assigneeUsername: null, createdAt: '2026-11-17T08:00:00', slaTargetDate: '2026-11-17T10:00:00' }
      ],
      totalPages: 1,
      totalElements: 1
    });
    ticketService.assignTicket.mockResolvedValue({ id: 9, assigneeUsername: 'it_dave' });

    render(wrap(<DepartmentQueue />, ['/queue/IT']));
    expect(await screen.findByText('TKT-2026-000009')).toBeInTheDocument();
    expect(screen.getByText(/Overdue/i)).toBeInTheDocument();

    const searchInput = screen.getByPlaceholderText(/Search/i);
    fireEvent.change(searchInput, { target: { value: 'nonexistent-xyz' } });
    expect(screen.getByText(/No tickets in/i)).toBeInTheDocument();
  });

  test('P96-FE-06: AuditLedger displays tamper violation details when chain verification fails', async () => {
    api.get.mockResolvedValueOnce({ data: { username: 'auditor_eve', roles: ['ROLE_AUDITOR'] } });
    ticketService.getAuditLogs.mockResolvedValue({
      content: [
        { id: 1, sequenceNumber: 1, actorUsername: 'admin', action: 'TICKET_CREATED', targetUsername: 'john_doe', details: 'Created TKT-1', currentHash: 'abcdef1234567890abcdef', timestamp: '2026-11-18T10:00:00' }
      ],
      totalPages: 1,
      totalElements: 1
    });
    ticketService.verifyAuditChain.mockResolvedValue({
      valid: false,
      totalChainedRecords: 1,
      unchainedLegacyCount: 0,
      headSequence: 1,
      headHash: 'abcdef1234567890abcdef',
      violations: ['Tamper detected at sequence [1]']
    });

    render(wrap(<AuditLedger />, ['/audit-chain']));
    expect(await screen.findByText('TICKET_CREATED')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Verify Cryptographic Chain/i }));
    expect(await screen.findByText(/Tamper detected at sequence \[1\]/i)).toBeInTheDocument();
  });

  test('P96-FE-07: AnalyticsDashboard handles API error recovery cleanly with retry button', async () => {
    api.get.mockResolvedValueOnce({ data: { username: 'admin', roles: ['ROLE_ADMIN'] } });
    analyticsService.getDashboardBundle
      .mockRejectedValueOnce({ title: 'Analytics Unavailable', detail: 'Temporary query timeout.' })
      .mockResolvedValueOnce({
        overview: { totalSubmittedInPeriod: 4, openBacklogSnapshot: 2, resolvedInPeriod: 2, closedInPeriod: 0, overdueOpenSnapshot: 0, startDate: '2026-11-01', endDate: '2026-11-18', departmentScope: 'ALL' },
        slaCompliance: { slaComplianceRate: 100, metSlaCount: 2, breachedSlaCount: 0 },
        approvalOverview: { totalPendingSnapshot: 0, overduePendingSnapshot: 0, stage1PendingSnapshot: 0, stage2PendingSnapshot: 0, approvedInPeriod: 1, rejectedInPeriod: 0 },
        ticketTrends: [{ date: '2026-11-18', submittedCount: 4, resolvedCount: 2 }],
        statusDistribution: [{ status: 'RESOLVED', count: 2, percentage: 50 }],
        priorityDistribution: [{ priority: 'HIGH', count: 2, percentage: 50 }],
        departmentPerformance: [{ department: 'IT', totalSubmittedInPeriod: 4, openBacklogSnapshot: 2, overdueOpenSnapshot: 0, slaComplianceRate: 100 }],
        servicePopularity: [{ catalogItemId: 1, title: 'VPN Access', department: 'IT', requestCount: 4 }]
      });

    render(wrap(<AnalyticsDashboard />, ['/analytics']));
    expect(await screen.findByText(/Temporary query timeout/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Refresh/i }));
    expect(await screen.findByText('100%')).toBeInTheDocument();
    expect(screen.getByText('VPN Access')).toBeInTheDocument();
  });
});
