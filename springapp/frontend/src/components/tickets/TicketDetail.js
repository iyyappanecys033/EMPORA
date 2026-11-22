import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import { useToast } from '../../services/ToastContext';
import { StatusBadge, PriorityBadge } from '../common/Badge';
import StatusTimeline from '../common/StatusTimeline';
import Avatar from '../common/Avatar';
import LoadingSpinner from '../common/LoadingSpinner';
import Alert from '../common/Alert';
import TicketComments from './TicketComments';

const TicketDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { currentUser, isResolver, isAdmin } = useAuth();
  const { showToast } = useToast();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionReason, setActionReason] = useState('');
  const [statusModalType, setStatusModalType] = useState(null);
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

  useEffect(() => {
    if (!statusModalType) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape' && !actionLoading) setStatusModalType(null);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [statusModalType, actionLoading]);

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
      showToast({
        type: 'success',
        title: 'Ticket Updated',
        message: `Lifecycle action recorded for ${ticket.ticketNumber}.`
      });
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
      showToast({ type: 'success', title: 'Ticket Assigned', message: `${ticket.ticketNumber} assigned to you.` });
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
      <div style={{ maxWidth: '760px', margin: '32px auto' }}>
        <Alert type="error" title={alert.title} detail={alert.detail} />
        <button type="button" onClick={() => navigate(-1)} className="ep-btn ep-btn--primary" style={{ marginTop: '12px' }}>
          &larr; Go Back
        </button>
      </div>
    );
  }

  if (!ticket) return null;

  const isRequester = currentUser?.username === ticket.requesterUsername;
  const isAssignee = currentUser?.username === ticket.assigneeUsername;
  const canManage = isAssignee || (isResolver && currentUser?.department === ticket.department) || isAdmin;

  const canCancel = isRequester && ['SUBMITTED', 'PENDING_APPROVAL', 'PENDING_SECONDARY_APPROVAL', 'ASSIGNED'].includes(ticket.status);
  const canReopen = isRequester && ticket.status === 'RESOLVED';
  const isSlaBreached =
    ticket.slaTargetDate &&
    !['RESOLVED', 'CLOSED', 'REJECTED', 'CANCELLED', 'ON_HOLD'].includes(ticket.status) &&
    new Date(ticket.slaTargetDate) < new Date();

  return (
    <div style={{ maxWidth: '1040px', margin: '0 auto' }}>
      {alert && (
        <Alert type="error" title={alert.title} detail={alert.detail} errors={alert.errors} onClose={() => setAlert(null)} />
      )}

      <div className="ep-card">
        {/* Header Row */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '20px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap', marginBottom: '6px' }}>
              <h1 style={{ margin: 0, fontSize: '21px', fontWeight: 800, fontFamily: 'var(--ep-font-mono)', color: 'var(--ep-text-primary)' }}>
                {ticket.ticketNumber}
              </h1>
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
            </div>
            <div style={{ fontSize: '13px', color: 'var(--ep-text-muted)' }}>
              Department: <strong style={{ color: 'var(--ep-text-primary)' }}>{ticket.department}</strong>
              {ticket.catalogItemTitle ? ` • Service: ${ticket.catalogItemTitle}` : ''}
              {` • Version ${ticket.version}`}
            </div>
          </div>

          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {canCancel && (
              <button
                type="button"
                onClick={() => {
                  setStatusModalType('CANCEL');
                  setActionReason('');
                }}
                className="ep-btn ep-btn--danger-outline ep-btn--sm"
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
                className="ep-btn ep-btn--subtle ep-btn--sm"
              >
                Reopen Ticket (48h Business Window)
              </button>
            )}

            {canManage && !ticket.assigneeUsername && (
              <button type="button" onClick={handleSelfAssign} disabled={actionLoading} className="ep-btn ep-btn--primary ep-btn--sm">
                Assign to Me
              </button>
            )}

            {canManage && ticket.status === 'ASSIGNED' && (
              <button
                type="button"
                onClick={() => {
                  setStatusModalType('TRANSITION');
                  setTargetStatus('IN_PROGRESS');
                  setActionReason('Commenced active diagnosis and resolution.');
                }}
                className="ep-btn ep-btn--primary ep-btn--sm"
              >
                Start Progress
              </button>
            )}

            {canManage && ticket.status === 'IN_PROGRESS' && (
              <>
                <button
                  type="button"
                  onClick={() => {
                    setStatusModalType('TRANSITION');
                    setTargetStatus('ON_HOLD');
                    setActionReason('Awaiting requester clarification or external dependency.');
                  }}
                  className="ep-btn ep-btn--secondary ep-btn--sm"
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
                  className="ep-btn ep-btn--success ep-btn--sm"
                >
                  Mark Resolved
                </button>
              </>
            )}

            {canManage && ticket.status === 'ON_HOLD' && (
              <button
                type="button"
                onClick={() => {
                  setStatusModalType('TRANSITION');
                  setTargetStatus('IN_PROGRESS');
                  setActionReason('Resuming resolution activity.');
                }}
                className="ep-btn ep-btn--primary ep-btn--sm"
              >
                Resume Progress
              </button>
            )}

            {canManage && ticket.status === 'RESOLVED' && (isAdmin || isAssignee) && (
              <button
                type="button"
                onClick={() => {
                  setStatusModalType('TRANSITION');
                  setTargetStatus('CLOSED');
                  setActionReason('Resolution confirmed and verified.');
                }}
                className="ep-btn ep-btn--secondary ep-btn--sm"
              >
                Close Ticket
              </button>
            )}
          </div>
        </div>

        {/* 11-State Lifecycle Progress Timeline */}
        <StatusTimeline status={ticket.status} />

        {/* Description */}
        <div
          style={{
            backgroundColor: 'var(--ep-bg-subtle)',
            border: '1px solid var(--ep-border)',
            borderRadius: 'var(--ep-radius-sm)',
            padding: '16px',
            marginBottom: '20px'
          }}
        >
          <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', marginBottom: '6px' }}>
            Request Justification / Description
          </div>
          <div style={{ fontSize: '14px', color: 'var(--ep-text-primary)', lineHeight: 1.65, whiteSpace: 'pre-wrap' }}>
            {ticket.description}
          </div>
        </div>

        {/* Metadata Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '16px',
            borderTop: '1px solid var(--ep-border)',
            paddingTop: '16px',
            fontSize: '13px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Avatar name={ticket.requesterUsername} size={32} />
            <div>
              <div style={{ color: 'var(--ep-text-muted)', fontSize: '11px', fontWeight: 600 }}>Requester</div>
              <div style={{ fontWeight: 600, color: 'var(--ep-text-primary)' }}>{ticket.requesterUsername}</div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Avatar name={ticket.assigneeUsername || '?'} size={32} />
            <div>
              <div style={{ color: 'var(--ep-text-muted)', fontSize: '11px', fontWeight: 600 }}>Assigned Specialist</div>
              <div style={{ fontWeight: 600, color: ticket.assigneeUsername ? 'var(--ep-text-primary)' : 'var(--ep-text-muted)' }}>
                {ticket.assigneeUsername || 'Unassigned'}
              </div>
            </div>
          </div>

          <div>
            <div style={{ color: 'var(--ep-text-muted)', fontSize: '11px', fontWeight: 600 }}>SLA Target Resolution</div>
            <div className="ep-tabular" style={{ fontWeight: 700, color: isSlaBreached ? 'var(--ep-danger-text)' : 'var(--ep-text-primary)', marginTop: '2px' }}>
              {ticket.slaTargetDate ? new Date(ticket.slaTargetDate).toLocaleString() : 'N/A'}
              {isSlaBreached && ' (Overdue)'}
            </div>
          </div>

          <div>
            <div style={{ color: 'var(--ep-text-muted)', fontSize: '11px', fontWeight: 600 }}>Paused SLA Time</div>
            <div className="ep-tabular" style={{ fontWeight: 600, color: 'var(--ep-text-primary)', marginTop: '2px' }}>
              {ticket.totalPausedMinutes || 0} minutes
            </div>
          </div>
        </div>
      </div>

      <TicketComments ticketId={ticket.id} />

      {statusModalType && (
        <div
          className="ep-modal-backdrop"
          role="dialog"
          aria-modal="true"
          aria-labelledby="ticket-action-modal-title"
          onClick={(e) => {
            if (e.target === e.currentTarget && !actionLoading) setStatusModalType(null);
          }}
        >
          <div className="ep-modal" style={{ maxWidth: '500px' }}>
            <h3 id="ticket-action-modal-title" style={{ margin: '0 0 8px', fontSize: '17px', fontWeight: 700 }}>
              {statusModalType === 'CANCEL'
                ? 'Cancel Service Ticket'
                : statusModalType === 'REOPEN'
                ? 'Reopen Service Ticket'
                : `Transition Ticket to ${targetStatus.replace(/_/g, ' ')}`}
            </h3>

            <p style={{ fontSize: '13px', color: 'var(--ep-text-muted)', margin: '0 0 14px' }}>
              Provide clear justification for recording in the immutable cryptographic audit chain.
            </p>

            <form onSubmit={handleActionSubmit}>
              <label htmlFor="ticket-action-reason" className="ep-label">Reason / Diagnostic Notes *</label>
              <textarea
                id="ticket-action-reason"
                rows={3}
                value={actionReason}
                onChange={(e) => setActionReason(e.target.value)}
                placeholder="Enter justification or diagnostic notes..."
                required
                maxLength={1000}
                className="ep-textarea"
              />

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button type="button" onClick={() => setStatusModalType(null)} disabled={actionLoading} className="ep-btn ep-btn--secondary">
                  Close
                </button>
                <button type="submit" disabled={actionLoading || !actionReason.trim()} className="ep-btn ep-btn--primary">
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

export default TicketDetail;
