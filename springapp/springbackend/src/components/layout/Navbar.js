import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import { usePreferences } from '../../services/PreferencesContext';
import NotificationBell from '../notifications/NotificationBell';
import Avatar from '../common/Avatar';
import {
  MenuIcon,
  SearchIcon,
  SunIcon,
  MoonIcon,
  MonitorIcon,
  UserIcon,
  SettingsIcon,
  HelpIcon,
  LogOutIcon
} from '../common/Icons';

const Navbar = ({ onOpenLogin, onToggleMobileMenu, onOpenSearch, mobileMenuOpen }) => {
  const { currentUser, isAuthenticated, logout, isAdmin, isResolver, isApprover } = useAuth();
  const { theme, resolvedTheme, updatePreference } = usePreferences();
  const navigate = useNavigate();

  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    if (!profileMenuOpen) return undefined;
    const onOutside = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setProfileMenuOpen(false);
      }
    };
    const onKey = (e) => {
      if (e.key === 'Escape') setProfileMenuOpen(false);
    };
    document.addEventListener('mousedown', onOutside);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onOutside);
      document.removeEventListener('keydown', onKey);
    };
  }, [profileMenuOpen]);

  const roleLabel = isAdmin
    ? 'System Admin'
    : isResolver
    ? 'Department Specialist'
    : isApprover
    ? 'Approver'
    : 'Employee';

  const displayName = currentUser?.fullName || currentUser?.username || 'Employee';

  const cycleTheme = () => {
    const next = theme === 'light' ? 'dark' : theme === 'dark' ? 'system' : 'light';
    updatePreference('theme', next);
  };

  return (
    <header className="ep-topbar">
      <div className="ep-topbar-left">
        <button
          type="button"
          className="ep-icon-btn ep-mobile-menu-btn"
          onClick={onToggleMobileMenu}
          aria-label={mobileMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          aria-expanded={Boolean(mobileMenuOpen)}
        >
          <MenuIcon size={18} />
        </button>

        <Link to="/" className="ep-brand" aria-label="EPAMS Enterprise Workspace Home">
          <span className="ep-brand-mark" aria-hidden="true">E</span>
          <span>
            <span className="ep-brand-title">EPAMS</span>
            <span className="ep-brand-sub">Enterprise Workspace</span>
          </span>
        </Link>

        {onOpenSearch && (
          <button
            type="button"
            className="ep-search-trigger"
            onClick={onOpenSearch}
            aria-label="Quick search workspace (Ctrl+K)"
          >
            <SearchIcon size={15} />
            <span className="ep-search-trigger-text">Quick search...</span>
            <span className="ep-kbd">Ctrl K</span>
          </button>
        )}
      </div>

      <div className="ep-topbar-right">
        <button
          type="button"
          onClick={cycleTheme}
          className="ep-icon-btn"
          aria-label={`Switch theme (current: ${theme})`}
          title={`Theme: ${theme} (active: ${resolvedTheme})`}
        >
          {theme === 'system' ? (
            <MonitorIcon size={17} />
          ) : resolvedTheme === 'dark' ? (
            <MoonIcon size={17} />
          ) : (
            <SunIcon size={17} />
          )}
        </button>

        {isAuthenticated ? (
          <>
            <NotificationBell />

            <div ref={menuRef} style={{ position: 'relative' }}>
              <button
                type="button"
                className="ep-profile-trigger"
                onClick={() => setProfileMenuOpen((prev) => !prev)}
                aria-haspopup="menu"
                aria-expanded={profileMenuOpen}
                aria-label="Account and profile menu"
                style={{
                  background: profileMenuOpen ? 'var(--ep-hover)' : 'var(--ep-bg-surface)'
                }}
              >
                <Avatar name={displayName} size={30} />
                <div className="ep-user-meta-hide-xs" style={{ textAlign: 'left', lineHeight: 1.2 }}>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--ep-text-primary)' }}>
                    {displayName}
                  </div>
                  <div style={{ fontSize: '11px', color: 'var(--ep-text-muted)' }}>
                    {roleLabel}{currentUser?.department ? ` • ${currentUser.department}` : ''}
                  </div>
                </div>
              </button>

              {profileMenuOpen && (
                <div role="menu" aria-label="Employee profile menu" className="ep-dropdown" style={{ width: '264px' }}>
                  <div style={{ padding: '12px 14px', borderBottom: '1px solid var(--ep-border)', background: 'var(--ep-bg-subtle)' }}>
                    <div style={{ fontWeight: 700, fontSize: '13px', color: 'var(--ep-text-primary)' }}>
                      {displayName}
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)', marginTop: '2px' }}>
                      @{currentUser?.username} • {roleLabel}
                    </div>
                  </div>

                  <div style={{ padding: '6px' }}>
                    <Link
                      to="/profile"
                      role="menuitem"
                      className="ep-dropdown-item"
                      style={{ borderRadius: '6px' }}
                      onClick={() => setProfileMenuOpen(false)}
                    >
                      <UserIcon size={16} /> My Profile
                    </Link>

                    <Link
                      to="/preferences"
                      role="menuitem"
                      className="ep-dropdown-item"
                      style={{ borderRadius: '6px' }}
                      onClick={() => setProfileMenuOpen(false)}
                    >
                      <SettingsIcon size={16} /> Preferences &amp; Accessibility
                    </Link>

                    <Link
                      to="/catalog"
                      role="menuitem"
                      className="ep-dropdown-item"
                      style={{ borderRadius: '6px' }}
                      onClick={() => setProfileMenuOpen(false)}
                    >
                      <HelpIcon size={16} /> Service Help &amp; Catalog
                    </Link>
                  </div>

                  <div style={{ padding: '10px 14px', borderTop: '1px solid var(--ep-border)', borderBottom: '1px solid var(--ep-border)' }}>
                    <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', marginBottom: '6px', textTransform: 'uppercase' }}>
                      Theme
                    </div>
                    <div className="ep-pill-group" role="group" aria-label="Theme mode">
                      {[
                        { id: 'light', label: 'Light' },
                        { id: 'dark', label: 'Dark' },
                        { id: 'system', label: 'System' }
                      ].map((opt) => (
                        <button
                          key={opt.id}
                          type="button"
                          onClick={() => updatePreference('theme', opt.id)}
                          aria-pressed={theme === opt.id}
                          className={`ep-pill ${theme === opt.id ? 'ep-pill--active' : ''}`}
                          style={{ flex: 1, textAlign: 'center' }}
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div style={{ padding: '6px' }}>
                    <button
                      type="button"
                      role="menuitem"
                      className="ep-dropdown-item"
                      style={{ borderRadius: '6px', color: 'var(--ep-danger-text)' }}
                      onClick={async () => {
                        setProfileMenuOpen(false);
                        await logout();
                        navigate('/');
                      }}
                    >
                      <LogOutIcon size={16} /> Sign Out
                    </button>
                  </div>
                </div>
              )}
            </div>
          </>
        ) : (
          <button type="button" onClick={onOpenLogin} className="ep-btn ep-btn--primary">
            Sign In / Register
          </button>
        )}
      </div>
    </header>
  );
};

export default Navbar;
