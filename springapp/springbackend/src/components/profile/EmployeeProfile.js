import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';
import { useAuth } from '../../services/AuthContext';
import Avatar from '../common/Avatar';
import Alert from '../common/Alert';
import { SkeletonBlock } from '../common/LoadingSkeleton';
import { SettingsIcon, CatalogIcon, ShieldIcon } from '../common/Icons';

const EmployeeProfile = () => {
  const { currentUser, roles, isAdmin, hasRole } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  const canEditProfiles = isAdmin || hasRole('HR_MANAGER');

  useEffect(() => {
    let active = true;
    const loadProfile = async () => {
      setLoading(true);
      setNotice(null);
      try {
        const res = await api.get('/profiles/me');
        if (active) setProfile(res.data);
      } catch (err) {
        if (active) {
          if (err?.status === 404) {
            setNotice({
              type: 'info',
              title: 'HR Profile Record Not Linked Yet',
              detail: 'Displaying your core identity account details. Contact HR Operations to link an HR employee profile.'
            });
          } else {
            setNotice({
              type: 'error',
              title: err?.title || 'Unable to Load Full Profile',
              detail: err?.detail || 'Showing basic session identity details.'
            });
          }
        }
      } finally {
        if (active) setLoading(false);
      }
    };
    loadProfile();
    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return (
      <div className="ep-card" role="status" aria-busy="true" aria-label="Loading employee profile">
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', marginBottom: '20px' }}>
          <SkeletonBlock width="64px" height="64px" style={{ borderRadius: '50%' }} />
          <div style={{ flex: 1 }}>
            <SkeletonBlock width="220px" height="22px" />
            <SkeletonBlock width="160px" height="14px" style={{ marginTop: '8px' }} />
          </div>
        </div>
        <SkeletonBlock height="140px" />
      </div>
    );
  }

  const fullName =
    (profile?.firstName || profile?.lastName
      ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim()
      : null) ||
    currentUser?.fullName ||
    currentUser?.username ||
    'Employee';

  const department = profile?.department || currentUser?.department || 'Not assigned';
  const designation = profile?.designation || currentUser?.designation || 'Workspace Member';
  const employeeId = profile?.employeeId || currentUser?.employeeId || 'Pending Assignment';
  const workEmail = profile?.email || currentUser?.email || 'Not configured';
  const phone = profile?.phone || currentUser?.phone || 'Not configured';
  const accountStatus = profile?.status || currentUser?.status || 'ACTIVE';
  const effectiveRoles = Array.isArray(profile?.roles) && profile.roles.length > 0 ? profile.roles : roles;

  return (
    <div>
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">My Profile</h1>
          <p className="ep-page-subtitle">
            Your verified organizational identity, reporting structure, and account permissions.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <Link to="/preferences" className="ep-btn ep-btn--secondary">
            <SettingsIcon size={16} /> Preferences
          </Link>
          {canEditProfiles && profile?.id ? (
            <Link to={`/profiles/${profile.id}/edit`} className="ep-btn ep-btn--primary">
              Edit Profile Record
            </Link>
          ) : (
            <Link to="/catalog" className="ep-btn ep-btn--primary">
              <CatalogIcon size={16} /> Request Profile Update
            </Link>
          )}
        </div>
      </div>

      {notice && (
        <Alert
          type={notice.type}
          title={notice.title}
          detail={notice.detail}
          onClose={() => setNotice(null)}
        />
      )}

      {/* Identity Hero Card */}
      <div className="ep-card" style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '18px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <Avatar name={fullName} size={68} />
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: 800, color: 'var(--ep-text-primary)' }}>
                {fullName}
              </h2>
              <span
                style={{
                  fontSize: '11px',
                  fontWeight: 700,
                  padding: '2px 9px',
                  borderRadius: '9999px',
                  backgroundColor: accountStatus === 'ACTIVE' ? 'var(--ep-success-bg)' : 'var(--ep-warning-bg)',
                  color: accountStatus === 'ACTIVE' ? 'var(--ep-success-text)' : 'var(--ep-warning-text)',
                  border: `1px solid ${accountStatus === 'ACTIVE' ? 'var(--ep-success-border)' : 'var(--ep-warning-border)'}`
                }}
              >
                {accountStatus}
              </span>
            </div>
            <div style={{ fontSize: '13px', color: 'var(--ep-text-secondary)', marginTop: '4px' }}>
              {designation} • <strong>{department}</strong>
            </div>
            <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)', marginTop: '2px', fontFamily: 'var(--ep-font-mono)' }}>
              @{profile?.username || currentUser?.username} • ID: {employeeId}
            </div>
          </div>
        </div>

        <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)', maxWidth: '300px' }}>
          <div style={{ fontWeight: 600, color: 'var(--ep-text-secondary)', marginBottom: '4px' }}>
            Avatar Fallback Active
          </div>
          Profile photo upload is not enabled in the current backend API; your deterministic initials badge is displayed across EPAMS.
        </div>
      </div>

      {/* Two Column Details */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px', marginBottom: '20px' }}>
        <section className="ep-card" aria-labelledby="profile-org-heading">
          <h3 id="profile-org-heading" style={{ margin: '0 0 14px', fontSize: '15px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
            Organization &amp; Employment
          </h3>
          <dl style={{ margin: 0, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
            <Field label="Employee Identifier" value={employeeId} mono />
            <Field label="Department" value={department} />
            <Field label="Designation" value={designation} />
            <Field label="Reporting Manager" value={profile?.reportingManager || 'Not specified'} />
            <Field label="Date of Joining" value={profile?.dateOfJoining || 'Not recorded'} />
            <Field label="Account Status" value={accountStatus} />
          </dl>
        </section>

        <section className="ep-card" aria-labelledby="profile-contact-heading">
          <h3 id="profile-contact-heading" style={{ margin: '0 0 14px', fontSize: '15px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
            Contact &amp; Personal Details
          </h3>
          <dl style={{ margin: 0, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
            <Field label="Work Email" value={workEmail} />
            <Field label="Phone" value={phone} />
            <Field label="Emergency Contact" value={profile?.emergencyContact || 'Not specified'} />
            <Field label="Date of Birth" value={profile?.dob || 'Not recorded'} />
            <Field label="Address" value={profile?.address || 'Not specified'} fullWidth />
          </dl>
        </section>
      </div>

      <section className="ep-card" aria-labelledby="profile-roles-heading">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <ShieldIcon size={17} />
          <h3 id="profile-roles-heading" style={{ margin: 0, fontSize: '15px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
            Assigned Access Roles &amp; Governance
          </h3>
        </div>
        <div className="ep-pill-group" style={{ marginBottom: '10px' }}>
          {effectiveRoles.length === 0 ? (
            <span style={{ fontSize: '13px', color: 'var(--ep-text-muted)' }}>Standard Employee</span>
          ) : (
            effectiveRoles.map((r) => (
              <span
                key={r}
                style={{
                  fontSize: '12px',
                  fontWeight: 700,
                  padding: '4px 10px',
                  borderRadius: 'var(--ep-radius-full)',
                  background: 'var(--ep-primary-subtle)',
                  color: 'var(--ep-selected-text)',
                  border: '1px solid var(--ep-primary-border)'
                }}
              >
                {r.replace(/^ROLE_/, '')}
              </span>
            ))
          )}
        </div>
        <p style={{ margin: 0, fontSize: '12px', color: 'var(--ep-text-muted)' }}>
          Direct profile edits require HR Manager or System Administrator authorization (`PUT /api/profiles/employee`). Employees can raise an HR service request to update employment records.
        </p>
      </section>
    </div>
  );
};

const Field = ({ label, value, mono = false, fullWidth = false }) => (
  <div style={fullWidth ? { gridColumn: '1 / -1' } : undefined}>
    <dt style={{ fontSize: '11px', fontWeight: 600, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.03em' }}>
      {label}
    </dt>
    <dd
      style={{
        margin: '4px 0 0',
        fontSize: '13px',
        fontWeight: 600,
        color: 'var(--ep-text-primary)',
        fontFamily: mono ? 'var(--ep-font-mono)' : 'inherit',
        wordBreak: 'break-word'
      }}
    >
      {value}
    </dd>
  </div>
);

export default EmployeeProfile;
