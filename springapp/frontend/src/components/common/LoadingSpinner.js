import React from 'react';

export const LoadingSpinner = ({ message = 'Loading...' }) => {
  return (
    <div
      role="status"
      aria-live="polite"
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 16px',
        color: 'var(--ep-text-muted)',
        fontSize: '14px',
        gap: '12px'
      }}
    >
      <div className="ep-spinner" aria-hidden="true" />
      <span>{message}</span>
    </div>
  );
};

export default LoadingSpinner;
