import React from 'react';

// Status color palette conforming to 11-State Machine
const STATUS_COLORS = {
  DRAFT: { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' },
  SUBMITTED: { bg: '#e0f2fe', text: '#0369a1', border: '#bae6fd' },
  PENDING_APPROVAL: { bg: '#fef3c7', text: '#b45309', border: '#fde68a' },
  PENDING_SECONDARY_APPROVAL: { bg: '#ffedd5', text: '#c2410c', border: '#fed7aa' },
  ASSIGNED: { bg: '#ede9fe', text: '#6d28d9', border: '#ddd6fe' },
  IN_PROGRESS: { bg: '#dbeafe', text: '#1d4ed8', border: '#bfdbfe' },
  ON_HOLD: { bg: '#fef9c3', text: '#a16207', border: '#fef08a' },
  RESOLVED: { bg: '#dcfce7', text: '#15803d', border: '#bbf7d0' },
  CLOSED: { bg: '#f3f4f6', text: '#374151', border: '#e5e7eb' },
  REJECTED: { bg: '#fee2e2', text: '#b91c1c', border: '#fecaca' },
  CANCELLED: { bg: '#f1f5f9', text: '#64748b', border: '#e2e8f0' }
};

const PRIORITY_COLORS = {
  LOW: { bg: '#f0fdf4', text: '#166534', border: '#bbf7d0' },
  MEDIUM: { bg: '#eff6ff', text: '#1e40af', border: '#bfdbfe' },
  HIGH: { bg: '#fff7ed', text: '#c2410c', border: '#fed7aa' },
  URGENT: { bg: '#fff1f2', text: '#be123c', border: '#fecdd3' },
  CRITICAL: { bg: '#fef2f2', text: '#991b1b', border: '#fecaca' }
};

export const StatusBadge = ({ status }) => {
  const norm = (status || '').toUpperCase();
  const theme = STATUS_COLORS[norm] || { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '3px 10px',
        borderRadius: '9999px',
        fontSize: '12px',
        fontWeight: 600,
        backgroundColor: theme.bg,
        color: theme.text,
        border: `1px solid ${theme.border}`,
        textTransform: 'uppercase',
        letterSpacing: '0.03em',
        whiteSpace: 'nowrap'
      }}
    >
      <span
        style={{
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          backgroundColor: theme.text,
          marginRight: '6px'
        }}
      />
      {norm.replace(/_/g, ' ')}
    </span>
  );
};

export const PriorityBadge = ({ priority }) => {
  const norm = (priority || '').toUpperCase();
  const theme = PRIORITY_COLORS[norm] || { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '11px',
        fontWeight: 700,
        backgroundColor: theme.bg,
        color: theme.text,
        border: `1px solid ${theme.border}`,
        textTransform: 'uppercase',
        letterSpacing: '0.04em'
      }}
    >
      {norm}
    </span>
  );
};

export default StatusBadge;

