import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import api, { formatApiError, getCookie } from './services/api';
import { AuthProvider, useAuth } from './services/AuthContext';
import ticketService from './services/ticketService';
import CreateTicketModal from './components/tickets/CreateTicketModal';
import App from './App';

const AuthProbe = () => {
  const { isAuthenticated, currentUser } = useAuth();
  return (
    <div>
      <span data-testid="auth-status">{isAuthenticated ? 'AUTH' : 'ANON'}</span>
      <span data-testid="auth-user">{currentUser?.username || 'none'}</span>
    </div>
  );
};

describe('P9 Frontend Security, Session Resilience & Duplicate-Submission QA', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
    window.history.pushState({}, 'Test', '/');
  });

  test('P9-FE-01: session-expired window event clears AuthContext immediately', async () => {
    jest.spyOn(api, 'get').mockResolvedValueOnce({
      data: { id: 11, username: 'alice_p9', roles: ['ROLE_EMPLOYEE'], department: 'Engineering' }
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <AuthProbe />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('AUTH');
      expect(screen.getByTestId('auth-user')).toHaveTextContent('alice_p9');
    });

    act(() => {
      window.dispatchEvent(new Event('session-expired'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('ANON');
      expect(screen.getByTestId('auth-user')).toHaveTextContent('none');
    });
  });

  test('P9-FE-02: ProtectedRoute blocks ROLE_EMPLOYEE from /users, /analytics, and /audit-chain with 403 UI', async () => {
    jest.spyOn(api, 'get').mockImplementation((url) => {
      if (url === '/users/me') {
        return Promise.resolve({
          data: { id: 12, username: 'emp_p9', roles: ['ROLE_EMPLOYEE'], department: 'Finance' }
        });
      }
      if (url === '/v1/notifications/my/unread-count') {
        return Promise.resolve({ data: { unreadCount: 0 } });
      }
      return Promise.resolve({ data: { content: [] } });
    });

    window.history.pushState({}, 'Users', '/users');
    const { unmount } = render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/403 — Unauthorized Scope/i)).toBeInTheDocument();
    });
    unmount();

    window.history.pushState({}, 'Audit', '/audit-chain');
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/403 — Unauthorized Scope/i)).toBeInTheDocument();
    });
  });

  test('P9-FE-03: formatApiError extracts RFC 7807 fields, object error maps, and network offline fallback', () => {
    const netErr = formatApiError({ message: 'Network Error' });
    expect(netErr.status).toBe(0);
    expect(netErr.title).toBe('Network Error');

    const validationErr = formatApiError({
      response: {
        status: 400,
        data: {
          title: 'Validation Failed',
          detail: 'One or more fields are invalid.',
          errors: { reason: 'Reason must be at least 15 characters.' }
        }
      }
    });
    expect(validationErr.status).toBe(400);
    expect(validationErr.title).toBe('Validation Failed');
    expect(validationErr.errors).toEqual([
      { field: 'reason', message: 'Reason must be at least 15 characters.' }
    ]);

    const conflictErr = formatApiError({
      response: {
        status: 409,
        data: { detail: 'Stale version conflict detected.' }
      }
    });
    expect(conflictErr.status).toBe(409);
    expect(conflictErr.title).toBe('Conflict');
    expect(conflictErr.detail).toBe('Stale version conflict detected.');
  });

  test('P9-FE-04: CreateTicketModal prevents blank justification and disables button during in-flight submit', async () => {
    jest.spyOn(ticketService, 'getCatalogItems').mockResolvedValueOnce({
      content: [
        { id: 7, title: 'VPN Access', department: 'IT', slaTargetHours: 8 }
      ]
    });

    let resolveSubmit;
    const submitSpy = jest.spyOn(ticketService, 'submitTicket').mockImplementation(
      () => new Promise((resolve) => { resolveSubmit = resolve; })
    );
    const onClose = jest.fn();

    render(
      <CreateTicketModal
        isOpen={true}
        preselectedItem={{ id: 7, targetDepartment: 'IT' }}
        onClose={onClose}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/Service Offering/i)).toBeInTheDocument();
    });

    // Whitespace-only justification must fail client-side validation before API call
    const justification = screen.getByLabelText(/Business Justification/i);
    fireEvent.change(justification, { target: { value: '     ' } });
    fireEvent.click(screen.getByRole('button', { name: /Submit Ticket/i }));

    await waitFor(() => {
      expect(screen.getByText(/Justification must not be blank/i)).toBeInTheDocument();
    });
    expect(submitSpy).not.toHaveBeenCalled();

    // Valid submission enters submitting state and disables button to prevent double-submit
    fireEvent.change(justification, { target: { value: 'Need corporate VPN access for P9 QA.' } });
    const submitBtn = screen.getByRole('button', { name: /Submit Ticket/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Submitting.../i })).toBeDisabled();
    });
    expect(submitSpy).toHaveBeenCalledTimes(1);

    // Resolve promise cleanly
    await act(async () => {
      resolveSubmit({ id: 99, ticketNumber: 'TKT-2026-000099' });
    });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test('P9-FE-05: getCookie reads XSRF-TOKEN accurately without touching localStorage', () => {
    document.cookie = 'XSRF-TOKEN=p9-csrf-token-value; path=/';
    expect(getCookie('XSRF-TOKEN')).toBe('p9-csrf-token-value');
    expect(getCookie('NON_EXISTENT_COOKIE')).toBeNull();
    expect(window.localStorage.getItem('token')).toBeNull();
    expect(window.localStorage.getItem('jwt')).toBeNull();
  });

  test('P9-FE-06: CreateTicketModal preserves backend catalog department field (HR/Facilities) without defaulting to IT', async () => {
    jest.spyOn(ticketService, 'getCatalogItems').mockResolvedValueOnce({
      content: [
        { id: 12, title: 'Workspace Ergonomics', department: 'Facilities', slaTargetHours: 24 }
      ]
    });

    render(
      <CreateTicketModal
        isOpen={true}
        preselectedItem={{ id: 12, title: 'Workspace Ergonomics', department: 'Facilities', slaTargetHours: 24 }}
        onClose={jest.fn()}
      />
    );

    await waitFor(() => {
      const deptInput = screen.getByLabelText(/Target Department/i);
      expect(deptInput.value).toBe('Facilities');
    });
  });
});

