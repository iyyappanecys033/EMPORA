import React from 'react';

export const EmptyState = ({
  icon = null,
  title = 'No items found',
  description = 'There are no records matching your request at this time.',
  action = null
}) => {
  return (
    <div
      className="ep-card"
      style={{
        textAlign: 'center',
        padding: '44px 24px',
        borderStyle: 'dashed',
        margin: '16px 0'
      }}
    >
      {icon && (
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '56px',
            height: '56px',
            borderRadius: '14px',
            backgroundColor: 'var(--ep-primary-subtle)',
            color: 'var(--ep-primary)',
            fontSize: '26px',
            marginBottom: '12px'
          }}
        >
          {icon}
        </div>
      )}
      <h3 style={{ margin: '0 0 6px', color: 'var(--ep-text-primary)', fontSize: '16px', fontWeight: 700 }}>
        {title}
      </h3>
      <p
        style={{
          margin: '0 auto 18px',
          color: 'var(--ep-text-muted)',
          fontSize: '13px',
          maxWidth: '420px',
          lineHeight: 1.55
        }}
      >
        {description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
};

export default EmptyState;
