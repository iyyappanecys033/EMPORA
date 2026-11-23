import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import { usePreferences } from '../../services/PreferencesContext';
import {
  DashboardIcon,
  CatalogIcon,
  TicketIcon,
  BellIcon,
  ChartIcon,
  ApprovalIcon,
  QueueIcon,
  ShieldIcon,
  UsersIcon,
  UserIcon,
  SettingsIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  KeyIcon,
  FileTextIcon,
  LinkIcon
} from '../common/Icons';

const Sidebar = ({ mobileOpen = false, onCloseMobile }) => {
  const { isAuthenticated, isAdmin, isAuditor, isResolver, isApprover, canViewAnalytics, department } = useAuth();
  const { sidebarCollapsed, toggleSidebarCollapsed } = usePreferences();

  if (!isAuthenticated) {
    return null;
  }

  const navClass = ({ isActive }) =>
    `ep-nav-link ${isActive ? 'ep-nav-link--active' : ''}`;

  const renderLink = (to, label, IconComp, end = false) => (
    <NavLink
      key={to}
      to={to}
      end={end}
      className={navClass}
      title={sidebarCollapsed ? label : undefined}
      aria-label={sidebarCollapsed ? label : undefined}
      onClick={() => {
        if (mobileOpen && onCloseMobile) onCloseMobile();
      }}
    >
      <IconComp size={18} />
      <span className="ep-nav-label">{label}</span>
    </NavLink>
  );

  const renderContent = (isDrawer = false) => {
    const collapsed = !isDrawer && sidebarCollapsed;
    return (
      <nav aria-label="Primary workspace navigation" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {isDrawer && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 10px 14px', borderBottom: '1px solid var(--ep-border)', marginBottom: '8px' }}>
            <span style={{ fontWeight: 800, fontSize: '15px', color: 'var(--ep-text-primary)' }}>EMPORA Navigation</span>
            <button
              type="button"
              onClick={onCloseMobile}
              className="ep-btn ep-btn--ghost ep-btn--sm"
              aria-label="Close navigation drawer"
            >
              &times;
            </button>
          </div>
        )}

        <div style={{ flex: 1 }}>
          <div className="ep-nav-group-label">{collapsed ? 'WORK' : 'WORKSPACE'}</div>
          {renderLink('/', 'Dashboard', DashboardIcon, true)}
          {isAuthenticated && renderLink('/catalog', 'Service Catalog', CatalogIcon)}
          {isAuthenticated && renderLink('/tickets', 'My Tickets', TicketIcon)}
          {isAuthenticated && renderLink('/notifications', 'Notifications', BellIcon)}

          {canViewAnalytics && (
            <>
              <div className="ep-nav-group-label">{collapsed ? 'DATA' : 'INSIGHTS'}</div>
              {renderLink('/analytics', 'Operational Analytics', ChartIcon)}
            </>
          )}

          {isApprover && (
            <>
              <div className="ep-nav-group-label">{collapsed ? 'APPR' : 'APPROVALS'}</div>
              {renderLink('/approvals', 'Pending Approvals', ApprovalIcon)}
            </>
          )}

          {isResolver && (
            <>
              <div className="ep-nav-group-label">{collapsed ? 'DESK' : 'SPECIALIST QUEUE'}</div>
              {renderLink(
                department ? `/queue/${department}` : '/queue/IT',
                department ? `${department} Queue` : 'Department Queue',
                QueueIcon
              )}
            </>
          )}

          {isAuditor && (
            <>
              <div className="ep-nav-group-label">{collapsed ? 'AUDIT' : 'AUDIT & SECURITY'}</div>
              {renderLink('/audit-chain', 'Cryptographic Ledger', ShieldIcon)}
            </>
          )}

          {isAuthenticated && (
            <>
              <div className="ep-nav-group-label">{collapsed ? 'ME' : 'ACCOUNT'}</div>
              {renderLink('/profile', 'My Profile', UserIcon)}
              {renderLink('/preferences', 'Preferences', SettingsIcon)}
            </>
          )}

          {isAdmin && (
            <>
              <div className="ep-nav-group-label">{collapsed ? 'ADM' : 'ADMINISTRATION'}</div>
              {renderLink('/admin/metrics', 'System Metrics', ChartIcon)}
              {renderLink('/users', 'Users', UsersIcon)}
              {renderLink('/roles', 'Roles', KeyIcon)}
              {renderLink('/profiles', 'Profiles', UserIcon)}
              {renderLink('/user-role-mappings', 'Role Mappings', LinkIcon)}
              {renderLink('/audit', 'Legacy Audit', FileTextIcon)}
            </>
          )}
        </div>

        <div className="ep-sidebar-footer">
          {!isAuthenticated && !collapsed && (
            <div
              style={{
                padding: '10px 12px',
                backgroundColor: 'var(--ep-info-bg)',
                borderRadius: 'var(--ep-radius-sm)',
                border: '1px solid var(--ep-info-border)',
                fontSize: '12px',
                color: 'var(--ep-info-text)',
                lineHeight: 1.45,
                marginBottom: '10px'
              }}
            >
              <strong>Guest Preview</strong>
              <p style={{ margin: '4px 0 0' }}>
                Sign in to submit requests, track tickets, and manage your profile.
              </p>
            </div>
          )}

          {!isDrawer && (
            <button
              type="button"
              onClick={toggleSidebarCollapsed}
              className="ep-btn ep-btn--ghost"
              style={{ width: '100%', justifyContent: collapsed ? 'center' : 'flex-start' }}
              aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              {collapsed ? <ChevronRightIcon size={17} /> : <ChevronLeftIcon size={17} />}
              {!collapsed && <span style={{ fontSize: '12px' }}>Collapse Sidebar</span>}
            </button>
          )}
        </div>
      </nav>
    );
  };

  return (
    <>
      <aside className={`ep-sidebar ep-sidebar-desktop ${sidebarCollapsed ? 'ep-sidebar--collapsed' : ''}`}>
        {renderContent(false)}
      </aside>

      {mobileOpen && (
        <>
          <div className="ep-drawer-backdrop" onClick={onCloseMobile} aria-hidden="true" />
          <aside className="ep-sidebar ep-sidebar-drawer" role="dialog" aria-modal="true" aria-label="Navigation menu">
            {renderContent(true)}
          </aside>
        </>
      )}
    </>
  );
};

export default Sidebar;
