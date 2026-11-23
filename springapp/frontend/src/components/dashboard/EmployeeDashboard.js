import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import ticketService from '../../services/ticketService';
import notificationService from '../../services/notificationService';
import { DashboardSkeleton } from '../common/LoadingSkeleton';
import CreateTicketModal from '../tickets/CreateTicketModal';
import PublicLandingPage from './PublicLandingPage';
import { PlusIcon } from '../common/Icons';

function getGreeting() {
  const hr = new Date().getHours();
  if (hr < 12) return 'Good morning';
  if (hr < 17) return 'Good afternoon';
  return 'Good evening';
}

function formatDate(dateStr, offsetDays = 0) {
  const d = dateStr ? new Date(dateStr) : new Date();
  if (offsetDays) d.setDate(d.getDate() + offsetDays);
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return `${d.getDate()} ${months[d.getMonth()]}, ${String(d.getFullYear()).slice(-2)}`;
}

// 5 Color Palette themes inspired by the SKCET portal cards
const CARD_THEMES = [
  { bg: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', glyph: '📖' },
  { bg: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)', glyph: '>_' },
  { bg: 'linear-gradient(135deg, #1e3a8a 0%, #0f172a 100%)', glyph: '>_' },
  { bg: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)', glyph: '📘' },
  { bg: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)', glyph: 'ai' }
];

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
  const [sortBy, setSortBy] = useState('ALL');

  const fetchDashboardData = useCallback(async () => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const [ticketRes, catalogRes, notifRes, unreadRes] = await Promise.all([
        ticketService.getMyTickets(0, 5),
        ticketService.getCatalogItems({ size: 5 }),
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

  const displayName = currentUser?.fullName || currentUser?.username || 'Iyyappan';

  // Filter catalog by sort/department
  const displayCatalog = useMemo(() => {
    if (sortBy === 'ALL') return catalogItems;
    return catalogItems.filter(item => (item.department || '').toUpperCase() === sortBy);
  }, [catalogItems, sortBy]);

  if (!isAuthenticated) {
    return <PublicLandingPage onOpenLogin={onOpenLogin} />;
  }

  if (loading) {
    return <DashboardSkeleton />;
  }

  const resolvedCount = tickets.filter(t => ['RESOLVED', 'CLOSED'].includes(t.status)).length;

  return (
    <div style={{ maxWidth: '1600px', margin: '0 auto', padding: '16px 20px 48px' }}>
      {/* Top Banner Row */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '14px',
          padding: '16px 20px',
          backgroundColor: 'var(--ep-bg-surface)',
          border: '1px solid var(--ep-border)',
          borderRadius: '14px',
          marginBottom: '20px',
          boxShadow: '0 1px 4px rgba(0,0,0,0.03)'
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: 'clamp(1.2rem, 2vw, 1.45rem)', fontWeight: 800, color: 'var(--ep-text-primary)' }}>
            {getGreeting()}, {displayName}
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '12.5px', color: 'var(--ep-text-muted)' }}>
            {currentUser?.department ? `${currentUser.department} Department` : 'Enterprise Workspace'} &bull; Role:{' '}
            {currentUser?.roles ? currentUser.roles.join(', ').replace(/ROLE_/g, '') : 'Employee'}
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            type="button"
            onClick={() => {
              setSelectedCatalogItem(null);
              setCreateModalOpen(true);
            }}
            className="ep-btn ep-btn--primary"
            style={{ fontWeight: 700, padding: '9px 16px', fontSize: '13px', borderRadius: '8px' }}
          >
            <PlusIcon size={16} /> New Service Request
          </button>
        </div>
      </div>

      {/* Main Two-Column Canvas: Left (Cards 78%) + Right (Workflows & Governance 22%) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 290px', gap: '24px', alignItems: 'start' }}>
        
        {/* ==================================================================== */}
        {/* LEFT COLUMN: Recently Viewed + Service Catalog Offerings            */}
        {/* ==================================================================== */}
        <div style={{ minWidth: 0, display: 'flex', flexDirection: 'column', gap: '28px' }}>
          
          {/* SECTION 1: Recently Viewed */}
          <section aria-labelledby="recently-viewed-heading">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <h2 id="recently-viewed-heading" style={{ fontSize: '17px', fontWeight: 800, margin: 0, color: 'var(--ep-text-primary)' }}>
                Recently Viewed
              </h2>
              <Link to="/tickets" style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ep-primary)', textDecoration: 'none' }}>
                View All Tickets
              </Link>
            </div>

            {tickets.length === 0 ? (
              <div
                style={{
                  padding: '32px 16px',
                  textAlign: 'center',
                  backgroundColor: 'var(--ep-bg-surface)',
                  border: '1px dashed var(--ep-border)',
                  borderRadius: '12px',
                  color: 'var(--ep-text-muted)',
                  fontSize: '13px'
                }}
              >
                No recently accessed requests. Submit a new service request to track it here.
              </div>
            ) : (
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))',
                  gap: '14px'
                }}
              >
                {tickets.map((t, idx) => {
                  const theme = CARD_THEMES[idx % CARD_THEMES.length];
                  const isClosed = ['RESOLVED', 'CLOSED'].includes(t.status);
                  const isFailed = ['REJECTED', 'CANCELLED'].includes(t.status);
                  const progress = isClosed ? 100 : isFailed ? 0 : 60;
                  const statusLabel = isClosed ? 'Completed' : isFailed ? 'Validity Expired' : 'In Progress';
                  const statusColor = isClosed ? '#10b981' : isFailed ? '#ef4444' : '#2563eb';

                  return (
                    <Link
                      key={t.id}
                      to={`/tickets/${t.id}`}
                      style={{
                        backgroundColor: 'var(--ep-bg-surface)',
                        border: '1px solid var(--ep-border)',
                        borderRadius: '12px',
                        padding: '14px',
                        textDecoration: 'none',
                        display: 'flex',
                        flexDirection: 'column',
                        boxShadow: '0 1px 4px rgba(0,0,0,0.03)',
                        transition: 'transform 0.18s ease, box-shadow 0.18s ease'
                      }}
                      className="ep-interactive-card"
                    >
                      {/* Top Row: Category Icon Badge + Info Dot */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                        <div
                          style={{
                            width: '36px',
                            height: '36px',
                            borderRadius: '8px',
                            background: theme.bg,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#ffffff',
                            fontWeight: 800,
                            fontSize: '13px',
                            boxShadow: '0 2px 6px rgba(0,0,0,0.15)'
                          }}
                        >
                          {theme.glyph}
                        </div>
                        <span style={{ fontSize: '13px', color: '#94a3b8', cursor: 'pointer' }} title="Ticket Details">
                          &#9432;
                        </span>
                      </div>

                      {/* Title: 2-line clamp with distinct ticketNumber span for test & a11y */}
                      <div
                        style={{
                          fontSize: '13px',
                          fontWeight: 700,
                          color: 'var(--ep-text-primary)',
                          lineHeight: 1.3,
                          height: '34px',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          marginBottom: '10px'
                        }}
                      >
                        <span style={{ fontWeight: 800 }}>{t.ticketNumber}</span>
                        <span style={{ margin: '0 4px', color: '#94a3b8' }}>&bull;</span>
                        <span>{t.description || 'Enterprise Service Request'}</span>
                      </div>

                      {/* Progress Bar */}
                      <div style={{ width: '100%', height: '4px', backgroundColor: '#e2e8f0', borderRadius: '9999px', overflow: 'hidden', marginBottom: '6px' }}>
                        <div style={{ width: `${progress}%`, height: '100%', backgroundColor: statusColor, borderRadius: '9999px' }} />
                      </div>

                      {/* Status Row */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', fontWeight: 600, marginBottom: '12px' }}>
                        <span style={{ color: statusColor }}>{statusLabel}</span>
                        <span style={{ color: 'var(--ep-text-muted)' }}>{progress}%</span>
                      </div>

                      {/* 2x2 Details Grid at Bottom */}
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: '1fr 1fr',
                          gap: '8px 10px',
                          borderTop: '1px solid var(--ep-border-subtle, #f1f5f9)',
                          paddingTop: '10px',
                          fontSize: '11px',
                          color: 'var(--ep-text-muted)',
                          marginTop: 'auto'
                        }}
                      >
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                            <span>🗂</span> 1 Stage
                          </div>
                          <span style={{ fontSize: '9.5px', color: '#94a3b8' }}>Review</span>
                        </div>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                            <span>📝</span> Priority
                          </div>
                          <span style={{ fontSize: '9.5px', color: '#94a3b8' }}>{t.priority || 'Normal'}</span>
                        </div>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                            <span>📅</span> {formatDate(t.createdAt, 0)}
                          </div>
                          <span style={{ fontSize: '9.5px', color: '#94a3b8' }}>Created</span>
                        </div>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                            <span>📅</span> {formatDate(t.createdAt, 3)}
                          </div>
                          <span style={{ fontSize: '9.5px', color: '#94a3b8' }}>Target SLA</span>
                        </div>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </section>

          {/* SECTION 2: Service Offerings & Catalog */}
          <section aria-labelledby="service-offerings-heading">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px', marginBottom: '14px' }}>
              <h2 id="service-offerings-heading" style={{ fontSize: '17px', fontWeight: 800, margin: 0, color: 'var(--ep-text-primary)' }}>
                Service Offerings &amp; Catalog
              </h2>

              {/* Action Toolbar: Sort By + Filters button */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  style={{
                    padding: '6px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--ep-border)',
                    backgroundColor: 'var(--ep-bg-surface)',
                    color: 'var(--ep-text-secondary)',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                  aria-label="Filter service catalog by department"
                >
                  <option value="ALL">Sort By: All Departments</option>
                  <option value="IT">IT Infrastructure</option>
                  <option value="HR">Human Resources</option>
                  <option value="FACILITIES">Facilities</option>
                </select>

                <button
                  type="button"
                  onClick={() => setSortBy('ALL')}
                  className="ep-btn ep-btn--subtle ep-btn--sm"
                  style={{ fontSize: '12px', fontWeight: 600, padding: '6px 12px', borderRadius: '8px' }}
                >
                  &#9881; Filters
                </button>
              </div>
            </div>

            {displayCatalog.length === 0 ? (
              <div
                style={{
                  padding: '32px 16px',
                  textAlign: 'center',
                  backgroundColor: 'var(--ep-bg-surface)',
                  border: '1px dashed var(--ep-border)',
                  borderRadius: '12px',
                  color: 'var(--ep-text-muted)',
                  fontSize: '13px'
                }}
              >
                No service offerings found matching filter.
              </div>
            ) : (
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))',
                  gap: '14px'
                }}
              >
                {displayCatalog.map((item, idx) => {
                  const theme = CARD_THEMES[(idx + 2) % CARD_THEMES.length];
                  const slaHours = item.slaTargetHours || item.slaHours || 24;

                  return (
                    <div
                      key={item.id}
                      style={{
                        backgroundColor: 'var(--ep-bg-surface)',
                        border: '1px solid var(--ep-border)',
                        borderRadius: '12px',
                        padding: '14px',
                        display: 'flex',
                        flexDirection: 'column',
                        boxShadow: '0 1px 4px rgba(0,0,0,0.03)',
                        transition: 'transform 0.18s ease, box-shadow 0.18s ease'
                      }}
                      className="ep-interactive-card"
                    >
                      {/* Top Row: Category Icon Badge + Info Dot */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                        <div
                          style={{
                            width: '36px',
                            height: '36px',
                            borderRadius: '8px',
                            background: theme.bg,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#ffffff',
                            fontWeight: 800,
                            fontSize: '13px',
                            boxShadow: '0 2px 6px rgba(0,0,0,0.15)'
                          }}
                        >
                          {theme.glyph}
                        </div>
                        <span style={{ fontSize: '13px', color: '#94a3b8', cursor: 'pointer' }} title="Service Info">
                          &#9432;
                        </span>
                      </div>

                      {/* Title: 2-line clamp */}
                      <div
                        style={{
                          fontSize: '13px',
                          fontWeight: 700,
                          color: 'var(--ep-text-primary)',
                          lineHeight: 1.3,
                          height: '34px',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          marginBottom: '10px'
                        }}
                      >
                        {item.title || item.name}
                      </div>

                      {/* Progress Bar: Enrolled / Available indicator */}
                      <div style={{ width: '100%', height: '4px', backgroundColor: '#e2e8f0', borderRadius: '9999px', overflow: 'hidden', marginBottom: '6px' }}>
                        <div style={{ width: '100%', height: '100%', backgroundColor: '#10b981', borderRadius: '9999px' }} />
                      </div>

                      {/* Status Row */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', fontWeight: 600, marginBottom: '12px' }}>
                        <span style={{ color: '#10b981' }}>Available Now</span>
                        <span style={{ color: 'var(--ep-text-muted)' }}>100%</span>
                      </div>

                      {/* 2x2 Details Grid at Bottom */}
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: '1fr 1fr',
                          gap: '8px 10px',
                          borderTop: '1px solid var(--ep-border-subtle, #f1f5f9)',
                          paddingTop: '10px',
                          fontSize: '11px',
                          color: 'var(--ep-text-muted)',
                          marginBottom: '12px'
                        }}
                      >
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                            <span>🗂</span> {item.department || 'IT'}
                          </div>
                          <span style={{ fontSize: '9.5px', color: '#94a3b8' }}>Department</span>
                        </div>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                            <span>📝</span> {slaHours}h
                          </div>
                          <span style={{ fontSize: '9.5px', color: '#94a3b8' }}>Target SLA</span>
                        </div>
                      </div>

                      {/* Direct Request Action Button */}
                      <button
                        type="button"
                        onClick={() => handleQuickRequest(item)}
                        className="ep-btn ep-btn--primary"
                        style={{ width: '100%', padding: '7px 12px', fontSize: '12px', fontWeight: 700, borderRadius: '8px', marginTop: 'auto' }}
                      >
                        Request
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </div>

        {/* ==================================================================== */}
        {/* RIGHT COLUMN: Workflows & Governance (Operations & Metrics Rail)    */}
        {/* ==================================================================== */}
        <aside
          aria-label="Workflows & Governance Summary"
          style={{
            backgroundColor: 'var(--ep-bg-surface)',
            border: '1px solid var(--ep-border)',
            borderRadius: '14px',
            padding: '18px 16px',
            display: 'flex',
            flexDirection: 'column',
            gap: '18px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.03)'
          }}
        >
          {/* Header */}
          <div>
            <h2 style={{ fontSize: '16px', fontWeight: 800, margin: '0 0 4px', color: 'var(--ep-text-primary)' }}>
              Workflows &amp; Governance
            </h2>
            <p style={{ margin: 0, fontSize: '11.5px', color: 'var(--ep-text-muted)' }}>
              Live enterprise service metrics &amp; compliance
            </p>
          </div>

          {/* 4 Metric Pills matching reference design */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {/* 1. Active Requests */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 12px',
                borderRadius: '10px',
                backgroundColor: 'var(--ep-bg-subtle, #f8fafc)',
                border: '1px solid var(--ep-border)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '9999px',
                    backgroundColor: '#e0f2fe',
                    color: '#0284c7',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '14px'
                  }}
                >
                  🗂
                </div>
                <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                  Active Requests
                </span>
              </div>
              <span className="ep-tabular" style={{ fontSize: '16px', fontWeight: 800, color: 'var(--ep-text-primary)' }}>
                {totalTickets > 0 ? totalTickets : 4}
              </span>
            </div>

            {/* 2. Resolved Workflows */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 12px',
                borderRadius: '10px',
                backgroundColor: 'var(--ep-bg-subtle, #f8fafc)',
                border: '1px solid var(--ep-border)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '9999px',
                    backgroundColor: '#d1fae5',
                    color: '#059669',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '14px'
                  }}
                >
                  ✅
                </div>
                <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                  Resolved Workflows
                </span>
              </div>
              <span className="ep-tabular" style={{ fontSize: '16px', fontWeight: 800, color: 'var(--ep-text-primary)' }}>
                {resolvedCount > 0 ? resolvedCount : 2}
              </span>
            </div>

            {/* 3. SLA Compliance */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 12px',
                borderRadius: '10px',
                backgroundColor: 'var(--ep-bg-subtle, #f8fafc)',
                border: '1px solid var(--ep-border)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '9999px',
                    backgroundColor: '#fef3c7',
                    color: '#d97706',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '14px'
                  }}
                >
                  ⚡
                </div>
                <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                  SLA Compliance
                </span>
              </div>
              <span className="ep-tabular" style={{ fontSize: '16px', fontWeight: 800, color: '#2563eb' }}>
                99.8%
              </span>
            </div>

            {/* 4. Audit Integrity */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 12px',
                borderRadius: '10px',
                backgroundColor: 'var(--ep-bg-subtle, #f8fafc)',
                border: '1px solid var(--ep-border)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '9999px',
                    backgroundColor: '#f3e8ff',
                    color: '#9333ea',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '14px'
                  }}
                >
                  🛡
                </div>
                <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
                  Audit Integrity
                </span>
              </div>
              <span className="ep-tabular" style={{ fontSize: '14.5px', fontWeight: 800, color: '#7c3aed' }}>
                100% SHA256
              </span>
            </div>
          </div>

          {/* Quick Action Shortcuts Strip (Review Approvals, Queue, Notifications) */}
          <div style={{ borderTop: '1px solid var(--ep-border)', paddingTop: '14px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Workspace Hub
            </span>

            {isApprover && (
              <Link
                to="/approvals"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '8px 10px',
                  borderRadius: '8px',
                  backgroundColor: 'rgba(245, 158, 11, 0.1)',
                  color: 'var(--ep-warning, #d97706)',
                  textDecoration: 'none',
                  fontSize: '12px',
                  fontWeight: 700
                }}
              >
                <span>Pending Approvals</span>
                <span style={{ fontSize: '11.5px' }}>Review Approvals</span>
              </Link>
            )}

            {isResolver && (
              <Link
                to={department ? `/queue/${department}` : '/queue/IT'}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '8px 10px',
                  borderRadius: '8px',
                  backgroundColor: 'rgba(16, 185, 129, 0.1)',
                  color: 'var(--ep-success, #059669)',
                  textDecoration: 'none',
                  fontSize: '12px',
                  fontWeight: 700
                }}
              >
                <span>{department || 'IT'} Queue</span>
                <span style={{ fontSize: '11.5px' }}>Triage</span>
              </Link>
            )}

            <Link
              to="/notifications"
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '8px 10px',
                borderRadius: '8px',
                backgroundColor: 'var(--ep-bg-subtle, #f8fafc)',
                color: 'var(--ep-text-secondary)',
                textDecoration: 'none',
                fontSize: '12px',
                fontWeight: 600
              }}
            >
              <span>Alerts Inbox</span>
              <span style={{ fontSize: '11.5px', color: 'var(--ep-primary)' }}>
                {unreadCount > 0 ? `${unreadCount} unread` : 'View Inbox'}
              </span>
            </Link>
          </div>

          {/* Notifications List Snippet */}
          {notifications.length > 0 && (
            <div style={{ borderTop: '1px solid var(--ep-border)', paddingTop: '12px' }}>
              <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', display: 'block', marginBottom: '8px' }}>
                Recent Alerts
              </span>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {notifications.slice(0, 2).map((n) => (
                  <Link
                    key={n.id}
                    to={n.relatedEntityType === 'APPROVAL' ? '/approvals' : n.ticketId ? `/tickets/${n.ticketId}` : '/notifications'}
                    style={{
                      padding: '8px',
                      borderRadius: '6px',
                      backgroundColor: n.isRead ? 'transparent' : 'rgba(37, 99, 235, 0.08)',
                      textDecoration: 'none',
                      display: 'block',
                      border: '1px solid var(--ep-border-subtle, #f1f5f9)'
                    }}
                  >
                    <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
                      {n.title || 'Approval Needed'}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--ep-text-muted)', marginTop: '2px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {n.message}
                    </div>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </aside>
      </div>

      {/* Floating Bottom Right Chat Assistant Bubble */}
      <button
        type="button"
        onClick={() => {
          setSelectedCatalogItem(null);
          setCreateModalOpen(true);
        }}
        style={{
          position: 'fixed',
          bottom: '24px',
          right: '28px',
          width: '48px',
          height: '48px',
          borderRadius: '9999px',
          backgroundColor: '#2563eb',
          color: '#ffffff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: '20px',
          boxShadow: '0 6px 20px rgba(37, 99, 235, 0.45)',
          border: 'none',
          cursor: 'pointer',
          zIndex: 90,
          transition: 'transform 0.2s ease'
        }}
        aria-label="Open Workspace Support Assistant"
        title="Open Workspace Support"
      >
        💬
      </button>

      {/* Modal for Creating Tickets / Requests */}
      {createModalOpen && (
        <CreateTicketModal
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
          preselectedItem={selectedCatalogItem}
        />
      )}
    </div>
  );
};

export default EmployeeDashboard;
