import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import notificationService from '../../services/notificationService';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import Alert from '../common/Alert';

const EVENT_TYPES = [
  { value: '', label: 'All Event Types' },
  { value: 'TICKET_SUBMITTED', label: 'Ticket Submitted' },
  { value: 'TICKET_ASSIGNED', label: 'Ticket Assigned' },
  { value: 'STATUS_CHANGED', label: 'Status Changed' },
  { value: 'TICKET_RESOLVED', label: 'Ticket Resolved' },
  { value: 'TICKET_REOPENED', label: 'Ticket Reopened' },
  { value: 'TICKET_CANCELLED', label: 'Ticket Cancelled' },
  { value: 'APPROVAL_REQUIRED', label: 'Approval Required' },
  { value: 'TICKET_APPROVED', label: 'Ticket Approved' },
  { value: 'APPROVAL_REJECTED', label: 'Approval Rejected' }
];

const NotificationsPage = () => {
  const navigate = useNavigate();

  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [eventType, setEventType] = useState('');
  const [loading, setLoading] = useState(true);
  const [alert, setAlert] = useState(null);

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    setAlert(null);
    try {
      const params = { page, size: 15 };
      if (unreadOnly) params.unreadOnly = true;
      if (eventType) params.eventType = eventType;

      const [pageData, countData] = await Promise.all([
        notificationService.getMyNotifications(params),
        notificationService.getUnreadCount()
      ]);

      setNotifications(pageData?.content || []);
      setTotalPages(pageData?.totalPages || 1);
      setTotalElements(pageData?.totalElements || 0);
      setUnreadCount(typeof countData?.unreadCount === 'number' ? countData.unreadCount : 0);
    } catch (err) {
      setAlert(err);
    } finally {
      setLoading(false);
    }
  }, [page, unreadOnly, eventType]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  const handleMarkRead = async (id) => {
    try {
      const updated = await notificationService.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, readAt: updated.readAt } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      setAlert(err);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
      setAlert({
        type: 'success',
        title: 'All Notifications Read',
        detail: 'All unread notifications have been marked as read.'
      });
    } catch (err) {
      setAlert(err);
    }
  };

  const handleOpenResource = async (item) => {
    if (!item.isRead) {
      await handleMarkRead(item.id);
    }
    if (item.relatedEntityType === 'APPROVAL') {
      navigate('/approvals');
    } else if (item.ticketId) {
      navigate(`/tickets/${item.ticketId}`);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 700, margin: '0 0 4px', color: '#0f172a' }}>
            Notification Center
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: '#64748b' }}>
            Real-time workflow alerts for your tickets, approvals, and assignments ({unreadCount} unread).
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            type="button"
            onClick={fetchNotifications}
            style={{
              padding: '8px 14px',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              backgroundColor: '#fff',
              color: '#334155',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer'
            }}
          >
            🔄 Refresh
          </button>
          <button
            type="button"
            onClick={handleMarkAllRead}
            disabled={unreadCount === 0}
            style={{
              padding: '8px 14px',
              borderRadius: '6px',
              border: 'none',
              backgroundColor: unreadCount === 0 ? '#cbd5e1' : '#2563eb',
              color: '#fff',
              fontSize: '13px',
              fontWeight: 600,
              cursor: unreadCount === 0 ? 'not-allowed' : 'pointer'
            }}
          >
            ✓ Mark All Read
          </button>
        </div>
      </div>

      {alert && (
        <Alert
          type={alert.type || 'error'}
          title={alert.title}
          detail={alert.detail}
          errors={alert.errors}
          onClose={() => setAlert(null)}
        />
      )}

      {/* Filter Bar */}
      <div
        style={{
          display: 'flex',
          gap: '12px',
          alignItems: 'center',
          flexWrap: 'wrap',
          marginBottom: '18px',
          padding: '12px 16px',
          backgroundColor: '#fff',
          borderRadius: '8px',
          border: '1px solid #e2e8f0'
        }}
      >
        <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: '#334155', cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={unreadOnly}
            onChange={(e) => {
              setUnreadOnly(e.target.checked);
              setPage(0);
            }}
          />
          <span>Unread only</span>
        </label>

        <select
          aria-label="Filter by event type"
          value={eventType}
          onChange={(e) => {
            setEventType(e.target.value);
            setPage(0);
          }}
          style={{
            padding: '7px 12px',
            borderRadius: '6px',
            border: '1px solid #cbd5e1',
            fontSize: '13px',
            backgroundColor: '#fff'
          }}
        >
          {EVENT_TYPES.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <span style={{ marginLeft: 'auto', fontSize: '12px', color: '#64748b' }}>
          Showing {notifications.length} of {totalElements} notifications
        </span>
      </div>

      {loading ? (
        <LoadingSpinner message="Loading your notifications..." />
      ) : notifications.length === 0 ? (
        <EmptyState
          icon="🔔"
          title={unreadOnly ? 'No unread notifications' : 'No notifications found'}
          description={
            unreadOnly
              ? 'You have read all your notifications. Uncheck "Unread only" to view history.'
              : 'Workflow alerts will appear here when tickets are submitted, assigned, approved, or resolved.'
          }
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {notifications.map((item) => (
            <div
              key={item.id}
              style={{
                backgroundColor: item.isRead ? '#ffffff' : '#eff6ff',
                border: item.isRead ? '1px solid #e2e8f0' : '1px solid #bfdbfe',
                borderLeft: item.isRead ? '4px solid #cbd5e1' : '4px solid #2563eb',
                borderRadius: '8px',
                padding: '14px 18px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                gap: '16px',
                flexWrap: 'wrap'
              }}
            >
              <div style={{ flex: 1, minWidth: '240px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', marginBottom: '4px' }}>
                  <span
                    style={{
                      fontSize: '10px',
                      fontWeight: 700,
                      padding: '2px 7px',
                      borderRadius: '4px',
                      backgroundColor: item.isRead ? '#f1f5f9' : '#dbeafe',
                      color: item.isRead ? '#475569' : '#1d4ed8',
                      textTransform: 'uppercase'
                    }}
                  >
                    {item.eventType?.replace(/_/g, ' ')}
                  </span>
                  {!item.isRead && (
                    <span style={{ fontSize: '10px', fontWeight: 700, color: '#2563eb' }}>NEW</span>
                  )}
                  <span style={{ fontSize: '11px', color: '#94a3b8' }}>
                    {item.createdAt ? new Date(item.createdAt).toLocaleString() : ''}
                  </span>
                </div>

                <div style={{ fontSize: '14px', fontWeight: 700, color: '#0f172a', marginBottom: '4px' }}>
                  {item.title}
                </div>
                <div style={{ fontSize: '13px', color: '#475569', lineHeight: 1.5 }}>
                  {item.message}
                </div>
              </div>

              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                {(item.ticketId || item.relatedEntityType === 'APPROVAL') && (
                  <button
                    type="button"
                    onClick={() => handleOpenResource(item)}
                    style={{
                      padding: '6px 12px',
                      borderRadius: '6px',
                      border: '1px solid #2563eb',
                      backgroundColor: '#fff',
                      color: '#2563eb',
                      fontSize: '12px',
                      fontWeight: 600,
                      cursor: 'pointer'
                    }}
                  >
                    {item.relatedEntityType === 'APPROVAL' ? 'Open Approvals' : 'View Ticket'}
                  </button>
                )}
                {!item.isRead && (
                  <button
                    type="button"
                    onClick={() => handleMarkRead(item.id)}
                    style={{
                      padding: '6px 12px',
                      borderRadius: '6px',
                      border: '1px solid #cbd5e1',
                      backgroundColor: '#fff',
                      color: '#334155',
                      fontSize: '12px',
                      fontWeight: 600,
                      cursor: 'pointer'
                    }}
                  >
                    Mark Read
                  </button>
                )}
              </div>
            </div>
          ))}

          <Pagination
            currentPage={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={setPage}
          />
        </div>
      )}
    </div>
  );
};

export default NotificationsPage;

