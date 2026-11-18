import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import ticketService from '../../services/ticketService';
import { StatusBadge, PriorityBadge } from '../common/Badge';
import LoadingSpinner from '../common/LoadingSpinner';
import CreateTicketModal from '../tickets/CreateTicketModal';

const EmployeeDashboard = () => {
  const { currentUser, isAuthenticated, isApprover, isResolver, department } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [totalTickets, setTotalTickets] = useState(0);
  const [catalogItems, setCatalogItems] = useState([]);
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
      const [ticketRes, catalogRes] = await Promise.all([
        ticketService.getMyTickets(0, 5),
        ticketService.getCatalogItems({ size: 4 })
      ]);
      setTickets(ticketRes.content || []);
      setTotalTickets(ticketRes.totalElements !== undefined ? ticketRes.totalElements : (ticketRes.content || []).length);
      setCatalogItems(catalogRes.content || []);
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
      <div style={{ textAlign: 'center', padding: '60px 20px', maxWidth: '600px', margin: '0 auto' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>🏢</div>
        <h2 style={{ fontSize: '26px', color: '#0f172a', margin: '0 0 12px' }}>
          Welcome to EPAMS
        </h2>
        <p style={{ color: '#64748b', fontSize: '15px', lineHeight: 1.6, marginBottom: '24px' }}>
          Employee Lifecycle & Service Management Platform. Request equipment, access software services, report facility needs, and track approvals seamlessly.
        </p>
        <div style={{ padding: '16px', backgroundColor: '#eff6ff', borderRadius: '8px', border: '1px solid #bfdbfe', color: '#1e40af', fontSize: '14px' }}>
          Please sign in to access your dashboard, service catalog, and tickets.
        </div>
      </div>
    );
  }

  if (loading) {
    return <LoadingSpinner message="Loading your dashboard overview..." />;
  }

  const activeTickets = tickets.filter(
    (t) => !['RESOLVED', 'CLOSED', 'REJECTED', 'CANCELLED'].includes(t.status)
  );

  return (
    <div>
      {/* Welcome Banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
          borderRadius: '12px',
          padding: '24px 28px',
          color: '#fff',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '16px',
          marginBottom: '24px',
          boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
        }}
      >
        <div>
          <h1 style={{ margin: '0 0 6px', fontSize: '22px', fontWeight: 700 }}>
            Welcome back, {currentUser?.fullName || currentUser?.username} 👋
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: '#94a3b8' }}>
            {currentUser?.designation ? `${currentUser.designation} • ` : ''}
            {currentUser?.department ? `Department: ${currentUser.department}` : ''}
            {currentUser?.employeeId ? ` • ID: ${currentUser.employeeId}` : ''}
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setSelectedCatalogItem(null);
            setCreateModalOpen(true);
          }}
          style={{
            backgroundColor: '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: '8px',
            padding: '10px 20px',
            fontSize: '14px',
            fontWeight: 600,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            boxShadow: '0 2px 4px rgba(37,99,235,0.3)'
          }}
        >
          <span>➕</span> New Service Request
        </button>
      </div>

      {/* Metrics Row */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '16px',
          marginBottom: '28px'
        }}
      >
        <div style={cardStyle}>
          <div style={cardLabelStyle}>Active Requests</div>
          <div style={{ ...cardValueStyle, color: '#2563eb' }}>{activeTickets.length}</div>
          <div style={cardSubtextStyle}>Awaiting resolution</div>
        </div>

        <div style={cardStyle}>
          <div style={cardLabelStyle}>Recent Submissions</div>
          <div style={{ ...cardValueStyle, color: '#0f172a' }}>{totalTickets}</div>
          <div style={cardSubtextStyle}>Total in your history</div>
        </div>

        {isApprover && (
          <div style={{ ...cardStyle, borderLeft: '4px solid #f59e0b' }}>
            <div style={cardLabelStyle}>Manager Role</div>
            <Link to="/approvals" style={{ textDecoration: 'none' }}>
              <div style={{ ...cardValueStyle, color: '#d97706', fontSize: '18px' }}>
                Review Approvals &rarr;
              </div>
            </Link>
            <div style={cardSubtextStyle}>Pending direct report requests</div>
          </div>
        )}

        {isResolver && (
          <div style={{ ...cardStyle, borderLeft: '4px solid #10b981' }}>
            <div style={cardLabelStyle}>Specialist Role</div>
            <Link to={department ? `/queue/${department}` : '/queue/IT'} style={{ textDecoration: 'none' }}>
              <div style={{ ...cardValueStyle, color: '#059669', fontSize: '18px' }}>
                {department || 'IT'} Queue &rarr;
              </div>
            </Link>
            <div style={cardSubtextStyle}>Assigned department queue</div>
          </div>
        )}
      </div>

      {/* Two Column Layout: Featured Services + Recent Tickets */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '24px' }}>
        {/* Popular Services Section */}
        <div style={sectionBoxStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={{ fontSize: '16px', fontWeight: 700, margin: 0, color: '#0f172a' }}>
              Featured Services
            </h2>
            <Link to="/catalog" style={{ fontSize: '13px', color: '#2563eb', fontWeight: 600, textDecoration: 'none' }}>
              View All Catalog &rarr;
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {catalogItems.length === 0 ? (
              <div style={{ color: '#94a3b8', fontSize: '13px', padding: '16px', textAlign: 'center' }}>
                No catalog items currently active.
              </div>
            ) : (
              catalogItems.map((item) => (
                <div
                  key={item.id}
                  style={{
                    padding: '12px 14px',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    backgroundColor: '#fff'
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '14px', color: '#1e293b' }}>
                      {item.title || item.name}
                    </div>
                    <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                      {item.department || item.targetDepartment} • SLA: {item.slaTargetHours || item.slaHours || 24}h
                      {item.estimatedCost ? ` • $${Number(item.estimatedCost).toLocaleString()}` : item.baseCost > 0 ? ` • $${item.baseCost.toFixed(2)}` : ' • Standard'}
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleQuickRequest(item)}
                    style={{
                      padding: '6px 14px',
                      borderRadius: '6px',
                      border: '1px solid #2563eb',
                      backgroundColor: '#eff6ff',
                      color: '#2563eb',
                      fontSize: '12px',
                      fontWeight: 600,
                      cursor: 'pointer'
                    }}
                  >
                    Request
                  </button>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Recent Tickets Section */}
        <div style={sectionBoxStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={{ fontSize: '16px', fontWeight: 700, margin: 0, color: '#0f172a' }}>
              Recent Tickets
            </h2>
            <Link to="/tickets" style={{ fontSize: '13px', color: '#2563eb', fontWeight: 600, textDecoration: 'none' }}>
              View All Tickets &rarr;
            </Link>
          </div>

          {tickets.length === 0 ? (
            <div style={{ color: '#94a3b8', fontSize: '13px', padding: '24px', textAlign: 'center' }}>
              You haven't submitted any service tickets yet.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {tickets.map((t) => (
                <Link
                  key={t.id}
                  to={`/tickets/${t.id}`}
                  style={{
                    padding: '12px 14px',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    textDecoration: 'none',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    backgroundColor: '#fff',
                    transition: 'border-color 0.15s'
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontWeight: 700, fontSize: '13px', color: '#0f172a' }}>
                        {t.ticketNumber}
                      </span>
                      <PriorityBadge priority={t.priority} />
                    </div>
                    <div
                      style={{
                        fontSize: '13px',
                        color: '#475569',
                        marginTop: '4px',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        maxWidth: '260px'
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
        </div>
      </div>

      {/* Ticket Creation Modal */}
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

const cardStyle = {
  backgroundColor: '#fff',
  borderRadius: '8px',
  padding: '16px 20px',
  border: '1px solid #e2e8f0',
  boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)'
};

const cardLabelStyle = {
  fontSize: '12px',
  fontWeight: 600,
  color: '#64748b',
  textTransform: 'uppercase',
  letterSpacing: '0.04em'
};

const cardValueStyle = {
  fontSize: '28px',
  fontWeight: 800,
  margin: '8px 0 2px'
};

const cardSubtextStyle = {
  fontSize: '12px',
  color: '#94a3b8'
};

const sectionBoxStyle = {
  backgroundColor: '#fff',
  borderRadius: '8px',
  border: '1px solid #e2e8f0',
  padding: '20px',
  boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)'
};

export default EmployeeDashboard;

