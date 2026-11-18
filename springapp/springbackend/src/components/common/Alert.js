import React from 'react';

export const Alert = ({ type = 'error', title, detail, errors = [], onClose }) => {
  if (!detail && !title && (!errors || errors.length === 0)) return null;

  const isError = type === 'error';
  const isSuccess = type === 'success';
  const isInfo = type === 'info';

  const bg = isError
    ? 'var(--ep-danger-bg)'
    : isSuccess
    ? 'var(--ep-success-bg)'
    : isInfo
    ? 'var(--ep-info-bg)'
    : 'var(--ep-warning-bg)';
  const bd = isError
    ? 'var(--ep-danger-border)'
    : isSuccess
    ? 'var(--ep-success-border)'
    : isInfo
    ? 'var(--ep-info-border)'
    : 'var(--ep-warning-border)';
  const fg = isError
    ? 'var(--ep-danger-text)'
    : isSuccess
    ? 'var(--ep-success-text)'
    : isInfo
    ? 'var(--ep-info-text)'
    : 'var(--ep-warning-text)';

  return (
    <div
      role={isError ? 'alert' : 'status'}
      style={{
        backgroundColor: bg,
        border: `1px solid ${bd}`,
        borderRadius: 'var(--ep-radius-md)',
        padding: '14px 16px',
        margin: '12px 0',
        color: fg,
        fontSize: '13px',
        position: 'relative'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
        <div style={{ minWidth: 0 }}>
          {title && (
            <div style={{ fontWeight: 700, marginBottom: detail || errors?.length ? '4px' : 0, fontSize: '14px' }}>
              {title}
            </div>
          )}
          {detail && <div style={{ lineHeight: 1.5 }}>{detail}</div>}

          {errors && errors.length > 0 && (
            <ul style={{ margin: '8px 0 0', paddingLeft: '20px', lineHeight: 1.5 }}>
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
            aria-label="Dismiss alert"
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: fg,
              fontSize: '18px',
              fontWeight: 700,
              padding: '2px 6px',
              lineHeight: 1,
              borderRadius: '4px'
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
