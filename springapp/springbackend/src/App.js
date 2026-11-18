import React, { useState, Suspense, lazy } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './services/AuthContext';
import { PreferencesProvider } from './services/PreferencesContext';
import { ToastProvider } from './services/ToastContext';
import AppLayout from './components/layout/AppLayout';
import LoadingSpinner from './components/common/LoadingSpinner';
import EmployeeDashboard from './components/dashboard/EmployeeDashboard';
import ServiceCatalog from './components/catalog/ServiceCatalog';
import MyTickets from './components/tickets/MyTickets';
import TicketDetail from './components/tickets/TicketDetail';
import DepartmentQueue from './components/queue/DepartmentQueue';
import ApprovalQueue from './components/approvals/ApprovalQueue';
import NotificationsPage from './components/notifications/NotificationsPage';
import EmployeeProfile from './components/profile/EmployeeProfile';
import PreferencesPage from './components/profile/PreferencesPage';
import LoginModal from './components/LoginModal';

// Existing Phase 1 Administrative Components
import UserList from './components/UserList';
import UserForm from './components/UserForm';
import RoleList from './components/RoleList';
import RoleForm from './components/RoleForm';
import ProfileList from './components/ProfileList';
import ProfileForm from './components/ProfileForm';
import UserRoleMapping from './components/UserRoleMapping';
import UserRoleMappingForm from './components/UserRoleMappingForm';
import AuditLogs from './components/AuditLogs';
import Dashboard from './components/Dashboard';

import './App.css';

// Code-split heavy operational workspaces (P8 frontend bundle optimization)
const AuditLedger = lazy(() => import('./components/audit/AuditLedger'));
const AnalyticsDashboard = lazy(() => import('./components/analytics/AnalyticsDashboard'));

// Guard for protected routes
const ProtectedRoute = ({ children, requiredRole, condition = true }) => {
  const { isAuthenticated, loading, hasRole } = useAuth();

  if (loading) return null;
  if (!isAuthenticated) return <Navigate to="/" replace />;
  if (requiredRole && !hasRole(requiredRole)) {
    return (
      <div className="ep-card" style={{ textAlign: 'center', padding: '56px 24px', maxWidth: '540px', margin: '32px auto' }}>
        <h2 style={{ margin: '0 0 8px', color: 'var(--ep-text-primary)' }}>403 — Access Denied</h2>
        <p style={{ color: 'var(--ep-text-muted)', fontSize: '14px', margin: '0 0 20px' }}>
          You do not have the required authority (<code>{requiredRole}</code>) to access this view.
        </p>
        <Link to="/" className="ep-btn ep-btn--primary">Return to Dashboard</Link>
      </div>
    );
  }
  if (!condition) {
    return (
      <div className="ep-card" style={{ textAlign: 'center', padding: '56px 24px', maxWidth: '540px', margin: '32px auto' }}>
        <h2 style={{ margin: '0 0 8px', color: 'var(--ep-text-primary)' }}>403 — Unauthorized Scope</h2>
        <p style={{ color: 'var(--ep-text-muted)', fontSize: '14px', margin: '0 0 20px' }}>
          Your profile is not authorized to access this workspace.
        </p>
        <Link to="/" className="ep-btn ep-btn--primary">Return to Dashboard</Link>
      </div>
    );
  }
  return children;
};

function AppContent() {
  const [loginModalOpen, setLoginModalOpen] = useState(false);
  const { isApprover, isResolver, isAuditor, isAdmin, canViewAnalytics } = useAuth();

  return (
    <AppLayout onOpenLogin={() => setLoginModalOpen(true)}>
      <Suspense fallback={<LoadingSpinner message="Loading workspace..." />}>
        <Routes>
          {/* Core Service Management Routes */}
          <Route path="/" element={<EmployeeDashboard onOpenLogin={() => setLoginModalOpen(true)} />} />
          <Route path="/catalog" element={<ServiceCatalog />} />
          <Route
            path="/tickets"
            element={
              <ProtectedRoute>
                <MyTickets />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tickets/:id"
            element={
              <ProtectedRoute>
                <TicketDetail />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <EmployeeProfile />
              </ProtectedRoute>
            }
          />
          <Route
            path="/preferences"
            element={
              <ProtectedRoute>
                <PreferencesPage />
              </ProtectedRoute>
            }
          />

          {/* Operational Business Analytics */}
          <Route
            path="/analytics"
            element={
              <ProtectedRoute condition={canViewAnalytics}>
                <AnalyticsDashboard />
              </ProtectedRoute>
            }
          />

          {/* Managerial Approvals */}
          <Route
            path="/approvals"
            element={
              <ProtectedRoute condition={isApprover}>
                <ApprovalQueue />
              </ProtectedRoute>
            }
          />

          {/* Department Specialist Workspace */}
          <Route
            path="/queue"
            element={
              <ProtectedRoute condition={isResolver}>
                <DepartmentQueue />
              </ProtectedRoute>
            }
          />
          <Route
            path="/queue/:department"
            element={
              <ProtectedRoute condition={isResolver}>
                <DepartmentQueue />
              </ProtectedRoute>
            }
          />

          {/* Cryptographic Audit Ledger */}
          <Route
            path="/audit-chain"
            element={
              <ProtectedRoute condition={isAuditor}>
                <AuditLedger />
              </ProtectedRoute>
            }
          />

          {/* Administration & IAM Records */}
          <Route path="/admin/metrics" element={<ProtectedRoute condition={isAdmin}><Dashboard /></ProtectedRoute>} />
          <Route path="/users" element={<ProtectedRoute condition={isAdmin}><UserList /></ProtectedRoute>} />
          <Route path="/users/new" element={<ProtectedRoute condition={isAdmin}><UserForm /></ProtectedRoute>} />
          <Route path="/users/:id/edit" element={<ProtectedRoute condition={isAdmin}><UserForm /></ProtectedRoute>} />
          <Route path="/roles" element={<ProtectedRoute condition={isAdmin}><RoleList /></ProtectedRoute>} />
          <Route path="/roles/new" element={<ProtectedRoute condition={isAdmin}><RoleForm /></ProtectedRoute>} />
          <Route path="/roles/:id/edit" element={<ProtectedRoute condition={isAdmin}><RoleForm /></ProtectedRoute>} />
          <Route path="/profiles" element={<ProtectedRoute condition={isAdmin}><ProfileList /></ProtectedRoute>} />
          <Route path="/profiles/new" element={<ProtectedRoute condition={isAdmin}><ProfileForm /></ProtectedRoute>} />
          <Route path="/profiles/:id/edit" element={<ProtectedRoute condition={isAdmin}><ProfileForm /></ProtectedRoute>} />
          <Route path="/user-role-mappings" element={<ProtectedRoute condition={isAdmin}><UserRoleMapping /></ProtectedRoute>} />
          <Route path="/user-role-mappings/new" element={<ProtectedRoute condition={isAdmin}><UserRoleMappingForm /></ProtectedRoute>} />
          <Route path="/audit" element={<ProtectedRoute condition={isAdmin}><AuditLogs /></ProtectedRoute>} />

          {/* Catch-all 404 Route */}
          <Route
            path="*"
            element={
              <div className="ep-card" style={{ textAlign: 'center', padding: '56px 24px', maxWidth: '520px', margin: '32px auto' }}>
                <h2 style={{ fontSize: '24px', margin: '0 0 8px', color: 'var(--ep-text-primary)' }}>
                  404 — Page Not Found
                </h2>
                <p style={{ color: 'var(--ep-text-muted)', margin: '0 0 20px', fontSize: '14px' }}>
                  The requested URL does not exist in EPAMS.
                </p>
                <Link to="/" className="ep-btn ep-btn--primary">
                  Return to Dashboard
                </Link>
              </div>
            }
          />
        </Routes>
      </Suspense>

      <LoginModal isOpen={loginModalOpen} onClose={() => setLoginModalOpen(false)} />
    </AppLayout>
  );
}

function App() {
  return (
    <PreferencesProvider>
      <ToastProvider>
        <Router>
          <AuthProvider>
            <AppContent />
          </AuthProvider>
        </Router>
      </ToastProvider>
    </PreferencesProvider>
  );
}

export default App;
