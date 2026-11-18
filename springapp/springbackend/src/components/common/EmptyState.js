import React from 'react';

export const EmptyState = ({
  icon = '📭',
  title = 'No items found',
  description = 'There are no records matching your request at this time.',
  action = null
}) => {
  return (
    <div
      style={{
        textAlign: 'center',
        padding: '48px 24px',
        backgroundColor: '#fff',
        borderRadius: '8px',
        border: '1px dashed #cbd5e1',
        margin: '16px 0'
      }}
    >
      <div style={{ fontSize: '42px', marginBottom: '8px' }}>{icon}</div>
      <h3 style={{ margin: '0 0 6px', color: '#1e293b', fontSize: '16px', fontWeight: 600 }}>
        {title}
      </h3>
      <p style={{ margin: '0 0 16px', color: '#64748b', fontSize: '13px', maxWidth: '400px', marginLeft: 'auto', marginRight: 'auto' }}>
        {description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
};

export default EmptyState;

