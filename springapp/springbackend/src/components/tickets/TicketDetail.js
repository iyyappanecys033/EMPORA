import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import { StatusBadge, PriorityBadge } from '../common/Badge';
import LoadingSpinner from '../common/LoadingSpinner';
import Alert from '../common/Alert';
import TicketComments from './TicketComments';

const TicketDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { currentUser, isResolver, isAdmin } = useAuth();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionReason, setActionReason] = useState('');
  const [statusModalType, setStatusModalType] = useState(null); // 'CANCEL' | 'REOPEN' | 'TRANSITION'
  const [targetStatus, setTargetStatus] = useState('');
  const [alert, setAlert] = useState(null);

  const fetchTicket = useCallback(async () => {
    setLoading(true);
    setAlert(null);
    try {
      const data = await ticketService.getTicketById(id);
      setTicket(data);
    } catch (err) {
      setAlert(err);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchTicket();
  }, [fetchTicket]);

  const handleActionSubmit = async (e) => {
    e.preventDefault();
    if (!actionReason.trim()) {
      setAlert({ title: 'Validation Error', detail: 'A reason must be provided for this action.' });
      return;
    }

    setActionLoading(true);
    setAlert(null);
    try {
      if (statusModalType === 'CANCEL') {
        await ticketService.cancelTicket(ticket.id, actionReason.trim(), ticket.version);
      } else if (statusModalType === 'REOPEN') {
        await ticketService.reopenTicket(ticket.id, actionReason.trim(), ticket.version);
      } else if (statusModalType === 'TRANSITION') {
        await ticketService.updateTicketStatus(ticket.id, targetStatus, actionReason.trim(), ticket.version);
      }
      setStatusModalType(null);
      setActionReason('');
      fetchTicket();
    } catch (err) {
      setAlert(err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleSelfAssign = async () => {
    if (!currentUser?.id) return;
    setActionLoading(true);
    setAlert(null);
    try {
      await ticketService.assignTicket(ticket.id, currentUser.id);
      fetchTicket();
    } catch (err) {
      setAlert(err);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return <LoadingSpinner message="Loading ticket details..." />;
  }

  if (!ticket && alert) {
    return (
      <div style={{ maxWidth: '800px', margin: '40px auto' }}>
        <Alert
          type="error"
          title={alert.title}
          detail={alert.detail}
        />
        <button
          type="button"
          onClick={() => navigate(-1)}
          style={{
            marginTop: '16px',
            padding: '8px 16px',
            backgroundColor: '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer'
          }}
        >
          &larr; Back to Safety
        </button>
      </div>
    );
  }

  if (!ticket) return null;

  const isRequester = currentUser?.username === ticket.requesterUsername;
  const isAssignee = currentUser?.username === ticket.assigneeUsername;
  const canManage = isAssignee || (isResolver && currentUser?.department === ticket.department) || isAdmin;

  const canCancel = isRequester && ['SUBMITTED', 'PENDING_APPROVAL', 'ASSIGNED'].includes(ticket.status);
  const canReopen = isRequester && ticket.status === 'RESOLVED';

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      {/* Top Breadcrumb & Navigation */}
      <div style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: '#64748b' }}>
        <Link to="/tickets" style={{ color: '#2563eb', textDecoration: 'none' }}>
          &larr; My Tickets
        </Link>
        <span>/</span>
        <span style={{ fontWeight: 600, color: '#1e293b' }}>{ticket.ticketNumber}</span>
      </div>

      {alert && (
        <Alert
          type="error"
          title={alert.title}
          detail={alert.detail}
          errors={alert.errors}
          onClose={() => setAlert(null)}
        />
      )}

      {/* Main Ticket Card */}
      <div
        style={{
          backgroundColor: '#fff',
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
          padding: '24px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
        }}
      >
        {/* Header Row */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '20px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
              <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 800, color: '#0f172a' }}>
                {ticket.ticketNumber}
              </h1>
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
            </div>
            <div style={{ fontSize: '13px', color: '#64748b' }}>
              Department: <strong>{ticket.department}</strong>
              {ticket.catalogItemTitle ? ` • Service: ${ticket.catalogItemTitle}` : ''}
              {` • Version token: ${ticket.version}`}
            </div>
          </div>

          {/* Action Buttons */}
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            {canCancel && (
              <button
                type="button"
                onClick={() => {
                  setStatusModalType('CANCEL');
                  setActionReason('');
                }}
                style={{
                  padding: '7px 14px',
                  borderRadius: '6px',
                  border: '1px solid #ef4444',
                  backgroundColor: '#fef2f2',
                  color: '#dc2626',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer'
                }}
              >
                Cancel Ticket
              </button>
            )}

            {canReopen && (
              <button
                type="button"
                onClick={() => {
                  setStatusModalType('REOPEN');
                  setActionReason('');
                }}
                style={{
                  padding: '7px 14px',
                  borderRadius: '6px',
                  border: '1px solid #0284c7',
                  backgroundColor: '#f0f9ff',
                  color: '#0284c7',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer'
                }}
              >
                Reopen Ticket (48h Window)
              </button>
            )}

            {/* Specialist Actions */}
            {canManage && !ticket.assigneeUsername && (
              <button
                type="button"
                onClick={handleSelfAssign}
                disabled={actionLoading}
                style={{
                  padding: '7px 14px',
                  borderRadius: '6px',
                  border: '1px solid #2563eb',
                  backgroundColor: '#2563eb',
                  color: '#fff',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer'
                }}
              >
                Assign to Me
              </button>
            )}

            {canManage && (
              <div style={{ display: 'flex', gap: '6px' }}>
                {ticket.status === 'ASSIGNED' && (
                  <button
                    type="button"
                    onClick={() => {
                      setStatusModalType('TRANSITION');
                      setTargetStatus('IN_PROGRESS');
                      setActionReason('Commenced active diagnosis and resolution.');
                    }}
                    style={actionBtnStyle}
                  >
                    Start Progress
                  </button>
                )}

                {ticket.status === 'IN_PROGRESS' && (
                  <>
                    <button
                      type="button"
                      onClick={() => {
                        setStatusModalType('TRANSITION');
                        setTargetStatus('ON_HOLD');
                        setActionReason('Awaiting customer input or third-party parts.');
                      }}
                      style={{ ...actionBtnStyle, backgroundColor: '#fef3c7', color: '#92400e', border: '1px solid #fde68a' }}
                    >
                      Put On Hold
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setStatusModalType('TRANSITION');
                        setTargetStatus('RESOLVED');
                        setActionReason('Issue diagnosed and resolved successfully.');
                      }}
                      style={{ ...actionBtnStyle, backgroundColor: '#dcfce7', color: '#15803d', border: '1px solid #bbf7d0' }}
                    >
                      Mark Resolved
                    </button>
                  </>
                )}

                {ticket.status === 'ON_HOLD' && (
                  <button
                    type="button"
                    onClick={() => {
                      setStatusModalType('TRANSITION');
                      setTargetStatus('IN_PROGRESS');
                      setActionReason('Resuming resolution activity.');
                    }}
                    style={actionBtnStyle}
                  >
                    Resume Progress
                  </button>
                )}

                {ticket.status === 'RESOLVED' && (isAdmin || isAssignee) && (
                  <button
                    type="button"
                    onClick={() => {
                      setStatusModalType('TRANSITION');
                      setTargetStatus('CLOSED');
                      setActionReason('Resolution confirmed and verified.');
                    }}
                    style={{ ...actionBtnStyle, backgroundColor: '#f1f5f9', color: '#334155', border: '1px solid #cbd5e1' }}
                  >
                    Close Ticket
                  </button>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Justification & Description */}
        <div style={{ backgroundColor: '#f8fafc', borderRadius: '8px', padding: '16px', marginBottom: '20px' }}>
          <div style={{ fontSize: '11px', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', marginBottom: '6px' }}>
            Request Justification / Description
          </div>
          <div style={{ fontSize: '14px', color: '#1e293b', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
            {ticket.description}
          </div>
        </div>

        {/* Metadata Details Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '16px',
            borderTop: '1px solid #f1f5f9',
            paddingTop: '16px',
            fontSize: '13px'
          }}
        >
          <div>
            <div style={{ color: '#94a3b8', fontSize: '11px', fontWeight: 600 }}>Requester</div>
            <div style={{ fontWeight: 600, color: '#1e293b', marginTop: '2px' }}>
              👤 {ticket.requesterUsername}
            </div>
          </div>

          <div>
            <div style={{ color: '#94a3b8', fontSize: '11px', fontWeight: 600 }}>Assigned Specialist</div>
            <div style={{ fontWeight: 600, color: '#1e293b', marginTop: '2px' }}>
              🛠️ {ticket.assigneeUsername || 'Unassigned'}
            </div>
          </div>

          <div>
            <div style={{ color: '#94a3b8', fontSize: '11px', fontWeight: 600 }}>SLA Target Resolution</div>
            <div style={{ fontWeight: 600, color: '#1e293b', marginTop: '2px' }}>
              ⏱️ {ticket.slaTargetDate ? new Date(ticket.slaTargetDate).toLocaleString() : 'N/A'}
            </div>
          </div>

          <div>
            <div style={{ color: '#94a3b8', fontSize: '11px', fontWeight: 600 }}>Paused Time</div>
            <div style={{ fontWeight: 600, color: '#1e293b', marginTop: '2px' }}>
              ⏸️ {ticket.totalPausedMinutes || 0} minutes
            </div>
          </div>
        </div>
      </div>

      {/* Discussion & Comments */}
      <TicketComments ticketId={ticket.id} />

      {/* Action / Reason Modal */}
      {statusModalType && (
        <div style={modalOverlayStyle}>
          <div style={modalContentStyle}>
            <h3 style={{ margin: '0 0 12px', fontSize: '16px', color: '#0f172a' }}>
              {statusModalType === 'CANCEL'
                ? 'Cancel Service Ticket'
                : statusModalType === 'REOPEN'
                ? 'Reopen Service Ticket'
                : `Transition Ticket to ${targetStatus}`}
            </h3>

            <p style={{ fontSize: '13px', color: '#64748b', margin: '0 0 14px' }}>
              Please provide a valid reason or notes for recording in the audit trail.
            </p>

            <form onSubmit={handleActionSubmit}>
              <textarea
                rows={3}
                value={actionReason}
                onChange={(e) => setActionReason(e.target.value)}
                placeholder="Enter justification or diagnostic notes..."
                required
                maxLength={1000}
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  borderRadius: '6px',
                  border: '1px solid #cbd5e1',
                  fontSize: '13px',
                  boxSizing: 'border-box'
                }}
              />

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={() => setStatusModalType(null)}
                  disabled={actionLoading}
                  style={{
                    padding: '8px 16px',
                    borderRadius: '6px',
                    border: '1px solid #cbd5e1',
                    backgroundColor: '#fff',
                    color: '#475569',
                    fontSize: '13px',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  Close
                </button>

                <button
                  type="submit"
                  disabled={actionLoading || !actionReason.trim()}
                  style={{
                    padding: '8px 18px',
                    borderRadius: '6px',
                    border: 'none',
                    backgroundColor: actionLoading || !actionReason.trim() ? '#93c5fd' : '#2563eb',
                    color: '#fff',
                    fontSize: '13px',
                    fontWeight: 600,
                    cursor: actionLoading || !actionReason.trim() ? 'not-allowed' : 'pointer'
                  }}
                >
                  {actionLoading ? 'Saving...' : 'Confirm Action'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

const actionBtnStyle = {
  padding: '7px 14px',
  borderRadius: '6px',
  border: '1px solid #2563eb',
  backgroundColor: '#eff6ff',
  color: '#2563eb',
  fontSize: '12px',
  fontWeight: 600,
  cursor: 'pointer'
};

const modalOverlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: 'rgba(15, 23, 42, 0.6)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 100,
  padding: '16px'
};

const modalContentStyle = {
  backgroundColor: '#fff',
  borderRadius: '10px',
  width: '100%',
  maxWidth: '500px',
  padding: '24px',
  boxSizing: 'border-box'
};

export default TicketDetail;

