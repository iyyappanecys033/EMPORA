import React from 'react';

const Pagination = ({ currentPage = 0, totalPages = 1, onPageChange, totalElements }) => {
  if (totalPages <= 1 && !totalElements) return null;

  return (
    <nav
      aria-label="Pagination"
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '10px',
        padding: '12px 16px',
        borderTop: '1px solid var(--ep-border)',
        backgroundColor: 'var(--ep-bg-subtle)',
        fontSize: '13px',
        color: 'var(--ep-text-muted)'
      }}
    >
      <div>
        {totalElements !== undefined && (
          <span className="ep-tabular">
            Total: <strong style={{ color: 'var(--ep-text-primary)' }}>{totalElements}</strong> items
          </span>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 0}
          className="ep-btn ep-btn--secondary ep-btn--sm"
        >
          &larr; Previous
        </button>

        <span
          className="ep-tabular"
          style={{ padding: '0 8px', fontWeight: 600, color: 'var(--ep-text-primary)', fontSize: '12px' }}
        >
          Page {currentPage + 1} of {Math.max(totalPages, 1)}
        </span>

        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="ep-btn ep-btn--secondary ep-btn--sm"
        >
          Next &rarr;
        </button>
      </div>
    </nav>
  );
};

export default Pagination;
