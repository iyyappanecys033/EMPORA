import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import { useToast } from '../../services/ToastContext';
import { TableSkeleton } from '../common/LoadingSkeleton';
import EmptyState from '../common/EmptyState';
import Alert from '../common/Alert';
import { ApprovalIcon } from '../common/Icons';

const ApprovalQueue = () => {
  const { currentUser } = useAuth();
  const { showToast } = useToast();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTask, setActiveTask] = useState(null);
  const [decisionType, setDecisionType] = useState('APPROVED');
  const [decisionNotes, setDecisionNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [alert, setAlert] = useState(null);

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    setAlert(null);
    try {
      const data = await ticketService.getPendingApprovals();
      setTasks(data || []);
    } catch (err) {
      setAlert(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  useEffect(() => {
    if (!activeTask) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape' && !submitting) setActiveTask(null);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [activeTask, submitting]);

  const handleOpenDecision = (task, type) => {
    setActiveTask(task);
    setDecisionType(type);
    setDecisionNotes(type === 'APPROVED' ? 'Approved within operational team budget allowance.' : '');
  };

  const handleDecisionSubmit = async (e) => {
    e.preventDefault();
    if (decisionType === 'REJECTED' && decisionNotes.trim().length < 15) {
      setAlert({
        title: 'Validation Error',
        detail: 'Rejection reasoning must be at least 15 characters long.'
      });
      return;
    }

    setSubmitting(true);
    setAlert(null);
    try {
      await ticketService.decideApproval(activeTask.id, decisionType, decisionNotes.trim());
      const resolvedId = activeTask.id;
      setActiveTask(null);
      setDecisionNotes('');
      showToast({
        type: 'success',
        title: 'Decision Recorded',
        message: `Task #${resolvedId} marked as ${decisionType}.`
      });
      setAlert({
        type: 'success',
        title: 'Decision Recorded',
        detail: `Task #${resolvedId} was successfully marked as ${decisionType}.`
      });
      fetchTasks();
    } catch (err) {
      setAlert(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">Approval Center</h1>
          <p className="ep-page-subtitle">
            Review pending service requests submitted by direct reports and departmental teams ({tasks.length} pending).
          </p>
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

      {loading ? (
        <TableSkeleton rows={3} />
      ) : tasks.length === 0 ? (
        <EmptyState
          icon={<ApprovalIcon size={26} />}
          title="No pending approvals"
          description="Your queue is clear! All assigned approval tasks have been resolved."
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {tasks.map((task) => {
            const requester = task.requesterUsername || task.ticketRequester;
            const isSelfRequest = requester && requester === currentUser?.username;

            return (
              <article
                key={task.id}
                className="ep-card"
                style={{
                  borderColor: isSelfRequest ? 'var(--ep-danger-border)' : 'var(--ep-border)',
                  borderLeft: `4px solid ${isSelfRequest ? 'var(--ep-danger)' : 'var(--ep-primary)'}`
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px', marginBottom: '14px' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px', flexWrap: 'wrap' }}>
                      <span
                        style={{
                          fontSize: '11px',
                          fontWeight: 700,
                          color: 'var(--ep-selected-text)',
                          backgroundColor: 'var(--ep-primary-subtle)',
                          border: '1px solid var(--ep-primary-border)',
                          padding: '2px 8px',
                          borderRadius: '4px'
                        }}
                      >
                        {task.approvalStage
                          ? `STAGE ${task.approvalStage} ${task.approvalStage === 1 ? '(MANAGER)' : '(SECONDARY)'}`
                          : task.stage || 'STAGE 1 (MANAGER)'}
                      </span>
                      <span className="ep-tabular" style={{ fontSize: '12px', color: 'var(--ep-text-muted)' }}>
                        Task ID #{task.id}
                      </span>
                    </div>

                    <h2 style={{ margin: '4px 0 0', fontSize: '17px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
                      Ticket #{task.ticketNumber || task.ticketId}
                    </h2>
                  </div>

                  {isSelfRequest ? (
                    <div
                      role="alert"
                      style={{
                        padding: '8px 12px',
                        backgroundColor: 'var(--ep-danger-bg)',
                        borderRadius: 'var(--ep-radius-sm)',
                        border: '1px solid var(--ep-danger-border)',
                        color: 'var(--ep-danger-text)',
                        fontSize: '12px',
                        fontWeight: 600
                      }}
                    >
                      ⚠️ Anti-Self-Approval: You cannot approve your own request.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                      <button
                        type="button"
                        onClick={() => handleOpenDecision(task, 'APPROVED')}
                        className="ep-btn ep-btn--success ep-btn--sm"
                      >
                        ✓ Approve
                      </button>

                      <button
                        type="button"
                        onClick={() => handleOpenDecision(task, 'REJECTED')}
                        className="ep-btn ep-btn--danger-outline ep-btn--sm"
                      >
                        ✕ Reject
                      </button>
                    </div>
                  )}
                </div>

                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                    gap: '12px',
                    backgroundColor: 'var(--ep-bg-subtle)',
                    border: '1px solid var(--ep-border-subtle)',
                    padding: '12px 14px',
                    borderRadius: 'var(--ep-radius-sm)',
                    fontSize: '13px'
                  }}
                >
                  <div>
                    <span style={{ color: 'var(--ep-text-muted)', fontSize: '11px', display: 'block', fontWeight: 600 }}>Requester</span>
                    <strong style={{ color: 'var(--ep-text-primary)' }}>👤 {requester || 'Unknown'}</strong>
                  </div>

                  <div>
                    <span style={{ color: 'var(--ep-text-muted)', fontSize: '11px', display: 'block', fontWeight: 600 }}>Assigned Approver</span>
                    <strong style={{ color: 'var(--ep-text-primary)' }}>{task.approverUsername || currentUser?.username}</strong>
                  </div>

                  <div>
                    <span style={{ color: 'var(--ep-text-muted)', fontSize: '11px', display: 'block', fontWeight: 600 }}>Created At</span>
                    <span className="ep-tabular" style={{ color: 'var(--ep-text-secondary)' }}>
                      {task.createdAt ? new Date(task.createdAt).toLocaleString() : 'N/A'}
                    </span>
                  </div>

                  <div>
                    <span style={{ color: 'var(--ep-text-muted)', fontSize: '11px', display: 'block', fontWeight: 600 }}>Ticket Context</span>
                    <Link to={`/tickets/${task.ticketId}`} style={{ color: 'var(--ep-primary)', fontWeight: 600, textDecoration: 'none' }}>
                      Inspect Full Ticket &rarr;
                    </Link>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {activeTask && (
        <div
          className="ep-modal-backdrop"
          role="dialog"
          aria-modal="true"
          aria-labelledby="approval-decision-modal-title"
          onClick={(e) => {
            if (e.target === e.currentTarget && !submitting) setActiveTask(null);
          }}
        >
          <div className="ep-modal" style={{ maxWidth: '520px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <h3 id="approval-decision-modal-title" style={{ margin: 0, fontSize: '17px', fontWeight: 700 }}>
                {decisionType === 'APPROVED' ? 'Approve Service Request' : 'Reject Service Request'}
              </h3>
              <button
                type="button"
                onClick={() => setActiveTask(null)}
                disabled={submitting}
                aria-label="Close approval modal"
                className="ep-btn ep-btn--ghost ep-btn--sm"
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '13px', color: 'var(--ep-text-muted)', margin: '0 0 14px' }}>
              {decisionType === 'APPROVED'
                ? `Confirming approval for Ticket #${activeTask.ticketNumber || activeTask.ticketId}. You may add optional justification notes.`
                : `State the explicit business reason for rejecting Ticket #${activeTask.ticketNumber || activeTask.ticketId} (minimum 15 characters).`}
            </p>

            <form onSubmit={handleDecisionSubmit}>
              <label htmlFor="approval-decision-notes" className="ep-label">
                {decisionType === 'APPROVED' ? 'Approval Notes (Optional)' : 'Rejection Reason *'}
              </label>
              <textarea
                id="approval-decision-notes"
                rows={4}
                value={decisionNotes}
                onChange={(e) => setDecisionNotes(e.target.value)}
                placeholder={
                  decisionType === 'APPROVED'
                    ? 'Optional approval notes...'
                    : 'State business justification for rejection (e.g. Budgetary constraint, redundant item)...'
                }
                required={decisionType === 'REJECTED'}
                maxLength={1000}
                className="ep-textarea"
              />
              <div style={{ fontSize: '11px', color: 'var(--ep-text-muted)', textAlign: 'right', marginTop: '4px' }}>
                <span className="ep-tabular">{decisionNotes.length} / 1000 characters</span>
                {decisionType === 'REJECTED' && decisionNotes.length < 15 && (
                  <span style={{ color: 'var(--ep-danger-text)', marginLeft: '8px', fontWeight: 600 }}>
                    (Min 15 chars required: {15 - decisionNotes.length} remaining)
                  </span>
                )}
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button type="button" onClick={() => setActiveTask(null)} disabled={submitting} className="ep-btn ep-btn--secondary">
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting || (decisionType === 'REJECTED' && decisionNotes.trim().length < 15)}
                  className={`ep-btn ${decisionType === 'APPROVED' ? 'ep-btn--success' : 'ep-btn--danger'}`}
                >
                  {submitting ? 'Submitting...' : decisionType === 'APPROVED' ? 'Confirm Approval' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ApprovalQueue;
