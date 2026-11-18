import React from 'react';

export const Alert = ({ type = 'error', title, detail, errors = [], onClose }) => {
  if (!detail && !title && (!errors || errors.length === 0)) return null;

  const isError = type === 'error';
  const isSuccess = type === 'success';

  const theme = isError
    ? { bg: '#fef2f2', border: '#fecaca', text: '#991b1b', titleText: '#7f1d1d' }
    : isSuccess
    ? { bg: '#f0fdf4', border: '#bbf7d0', text: '#166534', titleText: '#14532d' }
    : { bg: '#fffbeb', border: '#fde68a', text: '#92400e', titleText: '#78350f' };

  return (
    <div
      style={{
        backgroundColor: theme.bg,
        border: `1px solid ${theme.border}`,
        borderRadius: '8px',
        padding: '14px 16px',
        margin: '12px 0',
        color: theme.text,
        fontSize: '13px',
        position: 'relative'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          {title && (
            <div style={{ fontWeight: 700, color: theme.titleText, marginBottom: '4px', fontSize: '14px' }}>
              {isError ? '⚠️ ' : isSuccess ? '✅ ' : 'ℹ️ '}
              {title}
            </div>
          )}
          {detail && <div style={{ lineHeight: 1.5 }}>{detail}</div>}

          {errors && errors.length > 0 && (
            <ul style={{ margin: '8px 0 0', paddingLeft: '20px', lineHeight: 1.4 }}>
              {errors.map((err, idx) => (
                <li key={idx}>
                  <strong>{err.field ? `${err.field}: ` : ''}</strong>
                  {err.message || (typeof err === 'string' ? err : JSON.stringify(err))}
                </li>
              ))}
            </ul>
          )}
        </div>

        {onClose && (
          <button
            type="button"
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: theme.text,
              fontSize: '18px',
              fontWeight: 700,
              marginLeft: '12px',
              padding: '0 4px',
              lineHeight: 1
            }}
          >
            &times;
          </button>
        )}
      </div>
    </div>
  );
};

export default Alert;
