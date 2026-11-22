import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import notificationService from '../../services/notificationService';
import { TableSkeleton } from '../common/LoadingSkeleton';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import Alert from '../common/Alert';
import { BellIcon, RefreshIcon } from '../common/Icons';

const EVENT_TYPES = [
  { value: '', label: 'All Event Types' },
  { value: 'TICKET_SUBMITTED', label: 'Ticket Submitted' },
  { value: 'TICKET_ASSIGNED', label: 'Ticket Assigned' },
  { value: 'STATUS_CHANGED', label: 'Status Changed' },
  { value: 'TICKET_RESOLVED', label: 'Ticket Resolved' },
  { value: 'TICKET_AUTO_CLOSED', label: 'Ticket Auto-Closed' },
  { value: 'TICKET_REOPENED', label: 'Ticket Reopened' },
  { value: 'TICKET_CANCELLED', label: 'Ticket Cancelled' },
  { value: 'APPROVAL_REQUIRED', label: 'Approval Required' },
  { value: 'TICKET_APPROVED', label: 'Ticket Approved' },
  { value: 'APPROVAL_REJECTED', label: 'Approval Rejected' },
  { value: 'SLA_WARNING', label: 'SLA Warning' },
  { value: 'SLA_BREACHED', label: 'SLA Breached' }
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
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">Notification Center</h1>
          <p className="ep-page-subtitle">
            Real-time workflow alerts for your tickets, approvals, and assignments (<strong className="ep-tabular">{unreadCount}</strong> unread).
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button type="button" onClick={fetchNotifications} className="ep-btn ep-btn--secondary">
            <RefreshIcon size={15} /> Refresh
          </button>
          <button type="button" onClick={handleMarkAllRead} disabled={unreadCount === 0} className="ep-btn ep-btn--primary">
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

      <div className="ep-filter-bar">
        <label style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--ep-text-secondary)', cursor: 'pointer' }}>
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

        <div style={{ width: '210px' }}>
          <select
            aria-label="Filter by event type"
            value={eventType}
            onChange={(e) => {
              setEventType(e.target.value);
              setPage(0);
            }}
            className="ep-select"
          >
            {EVENT_TYPES.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        <span className="ep-tabular" style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--ep-text-muted)' }}>
          Showing {notifications.length} of {totalElements} notifications
        </span>
      </div>

      {loading ? (
        <TableSkeleton rows={4} />
      ) : notifications.length === 0 ? (
        <EmptyState
          icon={<BellIcon size={26} />}
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
            <article
              key={item.id}
              className="ep-card"
              style={{
                backgroundColor: item.isRead ? 'var(--ep-bg-surface)' : 'var(--ep-primary-subtle)',
                borderColor: item.isRead ? 'var(--ep-border)' : 'var(--ep-primary-border)',
                borderLeft: `4px solid ${item.isRead ? 'var(--ep-border-strong)' : 'var(--ep-primary)'}`,
                padding: '14px 18px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                gap: '16px',
                flexWrap: 'wrap'
              }}
            >
              <div style={{ flex: 1, minWidth: '230px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', marginBottom: '5px' }}>
                  <span
                    style={{
                      fontSize: '10px',
                      fontWeight: 700,
                      padding: '2px 7px',
                      borderRadius: '4px',
                      backgroundColor: item.isRead ? 'var(--ep-bg-muted)' : 'var(--ep-bg-surface)',
                      color: item.isRead ? 'var(--ep-text-secondary)' : 'var(--ep-selected-text)',
                      textTransform: 'uppercase'
                    }}
                  >
                    {item.eventType?.replace(/_/g, ' ')}
                  </span>
                  {!item.isRead && (
                    <span style={{ fontSize: '10px', fontWeight: 800, color: 'var(--ep-primary)' }}>NEW</span>
                  )}
                  <span className="ep-tabular" style={{ fontSize: '11px', color: 'var(--ep-text-muted)' }}>
                    {item.createdAt ? new Date(item.createdAt).toLocaleString() : ''}
                  </span>
                </div>

                <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--ep-text-primary)', marginBottom: '4px' }}>
                  {item.title}
                </div>
                <div style={{ fontSize: '13px', color: 'var(--ep-text-secondary)', lineHeight: 1.5 }}>
                  {item.message}
                </div>
              </div>

              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                {(item.ticketId || item.relatedEntityType === 'APPROVAL') && (
                  <button
                    type="button"
                    onClick={() => handleOpenResource(item)}
                    className="ep-btn ep-btn--subtle ep-btn--sm"
                  >
                    {item.relatedEntityType === 'APPROVAL' ? 'Open Approvals' : 'View Ticket'}
                  </button>
                )}
                {!item.isRead && (
                  <button
                    type="button"
                    onClick={() => handleMarkRead(item.id)}
                    className="ep-btn ep-btn--secondary ep-btn--sm"
                  >
                    Mark Read
                  </button>
                )}
              </div>
            </article>
          ))}

          <div className="ep-table-wrap">
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              totalElements={totalElements}
              onPageChange={setPage}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationsPage;
