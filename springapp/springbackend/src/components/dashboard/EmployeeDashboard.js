import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import ticketService from '../../services/ticketService';
import notificationService from '../../services/notificationService';
import { StatusBadge, PriorityBadge } from '../common/Badge';
import { DashboardSkeleton } from '../common/LoadingSkeleton';
import Avatar from '../common/Avatar';
import CreateTicketModal from '../tickets/CreateTicketModal';
import {
  PlusIcon,
  TicketIcon,
  ApprovalIcon,
  BellIcon,
  UserIcon,
  CatalogIcon,
  QueueIcon
} from '../common/Icons';

function getGreeting() {
  const hr = new Date().getHours();
  if (hr < 12) return 'Good morning';
  if (hr < 17) return 'Good afternoon';
  return 'Good evening';
}

const EmployeeDashboard = ({ onOpenLogin }) => {
  const { currentUser, isAuthenticated, isApprover, isResolver, department } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [totalTickets, setTotalTickets] = useState(0);
  const [catalogItems, setCatalogItems] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [selectedCatalogItem, setSelectedCatalogItem] = useState(null);

  const fetchDashboardData = useCallback(async () => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const [ticketRes, catalogRes, notifRes, unreadRes] = await Promise.all([
        ticketService.getMyTickets(0, 5),
        ticketService.getCatalogItems({ size: 4 }),
        notificationService.getMyNotifications({ page: 0, size: 4 }).catch(() => ({ content: [] })),
        notificationService.getUnreadCount().catch(() => ({ unreadCount: 0 }))
      ]);
      setTickets(ticketRes?.content || []);
      setTotalTickets(
        ticketRes?.totalElements !== undefined ? ticketRes.totalElements : (ticketRes?.content || []).length
      );
      setCatalogItems(catalogRes?.content || []);
      setNotifications(notifRes?.content || []);
      setUnreadCount(typeof unreadRes?.unreadCount === 'number' ? unreadRes.unreadCount : 0);
    } catch (err) {
      console.error('Error loading dashboard data', err);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const handleQuickRequest = (item) => {
    setSelectedCatalogItem(item);
    setCreateModalOpen(true);
  };

  if (!isAuthenticated) {
    return (
      <div style={{ maxWidth: '760px', margin: '28px auto' }}>
        <div className="ep-hero" style={{ textAlign: 'center', padding: '44px 28px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '4px 12px', borderRadius: '9999px', background: 'rgba(255,255,255,0.14)', fontSize: '12px', fontWeight: 600, marginBottom: '16px' }}>
            EPAMS — Modern Enterprise Workspace
          </div>
          <h1 style={{ fontSize: 'clamp(1.6rem, 3vw, 2.1rem)', margin: '0 0 12px', fontWeight: 800 }}>
            Welcome to Your Employee Service Portal
          </h1>
          <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: '15px', lineHeight: 1.6, maxWidth: '560px', margin: '0 auto 24px' }}>
            Request IT hardware, HR services, and workplace support, track 11-state ticket progress, manage approvals, and access your employee profile from one unified workspace.
          </p>
          <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', flexWrap: 'wrap' }}>
            {onOpenLogin && (
              <button
                type="button"
                onClick={onOpenLogin}
                className="ep-btn"
                style={{ backgroundColor: '#ffffff', color: '#1e1b4b', fontWeight: 700, padding: '10px 22px' }}
              >
                Sign In to Workspace
              </button>
            )}
            <Link
              to="/catalog"
              className="ep-btn"
              style={{ backgroundColor: 'rgba(255,255,255,0.14)', color: '#ffffff', border: '1px solid rgba(255,255,255,0.28)' }}
            >
              Browse Service Catalog
            </Link>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '16px' }}>
          {[
            { title: 'Self-Service Catalog', desc: 'Standardized IT, HR, and Facilities offerings with transparent SLA targets.' },
            { title: 'Real-Time Approvals', desc: 'Streamlined multi-stage managerial approvals with built-in governance.' },
            { title: 'Cryptographic Audit Trail', desc: 'Tamper-evident SHA-256 lifecycle history for every enterprise action.' }
          ].map((feat) => (
            <div key={feat.title} className="ep-card">
              <h2 style={{ margin: '0 0 6px', fontSize: '15px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>{feat.title}</h2>
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--ep-text-muted)', lineHeight: 1.5 }}>{feat.desc}</p>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (loading) {
    return <DashboardSkeleton />;
  }

  const activeRecentCount = tickets.filter(
    (t) => !['RESOLVED', 'CLOSED', 'REJECTED', 'CANCELLED'].includes(t.status)
  ).length;
  const displayName = currentUser?.fullName || currentUser?.username;

  return (
    <div>
      {/* Personalized Enterprise Welcome Banner */}
      <section className="ep-hero" aria-label="Employee welcome summary">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <Avatar name={displayName} size={52} />
            <div>
              <h1 style={{ margin: '0 0 4px', fontSize: 'clamp(1.25rem, 2.2vw, 1.55rem)', fontWeight: 800 }}>
                {getGreeting()}, {displayName}
              </h1>
              <p style={{ margin: 0, fontSize: '13px', color: 'rgba(255,255,255,0.82)' }}>
                {currentUser?.designation ? `${currentUser.designation} • ` : ''}
                {currentUser?.department ? `${currentUser.department} Department` : 'EPAMS Member'}
                {currentUser?.employeeId ? ` • ID: ${currentUser.employeeId}` : ''}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setSelectedCatalogItem(null);
              setCreateModalOpen(true);
            }}
            className="ep-btn"
            style={{
              backgroundColor: '#ffffff',
              color: '#1e1b4b',
              fontWeight: 700,
              padding: '10px 18px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.18)'
            }}
          >
            <PlusIcon size={17} /> New Service Request
          </button>
        </div>

        {/* Quick Actions Strip */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '18px', paddingTop: '16px', borderTop: '1px solid rgba(255,255,255,0.16)' }}>
          <Link to="/catalog" className="ep-btn ep-btn--sm" style={heroShortcutStyle}>
            <CatalogIcon size={14} /> Service Catalog
          </Link>
          <Link to="/tickets" className="ep-btn ep-btn--sm" style={heroShortcutStyle}>
            <TicketIcon size={14} /> My Tickets ({totalTickets})
          </Link>
          {isApprover && (
            <Link to="/approvals" className="ep-btn ep-btn--sm" style={heroShortcutStyle}>
              <ApprovalIcon size={14} /> Pending Approvals
            </Link>
          )}
          {isResolver && (
            <Link to={department ? `/queue/${department}` : '/queue/IT'} className="ep-btn ep-btn--sm" style={heroShortcutStyle}>
              <QueueIcon size={14} /> {department || 'IT'} Queue
            </Link>
          )}
          <Link to="/notifications" className="ep-btn ep-btn--sm" style={heroShortcutStyle}>
            <BellIcon size={14} /> Notifications {unreadCount > 0 ? `(${unreadCount})` : ''}
          </Link>
          <Link to="/profile" className="ep-btn ep-btn--sm" style={heroShortcutStyle}>
            <UserIcon size={14} /> My Profile
          </Link>
        </div>
      </section>

      {/* Metrics Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="ep-card" style={{ borderLeft: '4px solid var(--ep-primary)' }}>
          <div style={metricLabelStyle}>Active Requests (Recent)</div>
          <div className="ep-tabular" style={{ ...metricValueStyle, color: 'var(--ep-primary)' }}>
            {activeRecentCount}
          </div>
          <div style={metricSubStyle}>Open in your latest 5 tickets</div>
        </div>

        <div className="ep-card" style={{ borderLeft: '4px solid var(--ep-secondary)' }}>
          <div style={metricLabelStyle}>Total Tickets</div>
          <div className="ep-tabular" style={{ ...metricValueStyle, color: 'var(--ep-text-primary)' }}>
            {totalTickets}
          </div>
          <div style={metricSubStyle}>All-time submitted requests</div>
        </div>

        <div className="ep-card" style={{ borderLeft: `4px solid ${unreadCount > 0 ? 'var(--ep-warning)' : 'var(--ep-success)'}` }}>
          <div style={metricLabelStyle}>Unread Notifications</div>
          <div className="ep-tabular" style={{ ...metricValueStyle, color: unreadCount > 0 ? 'var(--ep-warning)' : 'var(--ep-success)' }}>
            {unreadCount}
          </div>
          <div style={metricSubStyle}>
            <Link to="/notifications" style={{ color: 'var(--ep-primary)', textDecoration: 'none', fontWeight: 600 }}>
              Open inbox &rarr;
            </Link>
          </div>
        </div>

        {isApprover && (
          <div className="ep-card" style={{ borderLeft: '4px solid var(--ep-warning)' }}>
            <div style={metricLabelStyle}>Manager Action</div>
            <Link to="/approvals" style={{ textDecoration: 'none', display: 'block', margin: '10px 0 4px', fontSize: '17px', fontWeight: 800, color: 'var(--ep-warning)' }}>
              Review Approvals &rarr;
            </Link>
            <div style={metricSubStyle}>Direct report &amp; stage approvals</div>
          </div>
        )}

        {isResolver && (
          <div className="ep-card" style={{ borderLeft: '4px solid var(--ep-success)' }}>
            <div style={metricLabelStyle}>Specialist Workspace</div>
            <Link to={department ? `/queue/${department}` : '/queue/IT'} style={{ textDecoration: 'none', display: 'block', margin: '10px 0 4px', fontSize: '17px', fontWeight: 800, color: 'var(--ep-success)' }}>
              {department || 'IT'} Queue &rarr;
            </Link>
            <div style={metricSubStyle}>Triage &amp; fulfill service tickets</div>
          </div>
        )}
      </div>

      {/* Main Workspace Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))', gap: '20px' }}>
        {/* Recent Tickets */}
        <section className="ep-card" aria-labelledby="dash-tickets-heading">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2 id="dash-tickets-heading" style={{ fontSize: '16px', fontWeight: 700, margin: 0 }}>
              My Recent Tickets
            </h2>
            <Link to="/tickets" style={{ fontSize: '12px', color: 'var(--ep-primary)', fontWeight: 600, textDecoration: 'none' }}>
              View All &rarr;
            </Link>
          </div>

          {tickets.length === 0 ? (
            <div style={{ color: 'var(--ep-text-muted)', fontSize: '13px', padding: '24px 12px', textAlign: 'center' }}>
              You haven&apos;t submitted any service tickets yet.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {tickets.map((t) => (
                <Link
                  key={t.id}
                  to={`/tickets/${t.id}`}
                  style={{
                    padding: '12px 14px',
                    borderRadius: 'var(--ep-radius-sm)',
                    border: '1px solid var(--ep-border)',
                    textDecoration: 'none',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    gap: '10px',
                    backgroundColor: 'var(--ep-bg-subtle)'
                  }}
                >
                  <div style={{ minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <span style={{ fontWeight: 700, fontSize: '13px', color: 'var(--ep-text-primary)' }}>
                        {t.ticketNumber}
                      </span>
                      <PriorityBadge priority={t.priority} />
                    </div>
                    <div
                      style={{
                        fontSize: '12px',
                        color: 'var(--ep-text-secondary)',
                        marginTop: '4px',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        maxWidth: '240px'
                      }}
                    >
                      {t.description}
                    </div>
                  </div>
                  <StatusBadge status={t.status} />
                </Link>
              ))}
            </div>
          )}
        </section>

        {/* Featured Services */}
        <section className="ep-card" aria-labelledby="dash-catalog-heading">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2 id="dash-catalog-heading" style={{ fontSize: '16px', fontWeight: 700, margin: 0 }}>
              Featured Services
            </h2>
            <Link to="/catalog" style={{ fontSize: '12px', color: 'var(--ep-primary)', fontWeight: 600, textDecoration: 'none' }}>
              Full Catalog &rarr;
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {catalogItems.length === 0 ? (
              <div style={{ color: 'var(--ep-text-muted)', fontSize: '13px', padding: '24px 12px', textAlign: 'center' }}>
                No catalog items currently active.
              </div>
            ) : (
              catalogItems.map((item) => (
                <div
                  key={item.id}
                  style={{
                    padding: '12px 14px',
                    borderRadius: 'var(--ep-radius-sm)',
                    border: '1px solid var(--ep-border)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    gap: '10px',
                    backgroundColor: 'var(--ep-bg-subtle)'
                  }}
                >
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontWeight: 600, fontSize: '13px', color: 'var(--ep-text-primary)' }}>
                      {item.title || item.name}
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)', marginTop: '2px' }}>
                      {item.department || item.targetDepartment} • SLA: {item.slaTargetHours || item.slaHours || 24}h
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleQuickRequest(item)}
                    className="ep-btn ep-btn--subtle ep-btn--sm"
                  >
                    Request
                  </button>
                </div>
              ))
            )}
          </div>
        </section>

        {/* Notifications Preview */}
        <section className="ep-card" aria-labelledby="dash-notifs-heading">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2 id="dash-notifs-heading" style={{ fontSize: '16px', fontWeight: 700, margin: 0 }}>
              Notifications Preview
            </h2>
            <Link to="/notifications" style={{ fontSize: '12px', color: 'var(--ep-primary)', fontWeight: 600, textDecoration: 'none' }}>
              All Alerts &rarr;
            </Link>
          </div>

          {notifications.length === 0 ? (
            <div style={{ color: 'var(--ep-text-muted)', fontSize: '13px', padding: '24px 12px', textAlign: 'center' }}>
              You&apos;re all caught up — no recent notifications.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {notifications.map((n) => (
                <Link
                  key={n.id}
                  to={n.relatedEntityType === 'APPROVAL' ? '/approvals' : n.ticketId ? `/tickets/${n.ticketId}` : '/notifications'}
                  style={{
                    padding: '10px 12px',
                    borderRadius: 'var(--ep-radius-sm)',
                    border: '1px solid var(--ep-border)',
                    borderLeft: n.isRead ? '3px solid var(--ep-border-strong)' : '3px solid var(--ep-primary)',
                    backgroundColor: n.isRead ? 'var(--ep-bg-subtle)' : 'var(--ep-primary-subtle)',
                    textDecoration: 'none',
                    display: 'block'
                  }}
                >
                  <div style={{ fontSize: '13px', fontWeight: n.isRead ? 600 : 700, color: 'var(--ep-text-primary)' }}>
                    {n.title}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)', marginTop: '2px' }}>
                    {n.message}
                  </div>
                </Link>
              ))}
            </div>
          )}
        </section>
      </div>

      {createModalOpen && (
        <CreateTicketModal
          preselectedItem={selectedCatalogItem}
          isOpen={createModalOpen}
          onClose={() => {
            setCreateModalOpen(false);
            setSelectedCatalogItem(null);
          }}
          onTicketCreated={() => {
            setCreateModalOpen(false);
            setSelectedCatalogItem(null);
            fetchDashboardData();
          }}
        />
      )}
    </div>
  );
};

const heroShortcutStyle = {
  backgroundColor: 'rgba(255, 255, 255, 0.14)',
  color: '#ffffff',
  border: '1px solid rgba(255, 255, 255, 0.22)'
};

const metricLabelStyle = {
  fontSize: '11px',
  fontWeight: 700,
  color: 'var(--ep-text-muted)',
  textTransform: 'uppercase',
  letterSpacing: '0.05em'
};

const metricValueStyle = {
  fontSize: '28px',
  fontWeight: 800,
  margin: '6px 0 2px'
};

const metricSubStyle = {
  fontSize: '12px',
  color: 'var(--ep-text-muted)'
};

export default EmployeeDashboard;
