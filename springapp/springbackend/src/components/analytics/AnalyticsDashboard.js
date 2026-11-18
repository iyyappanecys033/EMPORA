import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '../../services/AuthContext';
import analyticsService from '../../services/analyticsService';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import Alert from '../common/Alert';

const PRIORITY_BAR_COLORS = {
  LOW: '#16a34a',
  MEDIUM: '#2563eb',
  HIGH: '#ea580c',
  URGENT: '#e11d48',
  CRITICAL: '#991b1b'
};

const formatDateInput = (dateObj) => {
  const y = dateObj.getFullYear();
  const m = String(dateObj.getMonth() + 1).padStart(2, '0');
  const d = String(dateObj.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const getDefaultRange = (daysBack = 29) => {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - daysBack);
  return { startDate: formatDateInput(start), endDate: formatDateInput(end) };
};

const AnalyticsDashboard = () => {
  const { isAdmin, isAuditor, department: userDepartment } = useAuth();
  const canSelectAnyDepartment = isAdmin || isAuditor;

  const initialRange = useMemo(() => getDefaultRange(29), []);
  const [startDate, setStartDate] = useState(initialRange.startDate);
  const [endDate, setEndDate] = useState(initialRange.endDate);
  const [departmentFilter, setDepartmentFilter] = useState(canSelectAnyDepartment ? 'ALL' : userDepartment || '');
  const [priorityFilter, setPriorityFilter] = useState('');

  const [bundle, setBundle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [alert, setAlert] = useState(null);

  const fetchAnalytics = useCallback(async () => {
    setLoading(true);
    setAlert(null);
    try {
      const params = { startDate, endDate };
      if (canSelectAnyDepartment && departmentFilter && departmentFilter !== 'ALL') {
        params.department = departmentFilter;
      }
      if (priorityFilter) {
        params.priority = priorityFilter;
      }
      const data = await analyticsService.getDashboardBundle(params);
      setBundle(data);
    } catch (err) {
      setAlert(err);
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate, departmentFilter, priorityFilter, canSelectAnyDepartment]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  const applyPreset = (daysBack) => {
    const next = getDefaultRange(daysBack);
    setStartDate(next.startDate);
    setEndDate(next.endDate);
  };

  const overview = bundle?.overview;
  const statusDist = bundle?.statusDistribution || [];
  const priorityDist = bundle?.priorityDistribution || [];
  const trends = bundle?.trends || [];
  const deptPerf = bundle?.departmentPerformance || [];
  const popularity = bundle?.servicePopularity || [];
  const sla = bundle?.slaCompliance;
  const approvals = bundle?.approvalsOverview;

  const maxTrendVal = Math.max(
    1,
    ...trends.map((t) => Math.max(t.submittedCount || 0, t.resolvedCount || 0))
  );
  const maxPopVal = Math.max(1, ...popularity.map((p) => p.requestCount || 0));

  // SVG gauge geometry for SLA compliance ring
  const slaPct = sla?.compliancePercentage;
  const ringRadius = 42;
  const ringCircumference = 2 * Math.PI * ringRadius;
  const ringOffset =
    slaPct !== null && slaPct !== undefined
      ? ringCircumference * (1 - Math.min(100, Math.max(0, slaPct)) / 100)
      : ringCircumference;

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px', marginBottom: '20px' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 700, margin: '0 0 4px', color: '#0f172a' }}>
            Operational Analytics & Service Insights
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: '#64748b' }}>
            Backend-authoritative SLA compliance, backlog metrics, and workload trends
            {overview?.timezone ? ` (${overview.timezone})` : ' (Asia/Kolkata)'}
            {overview?.scopedDepartment ? ` • Scope: ${overview.scopedDepartment}` : ''}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '6px' }}>
          <button type="button" onClick={() => applyPreset(6)} style={presetBtnStyle}>7D</button>
          <button type="button" onClick={() => applyPreset(29)} style={presetBtnStyle}>30D</button>
          <button type="button" onClick={() => applyPreset(89)} style={presetBtnStyle}>90D</button>
          <button
            type="button"
            onClick={fetchAnalytics}
            style={{ ...presetBtnStyle, backgroundColor: '#2563eb', color: '#fff', borderColor: '#2563eb' }}
          >
            🔄 Refresh
          </button>
        </div>
      </div>

      {/* Filter Controls */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
          gap: '12px',
          backgroundColor: '#fff',
          padding: '14px 16px',
          borderRadius: '8px',
          border: '1px solid #e2e8f0',
          marginBottom: '20px'
        }}
      >
        <div>
          <label htmlFor="analyticsStartDate" style={filterLabelStyle}>Start Date</label>
          <input
            id="analyticsStartDate"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            style={filterInputStyle}
          />
        </div>
        <div>
          <label htmlFor="analyticsEndDate" style={filterLabelStyle}>End Date</label>
          <input
            id="analyticsEndDate"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            style={filterInputStyle}
          />
        </div>
        <div>
          <label htmlFor="analyticsDepartment" style={filterLabelStyle}>Department Scope</label>
          {canSelectAnyDepartment ? (
            <select
              id="analyticsDepartment"
              value={departmentFilter}
              onChange={(e) => setDepartmentFilter(e.target.value)}
              style={filterInputStyle}
            >
              <option value="ALL">All Departments</option>
              <option value="IT">IT</option>
              <option value="HR">HR</option>
              <option value="Facilities">Facilities</option>
              <option value="Finance">Finance</option>
              <option value="Engineering">Engineering</option>
            </select>
          ) : (
            <input
              id="analyticsDepartment"
              type="text"
              value={userDepartment || 'Assigned Department'}
              readOnly
              disabled
              style={{ ...filterInputStyle, backgroundColor: '#f1f5f9', color: '#475569' }}
            />
          )}
        </div>
        <div>
          <label htmlFor="analyticsPriority" style={filterLabelStyle}>Priority</label>
          <select
            id="analyticsPriority"
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
            style={filterInputStyle}
          >
            <option value="">All Priorities</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>
      </div>

      {alert && (
        <Alert
          type="error"
          title={alert.title || 'Analytics Error'}
          detail={alert.detail}
          errors={alert.errors}
          onClose={() => setAlert(null)}
        />
      )}

      {loading ? (
        <LoadingSpinner message="Aggregating operational analytics from backend..." />
      ) : !overview ? (
        <EmptyState
          icon="📊"
          title="No analytics data available"
          description="Adjust your date range or filters to inspect operational metrics."
        />
      ) : (
        <>
          {/* Summary KPI Cards */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(175px, 1fr))',
              gap: '14px',
              marginBottom: '24px'
            }}
          >
            <KpiCard label="Submitted (Period)" value={overview.periodTotalTickets} subtext={`${overview.startDate} to ${overview.endDate}`} color="#0f172a" />
            <KpiCard label="Open Backlog" value={overview.openTicketsSnapshot} subtext="Current live snapshot" color="#2563eb" />
            <KpiCard label="Resolved / Closed" value={overview.resolvedTicketsInPeriod} subtext={`${overview.closedTicketsInPeriod} closed in period`} color="#16a34a" />
            <KpiCard label="Overdue Open" value={overview.overdueOpenTicketsSnapshot} subtext="Active clock past SLA" color={overview.overdueOpenTicketsSnapshot > 0 ? '#dc2626' : '#16a34a'} />
            <KpiCard
              label="SLA Compliance"
              value={overview.slaComplianceRate !== null && overview.slaComplianceRate !== undefined ? `${overview.slaComplianceRate}%` : 'N/A'}
              subtext={overview.averageResolutionHours !== null && overview.averageResolutionHours !== undefined ? `Avg Res: ${overview.averageResolutionHours}h` : 'No resolved tickets'}
              color="#0f766e"
            />
            <KpiCard label="Pending Approvals" value={overview.pendingApprovalsSnapshot} subtext={`${approvals?.overduePendingSnapshot || 0} overdue (>24h)`} color="#d97706" />
          </div>

          {/* Row 1: Volume Trend + SLA & Approvals */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px', marginBottom: '20px' }}>
            {/* Daily Ticket Volume Trend */}
            <section style={panelStyle} aria-label="Ticket volume trend chart">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h2 style={panelTitleStyle}>Ticket Volume Trend</h2>
                <div style={{ display: 'flex', gap: '12px', fontSize: '11px', color: '#475569' }}>
                  <span><span style={{ display: 'inline-block', width: '10px', height: '10px', backgroundColor: '#2563eb', borderRadius: '2px', marginRight: '4px' }} />Submitted</span>
                  <span><span style={{ display: 'inline-block', width: '10px', height: '10px', backgroundColor: '#16a34a', borderRadius: '2px', marginRight: '4px' }} />Resolved</span>
                </div>
              </div>
              {trends.length === 0 ? (
                <div style={emptyPanelStyle}>No trend data for selected window.</div>
              ) : (
                <div
                  role="img"
                  aria-label={`Daily ticket trend across ${trends.length} days`}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-end',
                    gap: '4px',
                    height: '165px',
                    paddingTop: '12px',
                    borderBottom: '1px solid #e2e8f0',
                    overflowX: 'auto'
                  }}
                >
                  {trends.slice(-31).map((pt) => {
                    const subH = Math.round(((pt.submittedCount || 0) / maxTrendVal) * 130);
                    const resH = Math.round(((pt.resolvedCount || 0) / maxTrendVal) * 130);
                    return (
                      <div
                        key={pt.date}
                        title={`${pt.date}: ${pt.submittedCount} submitted, ${pt.resolvedCount} resolved`}
                        style={{ flex: '1 0 14px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '2px' }}
                      >
                        <div style={{ display: 'flex', alignItems: 'flex-end', gap: '2px', height: '134px' }}>
                          <div style={{ width: '6px', height: `${Math.max(pt.submittedCount > 0 ? 4 : 0, subH)}px`, backgroundColor: '#2563eb', borderRadius: '2px 2px 0 0' }} />
                          <div style={{ width: '6px', height: `${Math.max(pt.resolvedCount > 0 ? 4 : 0, resH)}px`, backgroundColor: '#16a34a', borderRadius: '2px 2px 0 0' }} />
                        </div>
                        <span style={{ fontSize: '9px', color: '#94a3b8' }}>
                          {String(pt.date).slice(5)}
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>

            {/* SLA Compliance Gauge & Approval Pipeline */}
            <section style={panelStyle} aria-label="SLA compliance and approval overview">
              <h2 style={{ ...panelTitleStyle, marginBottom: '14px' }}>SLA Performance & Approval Pipeline</h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '20px', flexWrap: 'wrap' }}>
                <svg width="110" height="110" viewBox="0 0 110 110" role="img" aria-label={`SLA compliance ${slaPct ?? 'not available'} percent`}>
                  <circle cx="55" cy="55" r={ringRadius} fill="none" stroke="#e2e8f0" strokeWidth="10" />
                  <circle
                    cx="55"
                    cy="55"
                    r={ringRadius}
                    fill="none"
                    stroke={slaPct === null || slaPct === undefined ? '#94a3b8' : slaPct >= 90 ? '#16a34a' : slaPct >= 75 ? '#d97706' : '#dc2626'}
                    strokeWidth="10"
                    strokeDasharray={ringCircumference}
                    strokeDashoffset={ringOffset}
                    strokeLinecap="round"
                    transform="rotate(-90 55 55)"
                  />
                  <text x="55" y="59" textAnchor="middle" fontSize="16" fontWeight="700" fill="#0f172a">
                    {slaPct !== null && slaPct !== undefined ? `${slaPct}%` : 'N/A'}
                  </text>
                </svg>

                <div style={{ flex: 1, fontSize: '13px', color: '#334155', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                  <div><span style={{ color: '#64748b', display: 'block', fontSize: '11px' }}>Met SLA</span><strong>{sla?.metSlaCount ?? 0}</strong></div>
                  <div><span style={{ color: '#64748b', display: 'block', fontSize: '11px' }}>Breached SLA</span><strong style={{ color: '#dc2626' }}>{sla?.breachedSlaCount ?? 0}</strong></div>
                  <div><span style={{ color: '#64748b', display: 'block', fontSize: '11px' }}>Stage 1 Pending</span><strong>{approvals?.stage1PendingSnapshot ?? 0}</strong></div>
                  <div><span style={{ color: '#64748b', display: 'block', fontSize: '11px' }}>Stage 2 Pending</span><strong>{approvals?.stage2PendingSnapshot ?? 0}</strong></div>
                  <div><span style={{ color: '#64748b', display: 'block', fontSize: '11px' }}>Approved (Period)</span><strong style={{ color: '#16a34a' }}>{approvals?.approvedInPeriod ?? 0}</strong></div>
                  <div><span style={{ color: '#64748b', display: 'block', fontSize: '11px' }}>Rejected (Period)</span><strong style={{ color: '#dc2626' }}>{approvals?.rejectedInPeriod ?? 0}</strong></div>
                </div>
              </div>
              <div style={{ marginTop: '12px', fontSize: '11px', color: '#64748b', borderTop: '1px solid #f1f5f9', paddingTop: '8px' }}>
                SLA excludes CANCELLED/REJECTED tickets and subtracts ON_HOLD paused minutes per Asia/Kolkata business calendar.
              </div>
            </section>
          </div>

          {/* Row 2: Status Distribution & Priority Distribution */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px', marginBottom: '20px' }}>
            <section style={panelStyle} aria-label="Ticket status distribution">
              <h2 style={{ ...panelTitleStyle, marginBottom: '12px' }}>Ticket Status Distribution</h2>
              {statusDist.length === 0 ? (
                <div style={emptyPanelStyle}>No tickets submitted in this period.</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {statusDist.map((item) => (
                    <div key={item.status}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '4px' }}>
                        <span style={{ fontWeight: 600, color: '#1e293b' }}>{item.status.replace(/_/g, ' ')}</span>
                        <span style={{ color: '#475569' }}>{item.count} ({item.percentage}%)</span>
                      </div>
                      <div style={{ height: '8px', backgroundColor: '#f1f5f9', borderRadius: '4px', overflow: 'hidden' }}>
                        <div style={{ width: `${item.percentage}%`, height: '100%', backgroundColor: '#2563eb' }} />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section style={panelStyle} aria-label="Ticket priority distribution">
              <h2 style={{ ...panelTitleStyle, marginBottom: '12px' }}>Priority Distribution (All 5 Tiers)</h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {priorityDist.map((item) => (
                  <div key={item.priority}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '4px' }}>
                      <span style={{ fontWeight: 600, color: '#1e293b' }}>{item.priority}</span>
                      <span style={{ color: '#475569' }}>{item.count} ({item.percentage}%)</span>
                    </div>
                    <div style={{ height: '8px', backgroundColor: '#f1f5f9', borderRadius: '4px', overflow: 'hidden' }}>
                      <div
                        style={{
                          width: `${item.percentage}%`,
                          height: '100%',
                          backgroundColor: PRIORITY_BAR_COLORS[item.priority] || '#475569'
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>

          {/* Row 3: Department Performance Table & Service Popularity */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
            <section style={panelStyle} aria-label="Department workload and backlog">
              <h2 style={{ ...panelTitleStyle, marginBottom: '12px' }}>Department Workload & Backlog</h2>
              {deptPerf.length === 0 ? (
                <div style={emptyPanelStyle}>No department activity recorded in this window.</div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid #e2e8f0', color: '#64748b' }}>
                        <th style={{ padding: '8px' }}>Department</th>
                        <th style={{ padding: '8px' }}>Submitted</th>
                        <th style={{ padding: '8px' }}>Open Backlog</th>
                        <th style={{ padding: '8px' }}>Overdue</th>
                        <th style={{ padding: '8px' }}>SLA %</th>
                      </tr>
                    </thead>
                    <tbody>
                      {deptPerf.map((d) => (
                        <tr key={d.department} style={{ borderBottom: '1px solid #f1f5f9' }}>
                          <td style={{ padding: '8px', fontWeight: 700, color: '#0f172a' }}>{d.department}</td>
                          <td style={{ padding: '8px' }}>{d.totalSubmittedInPeriod}</td>
                          <td style={{ padding: '8px', fontWeight: 600, color: '#2563eb' }}>{d.openBacklogSnapshot}</td>
                          <td style={{ padding: '8px', color: d.overdueOpenSnapshot > 0 ? '#dc2626' : '#475569' }}>
                            {d.overdueOpenSnapshot}
                          </td>
                          <td style={{ padding: '8px', fontWeight: 600 }}>
                            {d.slaComplianceRate !== null && d.slaComplianceRate !== undefined ? `${d.slaComplianceRate}%` : 'N/A'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section style={panelStyle} aria-label="Most requested catalog services">
              <h2 style={{ ...panelTitleStyle, marginBottom: '12px' }}>Top Requested Services</h2>
              {popularity.length === 0 ? (
                <div style={emptyPanelStyle}>No service requests recorded in this period.</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {popularity.map((svc) => (
                    <div key={svc.catalogItemId}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '4px' }}>
                        <span style={{ fontWeight: 600, color: '#0f172a' }}>
                          {svc.title} <span style={{ color: '#94a3b8', fontWeight: 400 }}>({svc.department})</span>
                        </span>
                        <span style={{ fontWeight: 700, color: '#2563eb' }}>{svc.requestCount}</span>
                      </div>
                      <div style={{ height: '7px', backgroundColor: '#f1f5f9', borderRadius: '4px', overflow: 'hidden' }}>
                        <div
                          style={{
                            width: `${Math.round((svc.requestCount / maxPopVal) * 100)}%`,
                            height: '100%',
                            backgroundColor: '#0f766e'
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>
        </>
      )}
    </div>
  );
};

const KpiCard = ({ label, value, subtext, color }) => (
  <div
    style={{
      backgroundColor: '#fff',
      borderRadius: '10px',
      padding: '16px 18px',
      border: '1px solid #e2e8f0',
      boxShadow: '0 1px 2px rgba(0,0,0,0.03)'
    }}
  >
    <div style={{ fontSize: '11px', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
      {label}
    </div>
    <div style={{ fontSize: '26px', fontWeight: 800, color, margin: '6px 0 4px' }}>
      {value}
    </div>
    <div style={{ fontSize: '11px', color: '#94a3b8' }}>{subtext}</div>
  </div>
);

const panelStyle = {
  backgroundColor: '#fff',
  borderRadius: '10px',
  padding: '18px 20px',
  border: '1px solid #e2e8f0',
  boxShadow: '0 1px 2px rgba(0,0,0,0.03)'
};

const panelTitleStyle = {
  margin: 0,
  fontSize: '15px',
  fontWeight: 700,
  color: '#0f172a'
};

const emptyPanelStyle = {
  padding: '28px 12px',
  textAlign: 'center',
  fontSize: '13px',
  color: '#94a3b8'
};

const filterLabelStyle = {
  display: 'block',
  fontSize: '11px',
  fontWeight: 600,
  color: '#475569',
  marginBottom: '4px'
};

const filterInputStyle = {
  width: '100%',
  padding: '7px 10px',
  borderRadius: '6px',
  border: '1px solid #cbd5e1',
  fontSize: '13px',
  boxSizing: 'border-box',
  backgroundColor: '#fff'
};

const presetBtnStyle = {
  padding: '6px 12px',
  borderRadius: '6px',
  border: '1px solid #cbd5e1',
  backgroundColor: '#fff',
  color: '#334155',
  fontSize: '12px',
  fontWeight: 600,
  cursor: 'pointer'
};

export default AnalyticsDashboard;

