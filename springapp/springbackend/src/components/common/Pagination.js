import React from 'react';

const Pagination = ({ currentPage = 0, totalPages = 1, onPageChange, totalElements }) => {
  if (totalPages <= 1 && !totalElements) return null;

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 16px',
        borderTop: '1px solid #e2e8f0',
        backgroundColor: '#fafafa',
        fontSize: '13px',
        color: '#64748b'
      }}
    >
      <div>
        {totalElements !== undefined && (
          <span>
            Total: <strong>{totalElements}</strong> items
          </span>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 0}
          style={{
            padding: '6px 12px',
            borderRadius: '6px',
            border: '1px solid #cbd5e1',
            backgroundColor: currentPage <= 0 ? '#f1f5f9' : '#fff',
            color: currentPage <= 0 ? '#94a3b8' : '#334155',
            cursor: currentPage <= 0 ? 'not-allowed' : 'pointer',
            fontSize: '12px',
            fontWeight: 500
          }}
        >
          &larr; Previous
        </button>

        <span style={{ padding: '0 8px', fontWeight: 600, color: '#1e293b' }}>
          Page {currentPage + 1} of {Math.max(totalPages, 1)}
        </span>

        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          style={{
            padding: '6px 12px',
            borderRadius: '6px',
            border: '1px solid #cbd5e1',
            backgroundColor: currentPage >= totalPages - 1 ? '#f1f5f9' : '#fff',
            color: currentPage >= totalPages - 1 ? '#94a3b8' : '#334155',
            cursor: currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer',
            fontSize: '12px',
            fontWeight: 500
          }}
        >
          Next &rarr;
        </button>
      </div>
    </div>
  );
};

export default Pagination;

