import React from 'react';
import { useAuth } from '../../services/AuthContext';
import NotificationBell from '../notifications/NotificationBell';

const Navbar = ({ onOpenLogin }) => {
  const { currentUser, isAuthenticated, logout, isAdmin, isResolver, isApprover } = useAuth();

  return (
    <header
      style={{
        height: '60px',
        backgroundColor: '#1e293b',
        color: '#fff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 24px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        position: 'sticky',
        top: 0,
        zIndex: 50
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <div
          style={{
            width: '32px',
            height: '32px',
            borderRadius: '6px',
            background: 'linear-gradient(135deg, #2563eb 0%, #38bdf8 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 800,
            fontSize: '16px',
            color: '#fff'
          }}
        >
          E
        </div>
        <div>
          <span style={{ fontWeight: 700, fontSize: '15px', letterSpacing: '0.02em', color: '#f8fafc' }}>
            EPAMS
          </span>
          <span style={{ fontSize: '12px', color: '#94a3b8', marginLeft: '8px', fontWeight: 500 }}>
            Employee Service Platform
          </span>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {isAuthenticated ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <NotificationBell />
            {currentUser?.department && (
              <span
                style={{
                  fontSize: '11px',
                  fontWeight: 600,
                  backgroundColor: '#334155',
                  color: '#cbd5e1',
                  padding: '3px 8px',
                  borderRadius: '4px',
                  textTransform: 'uppercase'
                }}
              >
                {currentUser.department}
              </span>
            )}

            <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column' }}>
              <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
                {currentUser?.fullName || currentUser?.username}
              </span>
              <span style={{ fontSize: '11px', color: '#94a3b8' }}>
                {isAdmin ? 'System Admin' : isResolver ? 'Department Specialist' : isApprover ? 'Approver' : 'Employee'}
              </span>
            </div>

            <button
              type="button"
              onClick={logout}
              style={{
                fontSize: '12px',
                padding: '6px 12px',
                background: '#dc2626',
                border: 'none',
                color: '#fff',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 600,
                transition: 'background 0.2s'
              }}
            >
              Sign Out
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={onOpenLogin}
            style={{
              fontSize: '13px',
              padding: '6px 16px',
              background: '#2563eb',
              border: 'none',
              color: '#fff',
              borderRadius: '6px',
              cursor: 'pointer',
              fontWeight: 600
            }}
          >
            Sign In / Register
          </button>
        )}
      </div>
    </header>
  );
};

export default Navbar;

