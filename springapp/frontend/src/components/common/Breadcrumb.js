import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const LABELS = {
  catalog: 'Service Catalog',
  tickets: 'My Tickets',
  notifications: 'Notifications',
  analytics: 'Operational Analytics',
  approvals: 'Approval Center',
  queue: 'Department Queue',
  'audit-chain': 'Cryptographic Audit Ledger',
  profile: 'My Profile',
  preferences: 'Preferences',
  admin: 'Administration',
  metrics: 'System Metrics',
  users: 'Users',
  roles: 'Roles',
  profiles: 'Employee Profiles',
  'user-role-mappings': 'Role Mappings',
  audit: 'Legacy Audit Logs',
  new: 'Create New',
  edit: 'Edit'
};

const Breadcrumb = () => {
  const location = useLocation();
  const segments = location.pathname.split('/').filter(Boolean);

  if (segments.length === 0) return null;

  let cumulativePath = '';
  return (
    <nav aria-label="Breadcrumb" className="ep-breadcrumb">
      <Link to="/">Home</Link>
      {segments.map((seg, idx) => {
        cumulativePath += `/${seg}`;
        const isLast = idx === segments.length - 1;
        const label = LABELS[seg] || (/^\d+$/.test(seg) ? `#${seg}` : decodeURIComponent(seg));
        return (
          <React.Fragment key={cumulativePath}>
            <span aria-hidden="true">/</span>
            {isLast ? (
              <span aria-current="page" style={{ fontWeight: 600, color: 'var(--ep-text-primary)' }}>
                {label}
              </span>
            ) : (
              <Link to={cumulativePath}>{label}</Link>
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
};

export default Breadcrumb;
