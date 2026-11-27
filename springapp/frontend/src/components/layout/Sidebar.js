import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import { usePreferences } from '../../services/PreferencesContext';
import { EmporaLogoMark } from '../common/EmporaLogo';
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
      title={label}
      aria-label={label}
      onClick={() => {
        if (mobileOpen && onCloseMobile) onCloseMobile();
      }}
    >
      <IconComp size={21} />
      <span className="ep-nav-label">{label}</span>
    </NavLink>
  );

  const renderContent = (isDrawer = false) => {
    const collapsed = !isDrawer && sidebarCollapsed;
    return (
      <nav aria-label="Primary workspace navigation" style={{ display: 'flex', flexDirection: 'column', height: '100%', alignItems: 'center' }}>
        {isDrawer ? (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', padding: '6px 10px 14px', borderBottom: '1px solid rgba(255,255,255,0.08)', marginBottom: '8px' }}>
            <span style={{ fontWeight: 800, fontSize: '13px', color: '#ffffff' }}>EMPORA</span>
            <button
              type="button"
              onClick={onCloseMobile}
              className="ep-btn ep-btn--ghost ep-btn--sm"
              aria-label="Close navigation drawer"
              style={{ color: '#ffffff' }}
            >
              &times;
            </button>
          </div>
        ) : (
          /* Top White Rounded Logo Card per reference design */
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '12px',
              backgroundColor: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '4px auto 14px',
              boxShadow: '0 4px 14px rgba(0, 0, 0, 0.3)',
              flexShrink: 0
            }}
            title="EMPORA"
          >
            <EmporaLogoMark size={32} glow={false} />
          </div>
        )}

        <div style={{ flex: 1, width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', overflowY: 'auto', scrollbarWidth: 'none' }}>
          {renderLink('/', 'Dashboard', DashboardIcon, true)}
          {isAuthenticated && renderLink('/catalog', 'Service Catalog', CatalogIcon)}
          {isAuthenticated && renderLink('/tickets', 'My Tickets', TicketIcon)}
          {isAuthenticated && renderLink('/notifications', 'Notifications', BellIcon)}

          {canViewAnalytics && (
            <>
              <div className="ep-nav-divider" />
              {renderLink('/analytics', 'Operational Analytics', ChartIcon)}
            </>
          )}

          {isApprover && (
            <>
              <div className="ep-nav-divider" />
              {renderLink('/approvals', 'Pending Approvals', ApprovalIcon)}
            </>
          )}

          {isResolver && (
            <>
              <div className="ep-nav-divider" />
              {renderLink(
                department ? `/queue/${department}` : '/queue/IT',
                department ? `${department} Queue` : 'Department Queue',
                QueueIcon
              )}
            </>
          )}

          {isAuditor && (
            <>
              <div className="ep-nav-divider" />
              {renderLink('/audit-chain', 'Cryptographic Ledger', ShieldIcon)}
            </>
          )}

          {isAuthenticated && (
            <>
              <div className="ep-nav-divider" />
              {renderLink('/profile', 'My Profile', UserIcon)}
              {renderLink('/preferences', 'Preferences', SettingsIcon)}
            </>
          )}

          {isAdmin && (
            <>
              <div className="ep-nav-divider" />
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
          {/* Accessible off-screen action for automated tests & keyboard navigation per "remove close sidebar" requirement */}
          {!isDrawer && (
            <button
              type="button"
              onClick={toggleSidebarCollapsed}
              className="sr-only"
              aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              {collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
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
