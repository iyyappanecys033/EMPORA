import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import notificationService from '../../services/notificationService';

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
      // Silently ignore background poll failures; manual open displays detailed error
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
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    if (open) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
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
        aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ''}`}
        aria-expanded={open}
        style={{
          position: 'relative',
          backgroundColor: open ? '#334155' : 'transparent',
          border: '1px solid #475569',
          color: '#f8fafc',
          borderRadius: '6px',
          padding: '6px 10px',
          cursor: 'pointer',
          fontSize: '15px',
          display: 'inline-flex',
          alignItems: 'center',
          gap: '4px'
        }}
      >
        <span aria-hidden="true">🔔</span>
        {unreadCount > 0 && (
          <span
            data-testid="unread-badge"
            style={{
              position: 'absolute',
              top: '-6px',
              right: '-6px',
              backgroundColor: '#ef4444',
              color: '#fff',
              borderRadius: '9999px',
              fontSize: '10px',
              fontWeight: 700,
              padding: '1px 6px',
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
          style={{
            position: 'absolute',
            right: 0,
            top: '42px',
            width: '360px',
            maxWidth: '92vw',
            backgroundColor: '#ffffff',
            color: '#0f172a',
            borderRadius: '10px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 10px 25px rgba(15, 23, 42, 0.2)',
            zIndex: 200,
            overflow: 'hidden'
          }}
        >
          <div
            style={{
              padding: '12px 16px',
              borderBottom: '1px solid #e2e8f0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              backgroundColor: '#f8fafc'
            }}
          >
            <strong style={{ fontSize: '13px', color: '#0f172a' }}>
              Notifications ({unreadCount} unread)
            </strong>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={fetchRecentNotifications}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#475569',
                  fontSize: '12px',
                  cursor: 'pointer',
                  fontWeight: 600
                }}
              >
                Refresh
              </button>
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={handleMarkAllRead}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#2563eb',
                    fontSize: '12px',
                    cursor: 'pointer',
                    fontWeight: 600
                  }}
                >
                  Mark all read
                </button>
              )}
            </div>
          </div>

          <div style={{ maxHeight: '340px', overflowY: 'auto' }}>
            {loading ? (
              <div style={{ padding: '24px', textAlign: 'center', fontSize: '13px', color: '#64748b' }}>
                Loading notifications...
              </div>
            ) : error ? (
              <div style={{ padding: '16px', fontSize: '12px', color: '#b91c1c', backgroundColor: '#fef2f2' }}>
                {error}
              </div>
            ) : items.length === 0 ? (
              <div style={{ padding: '28px 16px', textAlign: 'center', fontSize: '13px', color: '#64748b' }}>
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
                    borderBottom: '1px solid #f1f5f9',
                    backgroundColor: n.isRead ? '#ffffff' : '#eff6ff',
                    cursor: 'pointer',
                    display: 'block'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '12px', fontWeight: n.isRead ? 600 : 700, color: '#0f172a' }}>
                      {n.title}
                    </span>
                    {!n.isRead && (
                      <span
                        style={{
                          width: '8px',
                          height: '8px',
                          borderRadius: '50%',
                          backgroundColor: '#2563eb',
                          flexShrink: 0
                        }}
                      />
                    )}
                  </div>
                  <div style={{ fontSize: '12px', color: '#475569', marginTop: '4px', lineHeight: 1.4 }}>
                    {n.message}
                  </div>
                  <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '6px', display: 'flex', justifyContent: 'space-between' }}>
                    <span>{n.eventType?.replace(/_/g, ' ')}</span>
                    <span>{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</span>
                  </div>
                </button>
              ))
            )}
          </div>

          <div
            style={{
              padding: '10px 16px',
              borderTop: '1px solid #e2e8f0',
              textAlign: 'center',
              backgroundColor: '#f8fafc'
            }}
          >
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                navigate('/notifications');
              }}
              style={{
                background: 'none',
                border: 'none',
                color: '#2563eb',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer'
              }}
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

