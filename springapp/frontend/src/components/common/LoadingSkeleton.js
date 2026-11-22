import React from 'react';

export const SkeletonBlock = ({ width = '100%', height = '16px', style = {} }) => (
  <div
    className="ep-skeleton"
    aria-hidden="true"
    style={{ width, height, ...style }}
  />
);

export const DashboardSkeleton = () => (
  <div role="status" aria-busy="true" aria-label="Loading workspace">
    <SkeletonBlock height="110px" style={{ borderRadius: '14px', marginBottom: '22px' }} />
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="ep-card">
          <SkeletonBlock width="45%" height="12px" />
          <SkeletonBlock width="35%" height="28px" style={{ marginTop: '12px' }} />
          <SkeletonBlock width="65%" height="11px" style={{ marginTop: '8px' }} />
        </div>
      ))}
    </div>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
      {[1, 2].map((col) => (
        <div key={col} className="ep-card">
          <SkeletonBlock width="40%" height="18px" style={{ marginBottom: '16px' }} />
          {[1, 2, 3].map((r) => (
            <SkeletonBlock key={r} height="56px" style={{ marginBottom: '10px', borderRadius: '8px' }} />
          ))}
        </div>
      ))}
    </div>
  </div>
);

export const TableSkeleton = ({ rows = 5 }) => (
  <div className="ep-card" role="status" aria-busy="true" aria-label="Loading table">
    <SkeletonBlock height="36px" style={{ marginBottom: '14px' }} />
    {Array.from({ length: rows }).map((_, i) => (
      <SkeletonBlock key={i} height="42px" style={{ marginBottom: '8px' }} />
    ))}
  </div>
);

export const CardGridSkeleton = ({ count = 6 }) => (
  <div
    role="status"
    aria-busy="true"
    aria-label="Loading catalog items"
    style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))', gap: '18px' }}
  >
    {Array.from({ length: count }).map((_, i) => (
      <div key={i} className="ep-card">
        <SkeletonBlock width="30%" height="18px" />
        <SkeletonBlock width="75%" height="20px" style={{ marginTop: '12px' }} />
        <SkeletonBlock width="100%" height="36px" style={{ marginTop: '10px' }} />
        <SkeletonBlock width="100%" height="34px" style={{ marginTop: '18px' }} />
      </div>
    ))}
  </div>
);

export default SkeletonBlock;
