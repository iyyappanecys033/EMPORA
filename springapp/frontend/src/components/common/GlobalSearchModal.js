import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import ticketService from '../../services/ticketService';
import { SearchIcon } from './Icons';

const GlobalSearchModal = ({ isOpen, onClose }) => {
  const navigate = useNavigate();
  const { isAuthenticated, isApprover, isResolver, isAuditor, isAdmin, canViewAnalytics, department } = useAuth();
  const [query, setQuery] = useState('');
  const [catalogItems, setCatalogItems] = useState([]);
  const [myTickets, setMyTickets] = useState([]);
  const inputRef = useRef(null);
  const previousFocusRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return undefined;
    previousFocusRef.current = document.activeElement;
    setQuery('');
    setTimeout(() => inputRef.current?.focus(), 20);

    let active = true;
    ticketService
      .getCatalogItems({ size: 25 })
      .then((res) => active && setCatalogItems(res?.content || []))
      .catch(() => {});

    if (isAuthenticated) {
      ticketService
        .getMyTickets(0, 10)
        .then((res) => active && setMyTickets(res?.content || []))
        .catch(() => {});
    }
    return () => {
      active = false;
      if (previousFocusRef.current && typeof previousFocusRef.current.focus === 'function') {
        previousFocusRef.current.focus();
      }
    };
  }, [isOpen, isAuthenticated]);

  useEffect(() => {
    if (!isOpen) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [isOpen, onClose]);

  const navTargets = useMemo(() => {
    const items = [
      { label: 'Employee Dashboard', sub: 'Home workspace & active requests', path: '/' },
      { label: 'Service Catalog', sub: 'Browse IT, HR & Facilities services', path: '/catalog' }
    ];
    if (isAuthenticated) {
      items.push(
        { label: 'My Tickets', sub: 'Track your service requests', path: '/tickets' },
        { label: 'Notifications', sub: 'Alerts & workflow updates', path: '/notifications' },
        { label: 'My Profile', sub: 'Employee details & manager', path: '/profile' },
        { label: 'Preferences', sub: 'Theme, density & accessibility', path: '/preferences' }
      );
    }
    if (canViewAnalytics) {
      items.push({ label: 'Operational Analytics', sub: 'SLA, backlog & workload KPIs', path: '/analytics' });
    }
    if (isApprover) {
      items.push({ label: 'Approval Center', sub: 'Review pending requests', path: '/approvals' });
    }
    if (isResolver) {
      items.push({
        label: department ? `${department} Department Queue` : 'Department Queue',
        sub: 'Triage & fulfill assigned requests',
        path: department ? `/queue/${department}` : '/queue/IT'
      });
    }
    if (isAuditor) {
      items.push({ label: 'Cryptographic Audit Ledger', sub: 'SHA-256 chain & verification', path: '/audit-chain' });
    }
    if (isAdmin) {
      items.push(
        { label: 'System Metrics', sub: 'IAM administrative overview', path: '/admin/metrics' },
        { label: 'Users Management', sub: 'Employee accounts', path: '/users' },
        { label: 'Roles & Permissions', sub: 'RBAC administration', path: '/roles' },
        { label: 'Employee Profiles Admin', sub: 'Directory & profile records', path: '/profiles' }
      );
    }
    return items;
  }, [isAuthenticated, isApprover, isResolver, isAuditor, isAdmin, canViewAnalytics, department]);

  if (!isOpen) return null;

  const q = query.trim().toLowerCase();
  const matchedNav = navTargets.filter(
    (n) => !q || n.label.toLowerCase().includes(q) || n.sub.toLowerCase().includes(q)
  );
  const matchedCatalog = catalogItems
    .filter(
      (c) =>
        !q ||
        (c.title || c.name || '').toLowerCase().includes(q) ||
        (c.department || '').toLowerCase().includes(q) ||
        (c.category || '').toLowerCase().includes(q)
    )
    .slice(0, 5);
  const matchedTickets = myTickets
    .filter(
      (t) =>
        !q ||
        (t.ticketNumber || '').toLowerCase().includes(q) ||
        (t.description || '').toLowerCase().includes(q)
    )
    .slice(0, 5);

  const go = (path) => {
    onClose();
    navigate(path);
  };

  return (
    <div
      className="ep-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-label="Quick search EMPORA workspace"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="ep-modal" style={{ maxWidth: '580px', padding: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', paddingBottom: '12px', borderBottom: '1px solid var(--ep-border)' }}>
          <SearchIcon size={18} />
          <input
            ref={inputRef}
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Escape') {
                e.preventDefault();
                onClose();
              }
            }}
            placeholder="Jump to workspace, service catalog item, or your recent tickets..."
            aria-label="Search workspaces, services, and recent tickets"
            style={{
              border: 'none',
              background: 'transparent',
              padding: '4px 0',
              fontSize: '14px',
              color: 'var(--ep-text-primary)',
              outline: 'none',
              boxShadow: 'none'
            }}
          />
          <button type="button" onClick={onClose} className="ep-btn ep-btn--ghost ep-btn--sm">
            Esc
          </button>
        </div>

        <div style={{ maxHeight: '360px', overflowY: 'auto', marginTop: '10px' }}>
          {matchedNav.length > 0 && (
            <div style={{ marginBottom: '12px' }}>
              <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', padding: '4px 8px', textTransform: 'uppercase' }}>
                Workspaces
              </div>
              {matchedNav.map((item) => (
                <button
                  key={item.path}
                  type="button"
                  onClick={() => go(item.path)}
                  className="ep-dropdown-item"
                  style={{ borderRadius: '6px' }}
                >
                  <div>
                    <div style={{ fontWeight: 600 }}>{item.label}</div>
                    <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)' }}>{item.sub}</div>
                  </div>
                </button>
              ))}
            </div>
          )}

          {matchedCatalog.length > 0 && (
            <div style={{ marginBottom: '12px' }}>
              <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', padding: '4px 8px', textTransform: 'uppercase' }}>
                Service Catalog Offerings
              </div>
              {matchedCatalog.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => go('/catalog')}
                  className="ep-dropdown-item"
                  style={{ borderRadius: '6px' }}
                >
                  <div>
                    <div style={{ fontWeight: 600 }}>{item.title || item.name}</div>
                    <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)' }}>
                      {item.department} {item.slaTargetHours ? `• ${item.slaTargetHours}h SLA` : ''}
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}

          {matchedTickets.length > 0 && (
            <div style={{ marginBottom: '8px' }}>
              <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', padding: '4px 8px', textTransform: 'uppercase' }}>
                Your Recent Tickets
              </div>
              {matchedTickets.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => go(`/tickets/${t.id}`)}
                  className="ep-dropdown-item"
                  style={{ borderRadius: '6px' }}
                >
                  <div>
                    <div style={{ fontWeight: 600 }}>{t.ticketNumber}</div>
                    <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)' }}>{t.description}</div>
                  </div>
                </button>
              ))}
            </div>
          )}

          {matchedNav.length === 0 && matchedCatalog.length === 0 && matchedTickets.length === 0 && (
            <div style={{ padding: '28px 16px', textAlign: 'center', fontSize: '13px', color: 'var(--ep-text-muted)' }}>
              No matching workspaces, catalog services, or recent tickets found.
            </div>
          )}
        </div>

        <div style={{ paddingTop: '10px', borderTop: '1px solid var(--ep-border)', fontSize: '11px', color: 'var(--ep-text-muted)' }}>
          Searches authorized navigation routes, loaded Service Catalog items, and your recent tickets.
        </div>
      </div>
    </div>
  );
};

export default GlobalSearchModal;
