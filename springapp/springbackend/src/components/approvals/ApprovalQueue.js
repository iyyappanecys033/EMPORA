import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import Alert from '../common/Alert';

const ApprovalQueue = () => {
  const { currentUser } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTask, setActiveTask] = useState(null);
  const [decisionType, setDecisionType] = useState('APPROVED'); // 'APPROVED' | 'REJECTED'
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
      setActiveTask(null);
      setDecisionNotes('');
      setAlert({
        type: 'success',
        title: 'Decision Recorded',
        detail: `Task #${activeTask.id} was successfully marked as ${decisionType}.`
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
      <div style={{ marginBottom: '20px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 700, margin: '0 0 4px', color: '#0f172a' }}>
          Managerial Approval Queue
        </h1>
        <p style={{ margin: 0, fontSize: '13px', color: '#64748b' }}>
          Review pending service requests submitted by direct reports and departmental teams.
        </p>
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
        <LoadingSpinner message="Fetching pending approval tasks..." />
      ) : tasks.length === 0 ? (
        <EmptyState
          icon="✅"
          title="No pending approvals"
          description="Your queue is clear! All assigned approval tasks have been resolved."
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {tasks.map((task) => {
            const requester = task.requesterUsername || task.ticketRequester;
            const isSelfRequest = requester && requester === currentUser?.username;

            return (
              <div
                key={task.id}
                style={{
                  backgroundColor: '#fff',
                  borderRadius: '10px',
                  border: isSelfRequest ? '1px solid #fecaca' : '1px solid #e2e8f0',
                  padding: '20px',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px', marginBottom: '12px' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                      <span style={{ fontSize: '12px', fontWeight: 700, color: '#2563eb', backgroundColor: '#eff6ff', padding: '2px 8px', borderRadius: '4px' }}>
                        {task.approvalStage ? `STAGE ${task.approvalStage} ${task.approvalStage === 1 ? '(MANAGER)' : '(SECONDARY)'}` : task.stage || 'STAGE 1 (MANAGER)'}
                      </span>
                      <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                        Task ID #{task.id}
                      </span>
                    </div>

                    <h3 style={{ margin: '4px 0', fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                      Ticket #{task.ticketNumber || task.ticketId}
                    </h3>
                  </div>

                  {/* Anti-self-approval warning or Decision buttons */}
                  {isSelfRequest ? (
                    <div style={{ padding: '6px 12px', backgroundColor: '#fef2f2', borderRadius: '6px', border: '1px solid #fecaca', color: '#b91c1c', fontSize: '12px', fontWeight: 600 }}>
                      ⚠️ Anti-Self-Approval: You cannot approve your own request.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        type="button"
                        onClick={() => handleOpenDecision(task, 'APPROVED')}
                        style={{
                          padding: '7px 16px',
                          borderRadius: '6px',
                          border: 'none',
                          backgroundColor: '#16a34a',
                          color: '#fff',
                          fontSize: '12px',
                          fontWeight: 600,
                          cursor: 'pointer'
                        }}
                      >
                        ✓ Approve
                      </button>

                      <button
                        type="button"
                        onClick={() => handleOpenDecision(task, 'REJECTED')}
                        style={{
                          padding: '7px 16px',
                          borderRadius: '6px',
                          border: '1px solid #dc2626',
                          backgroundColor: '#fff',
                          color: '#dc2626',
                          fontSize: '12px',
                          fontWeight: 600,
                          cursor: 'pointer'
                        }}
                      >
                        ✕ Reject
                      </button>
                    </div>
                  )}
                </div>

                {/* Task Details */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', backgroundColor: '#f8fafc', padding: '12px', borderRadius: '6px', fontSize: '13px' }}>
                  <div>
                    <span style={{ color: '#64748b', fontSize: '11px', display: 'block' }}>Requester</span>
                    <strong>👤 {task.requesterUsername || task.ticketRequester || 'Unknown'}</strong>
                  </div>

                  <div>
                    <span style={{ color: '#64748b', fontSize: '11px', display: 'block' }}>Assigned Approver</span>
                    <strong>👔 {task.approverUsername || currentUser?.username}</strong>
                  </div>

                  <div>
                    <span style={{ color: '#64748b', fontSize: '11px', display: 'block' }}>Created At</span>
                    <span>{task.createdAt ? new Date(task.createdAt).toLocaleString() : 'N/A'}</span>
                  </div>

                  <div>
                    <span style={{ color: '#64748b', fontSize: '11px', display: 'block' }}>Ticket Link</span>
                    <Link to={`/tickets/${task.ticketId}`} style={{ color: '#2563eb', fontWeight: 600, textDecoration: 'none' }}>
                      Inspect Full Ticket &rarr;
                    </Link>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Decision Modal */}
      {activeTask && (
        <div style={modalOverlayStyle}>
          <div style={modalContentStyle}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <h3 style={{ margin: 0, fontSize: '16px', color: '#0f172a' }}>
                {decisionType === 'APPROVED' ? 'Approve Service Request' : 'Reject Service Request'}
              </h3>
              <button
                type="button"
                onClick={() => setActiveTask(null)}
                style={{ background: 'transparent', border: 'none', fontSize: '20px', cursor: 'pointer', color: '#64748b' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '13px', color: '#475569', margin: '0 0 14px' }}>
              {decisionType === 'APPROVED'
                ? 'Confirming approval for Ticket #' + (activeTask.ticketNumber || activeTask.ticketId) + '. You may add optional justification notes.'
                : 'Please state the explicit business reason for rejecting Ticket #' + (activeTask.ticketNumber || activeTask.ticketId) + ' (min 15 characters).'}
            </p>

            <form onSubmit={handleDecisionSubmit}>
              <textarea
                rows={4}
                value={decisionNotes}
                onChange={(e) => setDecisionNotes(e.target.value)}
                placeholder={decisionType === 'APPROVED' ? 'Optional approval notes...' : 'State business justification for rejection (e.g. Budgetary constraint, redundant item)...'}
                required={decisionType === 'REJECTED'}
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
              <div style={{ fontSize: '11px', color: '#94a3b8', textAlign: 'right', marginTop: '2px' }}>
                {decisionNotes.length} / 1000 characters
                {decisionType === 'REJECTED' && decisionNotes.length < 15 && (
                  <span style={{ color: '#dc2626', marginLeft: '6px' }}>
                    (Min 15 chars required: {15 - decisionNotes.length} remaining)
                  </span>
                )}
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={() => setActiveTask(null)}
                  disabled={submitting}
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
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={submitting || (decisionType === 'REJECTED' && decisionNotes.trim().length < 15)}
                  style={{
                    padding: '8px 18px',
                    borderRadius: '6px',
                    border: 'none',
                    backgroundColor:
                      submitting || (decisionType === 'REJECTED' && decisionNotes.trim().length < 15)
                        ? '#93c5fd'
                        : decisionType === 'APPROVED'
                        ? '#16a34a'
                        : '#dc2626',
                    color: '#fff',
                    fontSize: '13px',
                    fontWeight: 600,
                    cursor:
                      submitting || (decisionType === 'REJECTED' && decisionNotes.trim().length < 15)
                        ? 'not-allowed'
                        : 'pointer'
                  }}
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
  maxWidth: '520px',
  padding: '24px',
  boxSizing: 'border-box'
};

export default ApprovalQueue;

