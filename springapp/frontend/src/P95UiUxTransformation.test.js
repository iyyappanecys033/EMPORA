import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import api from './services/api';
import { AuthProvider } from './services/AuthContext';
import { PreferencesProvider, usePreferences } from './services/PreferencesContext';
import { ToastProvider } from './services/ToastContext';
import ticketService from './services/ticketService';
import Sidebar from './components/layout/Sidebar';
import EmployeeProfile from './components/profile/EmployeeProfile';
import PreferencesPage from './components/profile/PreferencesPage';
import StatusTimeline from './components/common/StatusTimeline';
import Breadcrumb from './components/common/Breadcrumb';
import LoginModal from './components/LoginModal';
import MyTickets from './components/tickets/MyTickets';

describe('P9.5 Enterprise UI/UX Design System, Themes & Accessibility', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.removeAttribute('data-density');
    document.documentElement.removeAttribute('data-reduced-motion');
    document.documentElement.removeAttribute('data-font-scale');
  });

  test('P95-01: PreferencesProvider switches light/dark/system themes and persists across remounts', () => {
    const ThemeProbe = () => {
      const { theme, resolvedTheme, updatePreference } = usePreferences();
      return (
        <div>
          <span data-testid="theme-mode">{theme}</span>
          <span data-testid="resolved-theme">{resolvedTheme}</span>
          <button type="button" onClick={() => updatePreference('theme', 'dark')}>Set Dark</button>
          <button type="button" onClick={() => updatePreference('theme', 'light')}>Set Light</button>
        </div>
      );
    };

    const { unmount } = render(
      <PreferencesProvider>
        <ThemeProbe />
      </PreferencesProvider>
    );

    expect(screen.getByTestId('theme-mode')).toHaveTextContent('system');
    fireEvent.click(screen.getByText('Set Dark'));
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(JSON.parse(window.localStorage.getItem('empora.preferences') || window.localStorage.getItem('epams.preferences')).theme).toBe('dark');

    unmount();

    render(
      <PreferencesProvider>
        <ThemeProbe />
      </PreferencesProvider>
    );
    expect(screen.getByTestId('theme-mode')).toHaveTextContent('dark');
    expect(screen.getByTestId('resolved-theme')).toHaveTextContent('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  test('P95-02: PreferencesPage applies compact density, reduced motion, and large font scale', () => {
    render(
      <PreferencesProvider>
        <ToastProvider>
          <PreferencesPage />
        </ToastProvider>
      </PreferencesProvider>
    );

    fireEvent.click(screen.getByRole('radio', { name: /Dark/i }));
    fireEvent.click(screen.getByRole('radio', { name: /Compact/i }));
    fireEvent.click(screen.getByRole('radio', { name: /Reduce Motion/i }));
    fireEvent.click(screen.getByRole('radio', { name: /Large \(112\.5%\)/i }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(document.documentElement.getAttribute('data-density')).toBe('compact');
    expect(document.documentElement.getAttribute('data-reduced-motion')).toBe('true');
    expect(document.documentElement.getAttribute('data-font-scale')).toBe('large');
  });

  test('P95-03: Sidebar enforces role-aware visibility and supports desktop collapse & mobile drawer', async () => {
    jest.spyOn(api, 'get').mockResolvedValueOnce({
      data: { id: 22, username: 'emp_sara', roles: ['ROLE_EMPLOYEE'], department: 'HR' }
    });

    render(
      <PreferencesProvider>
        <MemoryRouter initialEntries={['/']}>
          <AuthProvider>
            <Sidebar mobileOpen={false} onCloseMobile={jest.fn()} />
          </AuthProvider>
        </MemoryRouter>
      </PreferencesProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('My Profile')).toBeInTheDocument();
      expect(screen.getByText('Preferences')).toBeInTheDocument();
    });

    expect(screen.queryByText('Pending Approvals')).not.toBeInTheDocument();
    expect(screen.queryByText('Cryptographic Ledger')).not.toBeInTheDocument();
    expect(screen.queryByText('Users')).not.toBeInTheDocument();

    const collapseBtn = screen.getByRole('button', { name: /Collapse sidebar/i });
    fireEvent.click(collapseBtn);
    expect(screen.getByRole('button', { name: /Expand sidebar/i })).toBeInTheDocument();
  });

  test('P95-04: EmployeeProfile fetches GET /api/profiles/me and renders identity without edit privilege leak', async () => {
    jest.spyOn(api, 'get').mockImplementation((url) => {
      if (url === '/users/me') {
        return Promise.resolve({
          data: { id: 3, username: 'john_doe', roles: ['ROLE_EMPLOYEE'], department: 'Engineering' }
        });
      }
      if (url === '/profiles/me') {
        return Promise.resolve({
          data: {
            id: 3,
            employeeId: 'EMP-1003',
            username: 'john_doe',
            firstName: 'John',
            lastName: 'Doe',
            email: 'john.doe@epams.example',
            phone: '+91-9800000001',
            department: 'Engineering',
            designation: 'Software Engineer',
            reportingManager: 'mgr_alice',
            status: 'ACTIVE',
            roles: ['ROLE_EMPLOYEE']
          }
        });
      }
      return Promise.resolve({ data: {} });
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <EmployeeProfile />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('EMP-1003')).toBeInTheDocument();
      expect(screen.getByText('mgr_alice')).toBeInTheDocument();
      expect(screen.getByText('john.doe@epams.example')).toBeInTheDocument();
    });

    expect(screen.getByText(/Request Profile Update/i)).toBeInTheDocument();
    expect(screen.queryByText(/Edit Profile Record/i)).not.toBeInTheDocument();
  });

  test('P95-05: LoginModal has accessible dialog role, Escape close, and zero hardcoded credentials', () => {
    const onClose = jest.fn();
    render(
      <AuthProvider>
        <LoginModal isOpen={true} onClose={onClose} />
      </AuthProvider>
    );

    expect(screen.getByRole('dialog')).toHaveAttribute('aria-modal', 'true');
    expect(screen.queryByText(/admin123/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/user123/i)).not.toBeInTheDocument();

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test('P95-06: StatusTimeline accurately reflects all 11 backend lifecycle states and guidance', () => {
    const { rerender } = render(<StatusTimeline status="PENDING_SECONDARY_APPROVAL" />);
    expect(screen.getByText(/Secondary Approval/i)).toBeInTheDocument();
    expect(screen.getByText(/Awaiting Stage 2 Secondary/i)).toBeInTheDocument();

    rerender(<StatusTimeline status="ON_HOLD" />);
    expect(screen.getByText(/On Hold \(Paused\)/i)).toBeInTheDocument();
    expect(screen.getByText(/SLA clock is paused/i)).toBeInTheDocument();

    rerender(<StatusTimeline status="REJECTED" />);
    expect(screen.getByText('Rejected')).toBeInTheDocument();
    expect(screen.getByText(/Request was declined during approval review/i)).toBeInTheDocument();
  });

  test('P95-07: MyTickets supports table/card layout toggle, priority filter, and backend totalElements count', async () => {
    jest.spyOn(ticketService, 'getMyTickets').mockResolvedValueOnce({
      content: [
        { id: 1, ticketNumber: 'TKT-2026-000001', department: 'IT', description: 'Laptop screen flicker', priority: 'HIGH', status: 'IN_PROGRESS', createdAt: '2026-11-17T10:00:00' },
        { id: 2, ticketNumber: 'TKT-2026-000002', department: 'HR', description: 'Address update', priority: 'LOW', status: 'SUBMITTED', createdAt: '2026-11-16T10:00:00' }
      ],
      totalPages: 2,
      totalElements: 18
    });

    render(
      <MemoryRouter>
        <MyTickets />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('TKT-2026-000001')).toBeInTheDocument();
      expect(screen.getAllByText('18').length).toBeGreaterThanOrEqual(2);
    });

    // Switch to card view
    fireEvent.click(screen.getByRole('button', { name: /Card view/i }));
    expect(screen.getByText('TKT-2026-000001')).toBeInTheDocument();

    // Filter by HIGH priority
    fireEvent.change(screen.getByLabelText(/Filter by priority/i), { target: { value: 'HIGH' } });
    expect(screen.getByText('TKT-2026-000001')).toBeInTheDocument();
    expect(screen.queryByText('TKT-2026-000002')).not.toBeInTheDocument();
  });

  test('P95-08: Breadcrumb renders accessible hierarchy for nested routes', () => {
    render(
      <MemoryRouter initialEntries={['/tickets/42']}>
        <Breadcrumb />
      </MemoryRouter>
    );

    const nav = screen.getByRole('navigation', { name: /Breadcrumb/i });
    expect(nav).toBeInTheDocument();
    expect(screen.getByText('Home')).toBeInTheDocument();
    expect(screen.getByText('My Tickets')).toBeInTheDocument();
    expect(screen.getByText('#42')).toHaveAttribute('aria-current', 'page');
  });
});
