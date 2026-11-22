import React from 'react';

const PRIMARY_FLOW = [
  { key: 'SUBMITTED', label: 'Submitted' },
  { key: 'PENDING_APPROVAL', label: 'Manager Approval' },
  { key: 'ASSIGNED', label: 'Assigned' },
  { key: 'IN_PROGRESS', label: 'In Progress' },
  { key: 'RESOLVED', label: 'Resolved' },
  { key: 'CLOSED', label: 'Closed' }
];

const GUIDANCE_MAP = {
  DRAFT: 'Ticket is in draft state and has not been submitted yet.',
  SUBMITTED: 'Request received and queued for approval routing or department assignment.',
  PENDING_APPROVAL: 'Awaiting Stage 1 Manager approval before specialist assignment.',
  PENDING_SECONDARY_APPROVAL: 'Stage 1 approved. Awaiting Stage 2 Secondary / Department Head approval.',
  ASSIGNED: 'Assigned to the target department queue. A specialist will begin work shortly.',
  IN_PROGRESS: 'A department specialist is actively working on resolving this request.',
  ON_HOLD: 'SLA clock is paused while awaiting clarification or external dependencies.',
  RESOLVED: 'Marked resolved. You may verify resolution or reopen within the 48-business-hour dispute window.',
  CLOSED: 'This service request is closed and archived in the cryptographic audit trail.',
  REJECTED: 'Request was declined during approval review. Check comments or approval notes for details.',
  CANCELLED: 'Request was cancelled by the requester prior to fulfillment.'
};

function getStepIndex(status) {
  switch (status) {
    case 'DRAFT': return -1;
    case 'SUBMITTED': return 0;
    case 'PENDING_APPROVAL':
    case 'PENDING_SECONDARY_APPROVAL': return 1;
    case 'ASSIGNED': return 2;
    case 'IN_PROGRESS':
    case 'ON_HOLD': return 3;
    case 'RESOLVED': return 4;
    case 'CLOSED': return 5;
    default: return 1;
  }
}

const StatusTimeline = ({ status }) => {
  const norm = (status || 'SUBMITTED').toUpperCase();
  const isTerminalAlt = norm === 'REJECTED' || norm === 'CANCELLED';
  const currentIdx = getStepIndex(norm);

  const steps = PRIMARY_FLOW.map((step, idx) => {
    let label = step.label;
    if (step.key === 'PENDING_APPROVAL' && norm === 'PENDING_SECONDARY_APPROVAL') {
      label = 'Secondary Approval';
    }
    if (step.key === 'IN_PROGRESS' && norm === 'ON_HOLD') {
      label = 'On Hold (Paused)';
    }
    if (idx === 0 && norm === 'DRAFT') {
      label = 'Draft';
    }

    let state = 'upcoming';
    if (isTerminalAlt) {
      if (idx === 0) state = 'done';
      else if (idx === 1) state = 'danger';
    } else if (idx < currentIdx) {
      state = 'done';
    } else if (idx === currentIdx) {
      state = norm === 'ON_HOLD' ? 'warn' : 'active';
    }

    return {
      ...step,
      label: isTerminalAlt && idx === 1 ? (norm === 'REJECTED' ? 'Rejected' : 'Cancelled') : label,
      state
    };
  });

  return (
    <div style={{ marginBottom: '20px' }}>
      <div className="ep-timeline" role="list" aria-label="Ticket lifecycle progress">
        {steps.map((s, idx) => (
          <div
            key={s.key}
            role="listitem"
            aria-current={s.state === 'active' || s.state === 'warn' || s.state === 'danger' ? 'step' : undefined}
            className={`ep-timeline-step ${
              s.state === 'done'
                ? 'ep-timeline-step--done'
                : s.state === 'active'
                ? 'ep-timeline-step--active'
                : s.state === 'warn'
                ? 'ep-timeline-step--warn'
                : s.state === 'danger'
                ? 'ep-timeline-step--danger'
                : ''
            }`}
          >
            <span className="ep-timeline-dot">
              {s.state === 'done' ? '✓' : s.state === 'danger' ? '!' : idx + 1}
            </span>
            <span className="ep-timeline-label">{s.label}</span>
          </div>
        ))}
      </div>

      <div
        style={{
          marginTop: '8px',
          padding: '10px 14px',
          borderRadius: 'var(--ep-radius-sm)',
          backgroundColor: isTerminalAlt ? 'var(--ep-danger-bg)' : norm === 'ON_HOLD' ? 'var(--ep-warning-bg)' : 'var(--ep-info-bg)',
          border: `1px solid ${isTerminalAlt ? 'var(--ep-danger-border)' : norm === 'ON_HOLD' ? 'var(--ep-warning-border)' : 'var(--ep-info-border)'}`,
          color: isTerminalAlt ? 'var(--ep-danger-text)' : norm === 'ON_HOLD' ? 'var(--ep-warning-text)' : 'var(--ep-info-text)',
          fontSize: '12px',
          fontWeight: 500
        }}
      >
        <strong>Lifecycle Status:</strong> {GUIDANCE_MAP[norm] || `Current stage: ${norm.replace(/_/g, ' ')}.`}
      </div>
    </div>
  );
};

export default StatusTimeline;
