import React from 'react';

const STATUS_TOKEN_MAP = {
  DRAFT: { bg: 'var(--ep-st-draft-bg)', fg: 'var(--ep-st-draft-fg)', bd: 'var(--ep-st-draft-bd)' },
  SUBMITTED: { bg: 'var(--ep-st-submitted-bg)', fg: 'var(--ep-st-submitted-fg)', bd: 'var(--ep-st-submitted-bd)' },
  PENDING_APPROVAL: { bg: 'var(--ep-st-pending-bg)', fg: 'var(--ep-st-pending-fg)', bd: 'var(--ep-st-pending-bd)' },
  PENDING_SECONDARY_APPROVAL: { bg: 'var(--ep-st-pending2-bg)', fg: 'var(--ep-st-pending2-fg)', bd: 'var(--ep-st-pending2-bd)' },
  ASSIGNED: { bg: 'var(--ep-st-assigned-bg)', fg: 'var(--ep-st-assigned-fg)', bd: 'var(--ep-st-assigned-bd)' },
  IN_PROGRESS: { bg: 'var(--ep-st-progress-bg)', fg: 'var(--ep-st-progress-fg)', bd: 'var(--ep-st-progress-bd)' },
  ON_HOLD: { bg: 'var(--ep-st-hold-bg)', fg: 'var(--ep-st-hold-fg)', bd: 'var(--ep-st-hold-bd)' },
  RESOLVED: { bg: 'var(--ep-st-resolved-bg)', fg: 'var(--ep-st-resolved-fg)', bd: 'var(--ep-st-resolved-bd)' },
  CLOSED: { bg: 'var(--ep-st-closed-bg)', fg: 'var(--ep-st-closed-fg)', bd: 'var(--ep-st-closed-bd)' },
  REJECTED: { bg: 'var(--ep-st-rejected-bg)', fg: 'var(--ep-st-rejected-fg)', bd: 'var(--ep-st-rejected-bd)' },
  CANCELLED: { bg: 'var(--ep-st-cancelled-bg)', fg: 'var(--ep-st-cancelled-fg)', bd: 'var(--ep-st-cancelled-bd)' }
};

const PRIORITY_TOKEN_MAP = {
  LOW: { bg: 'var(--ep-pr-low-bg)', fg: 'var(--ep-pr-low-fg)', bd: 'var(--ep-pr-low-bd)' },
  MEDIUM: { bg: 'var(--ep-pr-medium-bg)', fg: 'var(--ep-pr-medium-fg)', bd: 'var(--ep-pr-medium-bd)' },
  HIGH: { bg: 'var(--ep-pr-high-bg)', fg: 'var(--ep-pr-high-fg)', bd: 'var(--ep-pr-high-bd)' },
  URGENT: { bg: 'var(--ep-pr-urgent-bg)', fg: 'var(--ep-pr-urgent-fg)', bd: 'var(--ep-pr-urgent-bd)' },
  CRITICAL: { bg: 'var(--ep-pr-critical-bg)', fg: 'var(--ep-pr-critical-fg)', bd: 'var(--ep-pr-critical-bd)' }
};

export const StatusBadge = ({ status }) => {
  const norm = (status || '').toUpperCase();
  const t = STATUS_TOKEN_MAP[norm] || STATUS_TOKEN_MAP.DRAFT;

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '3px 10px',
        borderRadius: '9999px',
        fontSize: '11px',
        fontWeight: 700,
        backgroundColor: t.bg,
        color: t.fg,
        border: `1px solid ${t.bd}`,
        textTransform: 'uppercase',
        letterSpacing: '0.03em',
        whiteSpace: 'nowrap'
      }}
    >
      <span
        aria-hidden="true"
        style={{
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          backgroundColor: t.fg,
          marginRight: '6px'
        }}
      />
      {norm.replace(/_/g, ' ')}
    </span>
  );
};

export const PriorityBadge = ({ priority }) => {
  const norm = (priority || '').toUpperCase();
  const t = PRIORITY_TOKEN_MAP[norm] || PRIORITY_TOKEN_MAP.MEDIUM;

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '5px',
        fontSize: '11px',
        fontWeight: 700,
        backgroundColor: t.bg,
        color: t.fg,
        border: `1px solid ${t.bd}`,
        textTransform: 'uppercase',
        letterSpacing: '0.04em',
        whiteSpace: 'nowrap'
      }}
    >
      {norm}
    </span>
  );
};

export default StatusBadge;
