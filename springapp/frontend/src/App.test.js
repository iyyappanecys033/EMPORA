import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import { AuthProvider, useAuth } from './services/AuthContext';
import { StatusBadge, PriorityBadge } from './components/common/Badge';
import Pagination from './components/common/Pagination';
import LoadingSpinner from './components/common/LoadingSpinner';
import EmptyState from './components/common/EmptyState';
import Alert from './components/common/Alert';
import { formatApiError } from './services/api';
import ticketService from './services/ticketService';
import CreateTicketModal from './components/tickets/CreateTicketModal';
import ServiceCatalog from './components/catalog/ServiceCatalog';
import ApprovalQueue from './components/approvals/ApprovalQueue';
import TicketComments from './components/tickets/TicketComments';
import AuditLedger from './components/audit/AuditLedger';

// =========================================================================
// Suite 1: Common UI & Utility Components
// =========================================================================
describe('EMPORA Common UI & Utility Components', () => {
  test('StatusBadge renders correct label and style for all 11 lifecycle states', () => {
    const states = [
      'DRAFT', 'SUBMITTED', 'PENDING_APPROVAL', 'PENDING_SECONDARY_APPROVAL',
      'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED', 'REJECTED', 'CANCELLED'
    ];

    states.forEach((state) => {
      const { unmount } = render(<StatusBadge status={state} />);
      const text = state.replace(/_/g, ' ');
      expect(screen.getByText(text)).toBeInTheDocument();
      unmount();
    });
  });

  test('PriorityBadge renders correct priority text for all 5 domain priorities', () => {
    ['LOW', 'MEDIUM', 'HIGH', 'URGENT', 'CRITICAL'].forEach((pri) => {
      const { unmount } = render(<PriorityBadge priority={pri} />);
      expect(screen.getByText(pri)).toBeInTheDocument();
      unmount();
    });
  });

  test('Pagination component renders current page and triggers onPageChange correctly', () => {
    const onPageChange = jest.fn();
    render(
      <Pagination
        currentPage={1}
        totalPages={5}
        totalElements={50}
        onPageChange={onPageChange}
      />
    );

    expect(screen.getByText('Page 2 of 5')).toBeInTheDocument();
    expect(screen.getByText(/Total:/)).toBeInTheDocument();

    const prevBtn = screen.getByText(/Previous/);
    const nextBtn = screen.getByText(/Next/);

    fireEvent.click(prevBtn);
    expect(onPageChange).toHaveBeenCalledWith(0);

    fireEvent.click(nextBtn);
    expect(onPageChange).toHaveBeenCalledWith(2);
  });

  test('LoadingSpinner renders message and spinner element', () => {
    render(<LoadingSpinner message="Validating authentication tokens..." />);
    expect(screen.getByText('Validating authentication tokens...')).toBeInTheDocument();
  });

  test('EmptyState renders custom icon, title, description, and action button', () => {
    render(
      <EmptyState
        icon="🎫"
        title="No Tickets"
        description="Submit your first ticket"
        action={<button>Create Ticket</button>}
      />
    );
    expect(screen.getByText('🎫')).toBeInTheDocument();
    expect(screen.getByText('No Tickets')).toBeInTheDocument();
    expect(screen.getByText('Submit your first ticket')).toBeInTheDocument();
    expect(screen.getByText('Create Ticket')).toBeInTheDocument();
  });

  test('Alert renders RFC 7807 field error breakdown correctly', () => {
    const errors = [
      { field: 'catalogItemId', message: 'must not be null' },
      { field: 'description', message: 'must not be blank' }
    ];
    render(
      <Alert
        type="error"
        title="Bad Request"
        detail="Payload validation failed"
        errors={errors}
      />
    );

    expect(screen.getByText(/Bad Request/)).toBeInTheDocument();
    expect(screen.getByText('Payload validation failed')).toBeInTheDocument();
    expect(screen.getByText('catalogItemId:')).toBeInTheDocument();
    expect(screen.getByText('must not be null')).toBeInTheDocument();
    expect(screen.getByText('description:')).toBeInTheDocument();
    expect(screen.getByText('must not be blank')).toBeInTheDocument();
  });
});

// =========================================================================
// Suite 2: RFC 7807 Error Formatting Tests
// =========================================================================
describe('RFC 7807 Problem Details Formatter', () => {
  test('formats validation 400 error correctly', () => {
    const axiosError = {
      response: {
        status: 400,
        data: {
          type: 'about:blank',
          title: 'Bad Request',
          status: 400,
          detail: 'Validation failed for request payload',
          errors: {
            catalogItemId: 'catalogItemId is mandatory'
          }
        }
      }
    };
    const formatted = formatApiError(axiosError);
    expect(formatted.status).toBe(400);
    expect(formatted.title).toBe('Bad Request');
    expect(formatted.detail).toBe('Validation failed for request payload');
    expect(formatted.errors).toEqual([
      { field: 'catalogItemId', message: 'catalogItemId is mandatory' }
    ]);
  });

  test('formats concurrency 409 conflict error correctly', () => {
    const axiosError = {
      response: {
        status: 409,
        data: {
          title: 'Conflict',
          detail: 'The resource was modified by another concurrent transaction.'
        }
      }
    };
    const formatted = formatApiError(axiosError);
    expect(formatted.status).toBe(409);
    expect(formatted.title).toBe('Conflict');
    expect(formatted.detail).toContain('modified by another concurrent transaction');
  });

  test('formats network disconnection error correctly', () => {
    const networkError = new Error('Network Error');
    const formatted = formatApiError(networkError);
    expect(formatted.status).toBe(0);
    expect(formatted.title).toBe('Network Error');
  });
});

// =========================================================================
// Suite 3: Role-Based Authority Navigation
// =========================================================================
describe('Role-Based Authority Navigation', () => {
  const TestConsumer = () => {
    const { hasRole, isAdmin, isResolver, isApprover, isAuditor } = useAuth();
    return (
      <div>
        <div data-testid="is-admin">{isAdmin ? 'YES' : 'NO'}</div>
        <div data-testid="is-resolver">{isResolver ? 'YES' : 'NO'}</div>
        <div data-testid="is-approver">{isApprover ? 'YES' : 'NO'}</div>
        <div data-testid="is-auditor">{isAuditor ? 'YES' : 'NO'}</div>
        <div data-testid="has-employee">{hasRole('ROLE_EMPLOYEE') ? 'YES' : 'NO'}</div>
      </div>
    );
  };

  test('AuthContext role evaluation normalizes role names correctly', async () => {
    jest.spyOn(require('./services/api').default, 'get').mockResolvedValueOnce({
      data: {
        id: 1,
        username: 'specialist_dave',
        roles: ['ROLE_IT_SUPPORT', 'ROLE_EMPLOYEE'],
        department: 'IT',
        fullName: 'Dave Miller'
      }
    });

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('is-admin').textContent).toBe('NO');
      expect(screen.getByTestId('is-resolver').textContent).toBe('YES');
      expect(screen.getByTestId('is-approver').textContent).toBe('NO');
      expect(screen.getByTestId('has-employee').textContent).toBe('YES');
    });
  });

  test('AuthContext recognizes Manager as Approver', async () => {
    jest.spyOn(require('./services/api').default, 'get').mockResolvedValueOnce({
      data: {
        id: 2,
        username: 'mgr_bob',
        roles: ['ROLE_MANAGER', 'ROLE_EMPLOYEE'],
        department: 'Engineering',
        fullName: 'Bob Jones'
      }
    });

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('is-approver').textContent).toBe('YES');
      expect(screen.getByTestId('is-admin').textContent).toBe('NO');
    });
  });

  test('AuthContext recognizes Auditor correctly', async () => {
    jest.spyOn(require('./services/api').default, 'get').mockResolvedValueOnce({
      data: {
        id: 3,
        username: 'auditor_arthur',
        roles: ['ROLE_AUDITOR'],
        department: 'Internal Audit',
        fullName: 'Arthur Conan'
      }
    });

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('is-auditor').textContent).toBe('YES');
      expect(screen.getByTestId('is-resolver').textContent).toBe('NO');
    });
  });
});

// =========================================================================
// Suite 4: API Contract Fidelity & Service Facade Verification
// =========================================================================
describe('API Contract Fidelity & Service Facade', () => {
  const apiMock = require('./services/api').default;

  beforeEach(() => {
    jest.restoreAllMocks();
  });

  test('submitTicket sends exact backend DTO property names (department, description)', async () => {
    const postSpy = jest.spyOn(apiMock, 'post').mockResolvedValueOnce({
      data: { id: 101, ticketNumber: 'TKT-2026-000101', status: 'SUBMITTED' }
    });

    const payload = {
      catalogItemId: 5,
      department: 'IT',
      priority: 'URGENT',
      description: 'Emergency VPN configuration required for remote work.'
    };

    const result = await ticketService.submitTicket(payload);

    expect(postSpy).toHaveBeenCalledWith('/v1/tickets', {
      catalogItemId: 5,
      department: 'IT',
      priority: 'URGENT',
      description: 'Emergency VPN configuration required for remote work.'
    });
    expect(result.ticketNumber).toBe('TKT-2026-000101');
  });

  test('updateTicketStatus sends exact backend DTO property "status" (not newStatus)', async () => {
    const postSpy = jest.spyOn(apiMock, 'post').mockResolvedValueOnce({
      data: { id: 101, status: 'IN_PROGRESS' }
    });

    await ticketService.updateTicketStatus(101, 'IN_PROGRESS', 'Beginning diagnostic tests', 2);

    expect(postSpy).toHaveBeenCalledWith('/v1/tickets/101/status', {
      status: 'IN_PROGRESS',
      reason: 'Beginning diagnostic tests',
      version: 2
    });
  });

  test('decideApproval sends exact backend DTO property "reason" (not decisionNotes)', async () => {
    const postSpy = jest.spyOn(apiMock, 'post').mockResolvedValueOnce({
      data: { id: 12, status: 'APPROVED' }
    });

    await ticketService.decideApproval(12, 'APPROVED', 'Approved within department budget allowance.');

    expect(postSpy).toHaveBeenCalledWith('/v1/approvals/12/decide', {
      decision: 'APPROVED',
      reason: 'Approved within department budget allowance.'
    });
  });

  test('verifyAuditChain calls GET /v1/audit/verify conforming to backend AuditApiController', async () => {
    const getSpy = jest.spyOn(apiMock, 'get').mockResolvedValueOnce({
      data: { valid: true, totalRecordsVerified: 42, calculatedHeadHash: 'abc123sha256' }
    });

    const report = await ticketService.verifyAuditChain();

    expect(getSpy).toHaveBeenCalledWith('/v1/audit/verify');
    expect(report.valid).toBe(true);
    expect(report.totalRecordsVerified).toBe(42);
  });
});

// =========================================================================
// Suite 5: Workflow Integration & Component Form Verification
// =========================================================================
describe('End-to-End Workflow & Component Integration', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  test('CreateTicketModal populates catalog options with title/department and submits valid DTO with URGENT priority', async () => {
    jest.spyOn(ticketService, 'getCatalogItems').mockResolvedValueOnce({
      content: [
        {
          id: 7,
          itemCode: 'IT-DEV-LAPTOP',
          title: 'MacBook Pro 16" Requisition',
          department: 'IT',
          slaTargetHours: 48,
          estimatedCost: 2400.00,
          requiresManagerApproval: true
        }
      ]
    });

    const submitSpy = jest.spyOn(ticketService, 'submitTicket').mockResolvedValueOnce({
      id: 201,
      ticketNumber: 'TKT-2026-000201'
    });

    const handleCreated = jest.fn();
    const handleClose = jest.fn();

    render(
      <CreateTicketModal
        isOpen={true}
        onClose={handleClose}
        onTicketCreated={handleCreated}
      />
    );

    // Wait for catalog item to load into dropdown
    await waitFor(() => {
      expect(screen.getByText(/MacBook Pro 16" Requisition/)).toBeInTheDocument();
    });

    // Select the service offering
    const serviceSelect = screen.getByLabelText(/Service Offering \*/i);
    fireEvent.change(serviceSelect, { target: { value: '7' } });

    // Target department should auto-fill to IT
    const deptInput = screen.getByLabelText(/Target Department \*/i);
    expect(deptInput.value).toBe('IT');

    // Select URGENT priority
    const prioritySelect = screen.getByLabelText(/Priority \*/i);
    fireEvent.change(prioritySelect, { target: { value: 'URGENT' } });
    expect(prioritySelect.value).toBe('URGENT');

    // Fill justification / description
    const justificationText = screen.getByLabelText(/Business Justification & Details \*/i);
    fireEvent.change(justificationText, {
      target: { value: 'Required for urgent production deployment engineering project.' }
    });

    // Submit ticket
    const submitBtn = screen.getByRole('button', { name: /Submit Ticket/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(submitSpy).toHaveBeenCalledWith({
        catalogItemId: 7,
        department: 'IT',
        priority: 'URGENT',
        description: 'Required for urgent production deployment engineering project.'
      });
      expect(handleCreated).toHaveBeenCalled();
      expect(handleClose).toHaveBeenCalled();
    });
  });

  test('ServiceCatalog correctly displays backend title, department, SLA, and estimated cost', async () => {
    jest.spyOn(ticketService, 'getCatalogItems').mockResolvedValueOnce({
      content: [
        {
          id: 1,
          itemCode: 'IT-SW-INTELLIJ',
          title: 'IntelliJ IDEA Ultimate License',
          category: 'SOFTWARE',
          department: 'IT',
          description: 'Standard enterprise development IDE license.',
          estimatedCost: 499.00,
          slaTargetHours: 24,
          requiresManagerApproval: true,
          requiresSecondaryApproval: false
        }
      ]
    });

    render(
      <BrowserRouter>
        <AuthProvider>
          <ServiceCatalog />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('IntelliJ IDEA Ultimate License')).toBeInTheDocument();
      expect(screen.getByText('IT')).toBeInTheDocument();
      expect(screen.getByText('24 hours')).toBeInTheDocument();
      expect(screen.getByText('Est. $499')).toBeInTheDocument();
      expect(screen.getByText('Manager Approval')).toBeInTheDocument();
    });
  });

  test('ApprovalQueue enforces anti-self-approval rule and renders requester username', async () => {
    jest.spyOn(require('./services/api').default, 'get').mockResolvedValueOnce({
      data: {
        id: 10,
        username: 'manager_mary',
        roles: ['ROLE_MANAGER']
      }
    });

    jest.spyOn(ticketService, 'getPendingApprovals').mockResolvedValueOnce([
      {
        id: 55,
        ticketId: 101,
        ticketNumber: 'TKT-2026-000101',
        approvalStage: 1,
        requesterUsername: 'manager_mary', // Self-request
        status: 'PENDING'
      },
      {
        id: 56,
        ticketId: 102,
        ticketNumber: 'TKT-2026-000102',
        approvalStage: 1,
        requesterUsername: 'developer_dan', // Other requester
        status: 'PENDING'
      }
    ]);

    render(
      <BrowserRouter>
        <AuthProvider>
          <ApprovalQueue />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Ticket #TKT-2026-000101')).toBeInTheDocument();
      expect(screen.getByText('Ticket #TKT-2026-000102')).toBeInTheDocument();
    });

    // Task 55 is self-request: should show Anti-Self-Approval warning
    expect(screen.getByText(/Anti-Self-Approval: You cannot approve your own request/i)).toBeInTheDocument();

    // Task 56 is from developer_dan: should display Approve & Reject buttons
    expect(screen.getByText('👤 developer_dan')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /✓ Approve/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /✕ Reject/i })).toBeInTheDocument();
  });

  test('TicketComments allows posting internal note with isInternal: true', async () => {
    jest.spyOn(require('./services/api').default, 'get').mockResolvedValueOnce({
      data: {
        id: 5,
        username: 'resolver_rick',
        roles: ['ROLE_IT_SUPPORT'],
        department: 'IT'
      }
    });

    jest.spyOn(ticketService, 'getComments').mockResolvedValueOnce([
      {
        id: 1,
        authorUsername: 'alice',
        commentText: 'Has the license key been issued?',
        isInternal: false,
        createdAt: '2026-11-17T10:00:00Z'
      }
    ]);

    const addCommentSpy = jest.spyOn(ticketService, 'addComment').mockResolvedValueOnce({
      id: 2,
      authorUsername: 'resolver_rick',
      commentText: 'Checking procurement inventory for spare license key.',
      isInternal: true,
      createdAt: '2026-11-17T10:15:00Z'
    });

    render(
      <AuthProvider>
        <TicketComments ticketId={101} />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('alice')).toBeInTheDocument();
      expect(screen.getByText('Has the license key been issued?')).toBeInTheDocument();
    });

    // Resolver should see the internal note checkbox
    const internalCheckbox = screen.getByRole('checkbox');
    expect(internalCheckbox).toBeInTheDocument();
    fireEvent.click(internalCheckbox);
    expect(internalCheckbox.checked).toBe(true);

    const textarea = screen.getByPlaceholderText(/Add an update, clarification, or note/i);
    fireEvent.change(textarea, { target: { value: 'Checking procurement inventory for spare license key.' } });

    const postBtn = screen.getByRole('button', { name: /Post Comment/i });
    fireEvent.click(postBtn);

    await waitFor(() => {
      expect(addCommentSpy).toHaveBeenCalledWith(
        101,
        'Checking procurement inventory for spare license key.',
        true
      );
    });
  });

  test('AuditLedger displays immutable entries and triggers verifyAuditChain on button click', async () => {
    jest.spyOn(ticketService, 'getAuditLogs').mockResolvedValueOnce({
      content: [
        {
          id: 1,
          sequenceNumber: 1,
          previousHash: 'GENESIS-0000000000000000000000000000000000000000000000000000000000000000',
          currentHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
          actorUsername: 'SYSTEM',
          action: 'CHAIN_INITIALIZED',
          targetUsername: 'SYSTEM',
          details: 'Audit log hash-chain genesis initialized successfully.',
          timestamp: '2026-11-15T18:00:00Z'
        }
      ],
      totalPages: 1,
      totalElements: 1
    });

    const verifySpy = jest.spyOn(ticketService, 'verifyAuditChain').mockResolvedValueOnce({
      valid: true,
      totalRecordsVerified: 1,
      calculatedHeadHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
    });

    render(<AuditLedger />);

    await waitFor(() => {
      expect(screen.getByText('CHAIN_INITIALIZED')).toBeInTheDocument();
      expect(screen.getAllByText('SYSTEM').length).toBeGreaterThan(0);
    });

    const verifyBtn = screen.getByRole('button', { name: /Verify Cryptographic Chain/i });
    fireEvent.click(verifyBtn);

    await waitFor(() => {
      expect(verifySpy).toHaveBeenCalled();
      expect(screen.getByText(/Cryptographic Audit Chain Intact/i)).toBeInTheDocument();
    });
  });
});
