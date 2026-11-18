import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import notificationService from '../../services/notificationService';
import { BellIcon } from '../common/Icons';

const POLL_INTERVAL_MS = 30000;

const NotificationBell = () => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const isPollingRef = useRef(false);
  const containerRef = useRef(null);

  const fetchUnreadCount = useCallback(async () => {
    if (!isAuthenticated || isPollingRef.current) return;
    if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return;

    isPollingRef.current = true;
    try {
      const data = await notificationService.getUnreadCount();
      setUnreadCount(typeof data?.unreadCount === 'number' ? data.unreadCount : 0);
    } catch {
      // Silently ignore background poll failures
    } finally {
      isPollingRef.current = false;
    }
  }, [isAuthenticated]);

  const fetchRecentNotifications = useCallback(async () => {
    if (!isAuthenticated) return;
    setLoading(true);
    setError(null);
    try {
      const data = await notificationService.getMyNotifications({ page: 0, size: 6 });
      setItems(data?.content || []);
      const countRes = await notificationService.getUnreadCount();
      setUnreadCount(typeof countRes?.unreadCount === 'number' ? countRes.unreadCount : 0);
    } catch (err) {
      setError(err?.detail || 'Unable to load notifications.');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      setItems([]);
      setOpen(false);
      return undefined;
    }

    fetchUnreadCount();
    const timer = setInterval(fetchUnreadCount, POLL_INTERVAL_MS);

    const handleVisibility = () => {
      if (document.visibilityState === 'visible') {
        fetchUnreadCount();
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);

    return () => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', handleVisibility);
    };
  }, [isAuthenticated, fetchUnreadCount]);

  useEffect(() => {
    if (!open) return undefined;
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    const handleKey = (e) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKey);
    };
  }, [open]);

  if (!isAuthenticated) return null;

  const handleToggle = () => {
    const next = !open;
    setOpen(next);
    if (next) {
      fetchRecentNotifications();
    }
  };

  const handleItemClick = async (item) => {
    try {
      if (!item.isRead) {
        await notificationService.markAsRead(item.id);
        setItems((prev) => prev.map((n) => (n.id === item.id ? { ...n, isRead: true } : n)));
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
    } catch {
      // Allow navigation even if mark-read fails
    }
    setOpen(false);
    if (item.relatedEntityType === 'APPROVAL') {
      navigate('/approvals');
    } else if (item.ticketId) {
      navigate(`/tickets/${item.ticketId}`);
    } else {
      navigate('/notifications');
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setItems((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      setError(err?.detail || 'Failed to mark notifications as read.');
    }
  };

  return (
    <div ref={containerRef} style={{ position: 'relative' }}>
      <button
        type="button"
        onClick={handleToggle}
        className="ep-icon-btn"
        aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ''}`}
        aria-expanded={open}
        style={{
          backgroundColor: open ? 'var(--ep-selected)' : 'var(--ep-bg-surface)',
          color: open ? 'var(--ep-selected-text)' : 'var(--ep-text-secondary)'
        }}
      >
        <BellIcon size={17} />
        {unreadCount > 0 && (
          <span
            data-testid="unread-badge"
            className="ep-tabular"
            style={{
              position: 'absolute',
              top: '-5px',
              right: '-5px',
              backgroundColor: 'var(--ep-danger)',
              color: '#ffffff',
              borderRadius: '9999px',
              fontSize: '10px',
              fontWeight: 700,
              padding: '1px 5px',
              minWidth: '18px',
              textAlign: 'center',
              lineHeight: '14px'
            }}
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Recent notifications"
          className="ep-dropdown"
          style={{ width: '360px', maxWidth: '92vw' }}
        >
          <div
            style={{
              padding: '12px 16px',
              borderBottom: '1px solid var(--ep-border)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              backgroundColor: 'var(--ep-bg-subtle)'
            }}
          >
            <strong style={{ fontSize: '13px', color: 'var(--ep-text-primary)' }}>
              Notifications ({unreadCount} unread)
            </strong>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={fetchRecentNotifications}
                className="ep-btn ep-btn--ghost ep-btn--sm"
                style={{ padding: '2px 6px', minHeight: '24px' }}
              >
                Refresh
              </button>
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={handleMarkAllRead}
                  className="ep-btn ep-btn--ghost ep-btn--sm"
                  style={{ padding: '2px 6px', minHeight: '24px', color: 'var(--ep-primary)' }}
                >
                  Mark all read
                </button>
              )}
            </div>
          </div>

          <div style={{ maxHeight: '340px', overflowY: 'auto' }}>
            {loading ? (
              <div style={{ padding: '24px', textAlign: 'center', fontSize: '13px', color: 'var(--ep-text-muted)' }}>
                Loading notifications...
              </div>
            ) : error ? (
              <div style={{ padding: '14px 16px', fontSize: '12px', color: 'var(--ep-danger-text)', backgroundColor: 'var(--ep-danger-bg)' }}>
                {error}
              </div>
            ) : items.length === 0 ? (
              <div style={{ padding: '28px 16px', textAlign: 'center', fontSize: '13px', color: 'var(--ep-text-muted)' }}>
                You are all caught up! No notifications yet.
              </div>
            ) : (
              items.map((n) => (
                <button
                  key={n.id}
                  type="button"
                  onClick={() => handleItemClick(n)}
                  style={{
                    width: '100%',
                    textAlign: 'left',
                    padding: '12px 16px',
                    border: 'none',
                    borderBottom: '1px solid var(--ep-border-subtle)',
                    backgroundColor: n.isRead ? 'var(--ep-bg-elevated)' : 'var(--ep-primary-subtle)',
                    color: 'var(--ep-text-primary)',
                    cursor: 'pointer',
                    display: 'block'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '12px', fontWeight: n.isRead ? 600 : 700, color: 'var(--ep-text-primary)' }}>
                      {n.title}
                    </span>
                    {!n.isRead && (
                      <span
                        aria-hidden="true"
                        style={{
                          width: '8px',
                          height: '8px',
                          borderRadius: '50%',
                          backgroundColor: 'var(--ep-primary)',
                          flexShrink: 0
                        }}
                      />
                    )}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--ep-text-secondary)', marginTop: '4px', lineHeight: 1.4 }}>
                    {n.message}
                  </div>
                  <div style={{ fontSize: '11px', color: 'var(--ep-text-muted)', marginTop: '6px', display: 'flex', justifyContent: 'space-between', gap: '8px' }}>
                    <span>{n.eventType?.replace(/_/g, ' ')}</span>
                    <span className="ep-tabular">{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</span>
                  </div>
                </button>
              ))
            )}
          </div>

          <div
            style={{
              padding: '10px 16px',
              borderTop: '1px solid var(--ep-border)',
              textAlign: 'center',
              backgroundColor: 'var(--ep-bg-subtle)'
            }}
          >
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                navigate('/notifications');
              }}
              className="ep-btn ep-btn--ghost ep-btn--sm"
              style={{ color: 'var(--ep-primary)' }}
            >
              View All Notifications &rarr;
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;
