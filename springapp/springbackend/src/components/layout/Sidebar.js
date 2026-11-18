import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';

const Sidebar = () => {
  const { isAuthenticated, isAdmin, isAuditor, isResolver, isApprover, canViewAnalytics, department } = useAuth();

  const getNavLinkStyle = ({ isActive }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '9px 14px',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: isActive ? 600 : 500,
    color: isActive ? '#2563eb' : '#475569',
    backgroundColor: isActive ? '#eff6ff' : 'transparent',
    textDecoration: 'none',
    transition: 'all 0.15s ease',
    marginBottom: '2px'
  });

  return (
    <aside
      style={{
        width: '230px',
        backgroundColor: '#ffffff',
        borderRight: '1px solid #e2e8f0',
        display: 'flex',
        flexDirection: 'column',
        minHeight: 'calc(100vh - 60px)',
        padding: '16px 12px',
        boxSizing: 'border-box'
      }}
    >
      <div style={{ flex: 1 }}>
        <div style={sectionHeadingStyle}>SERVICE MANAGEMENT</div>
        <NavLink to="/" end style={getNavLinkStyle}>
          <span>📊</span> Dashboard
        </NavLink>
        <NavLink to="/catalog" style={getNavLinkStyle}>
          <span>📋</span> Service Catalog
        </NavLink>
        <NavLink to="/tickets" style={getNavLinkStyle}>
          <span>🎫</span> My Tickets
        </NavLink>
        {isAuthenticated && (
          <NavLink to="/notifications" style={getNavLinkStyle}>
            <span>🔔</span> Notifications
          </NavLink>
        )}

        {canViewAnalytics && (
          <>
            <div style={{ ...sectionHeadingStyle, marginTop: '20px' }}>INSIGHTS</div>
            <NavLink to="/analytics" style={getNavLinkStyle}>
              <span>📊</span> Operational Analytics
            </NavLink>
          </>
        )}

        {isApprover && (
          <>
            <div style={{ ...sectionHeadingStyle, marginTop: '20px' }}>APPROVALS</div>
            <NavLink to="/approvals" style={getNavLinkStyle}>
              <span>✍️</span> Pending Approvals
            </NavLink>
          </>
        )}

        {isResolver && (
          <>
            <div style={{ ...sectionHeadingStyle, marginTop: '20px' }}>SPECIALIST QUEUE</div>
            <NavLink
              to={department ? `/queue/${department}` : '/queue/IT'}
              style={getNavLinkStyle}
            >
              <span>📥</span> {department ? `${department} Queue` : 'Department Queue'}
            </NavLink>
          </>
        )}

        {isAuditor && (
          <>
            <div style={{ ...sectionHeadingStyle, marginTop: '20px' }}>AUDIT & SECURITY</div>
            <NavLink to="/audit-chain" style={getNavLinkStyle}>
              <span>🔒</span> Cryptographic Ledger
            </NavLink>
          </>
        )}

        {isAdmin && (
          <>
            <div style={{ ...sectionHeadingStyle, marginTop: '20px' }}>ADMINISTRATION</div>
            <NavLink to="/admin/metrics" style={getNavLinkStyle}>
              <span>📈</span> System Metrics
            </NavLink>
            <NavLink to="/users" style={getNavLinkStyle}>
              <span>👥</span> Users
            </NavLink>
            <NavLink to="/roles" style={getNavLinkStyle}>
              <span>🛡️</span> Roles
            </NavLink>
            <NavLink to="/profiles" style={getNavLinkStyle}>
              <span>📇</span> Profiles
            </NavLink>
            <NavLink to="/user-role-mappings" style={getNavLinkStyle}>
              <span>🔗</span> Role Mappings
            </NavLink>
            <NavLink to="/audit" style={getNavLinkStyle}>
              <span>📜</span> Legacy Audit
            </NavLink>
          </>
        )}
      </div>

      {!isAuthenticated && (
        <div
          style={{
            padding: '12px',
            backgroundColor: '#eff6ff',
            borderRadius: '6px',
            border: '1px solid #bfdbfe',
            fontSize: '12px',
            color: '#1e40af',
            lineHeight: 1.4
          }}
        >
          <strong>Guest Mode</strong>
          <p style={{ margin: '4px 0 0', color: '#3b82f6' }}>
            Please sign in to submit service tickets and track requests.
          </p>
        </div>
      )}
    </aside>
  );
};

const sectionHeadingStyle = {
  fontSize: '11px',
  fontWeight: 700,
  color: '#94a3b8',
  letterSpacing: '0.05em',
  padding: '6px 14px',
  marginBottom: '4px'
};

export default Sidebar;

