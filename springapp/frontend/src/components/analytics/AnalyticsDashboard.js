import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '../../services/AuthContext';
import analyticsService from '../../services/analyticsService';
import { DashboardSkeleton } from '../common/LoadingSkeleton';
import EmptyState from '../common/EmptyState';
import Alert from '../common/Alert';
import { ChartIcon, RefreshIcon } from '../common/Icons';

const PRIORITY_BAR_COLORS = {
  LOW: 'var(--ep-success)',
  MEDIUM: 'var(--ep-primary)',
  HIGH: 'var(--ep-warning)',
  URGENT: '#e11d48',
  CRITICAL: 'var(--ep-danger)'
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
  const { isAdmin, isAuditor, department: userDepartment, loading: authLoading } = useAuth();
  const canSelectAnyDepartment = isAdmin || isAuditor;

  const initialRange = useMemo(() => getDefaultRange(29), []);
  const [startDate, setStartDate] = useState(initialRange.startDate);
  const [endDate, setEndDate] = useState(initialRange.endDate);
  const [departmentFilter, setDepartmentFilter] = useState('ALL');
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
    if (!authLoading) {
      fetchAnalytics();
    }
  }, [authLoading, fetchAnalytics]);

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

  const maxTrendVal = Math.max(1, ...trends.map((t) => Math.max(t.submittedCount || 0, t.resolvedCount || 0)));
  const maxPopVal = Math.max(1, ...popularity.map((p) => p.requestCount || 0));

  const slaPct = sla?.compliancePercentage;
  const ringRadius = 42;
  const ringCircumference = 2 * Math.PI * ringRadius;
  const ringOffset =
    slaPct !== null && slaPct !== undefined
      ? ringCircumference * (1 - Math.min(100, Math.max(0, slaPct)) / 100)
      : ringCircumference;

  return (
    <div>
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">Operational Analytics &amp; Service Insights</h1>
          <p className="ep-page-subtitle">
            Backend-authoritative SLA compliance, backlog metrics, and workload trends
            {overview?.timezone ? ` (${overview.timezone})` : ' (Asia/Kolkata)'}
            {overview?.scopedDepartment ? ` • Scope: ${overview.scopedDepartment}` : ''}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
          <button type="button" onClick={() => applyPreset(6)} className="ep-btn ep-btn--secondary ep-btn--sm">7D</button>
          <button type="button" onClick={() => applyPreset(29)} className="ep-btn ep-btn--secondary ep-btn--sm">30D</button>
          <button type="button" onClick={() => applyPreset(89)} className="ep-btn ep-btn--secondary ep-btn--sm">90D</button>
          <button type="button" onClick={fetchAnalytics} className="ep-btn ep-btn--primary ep-btn--sm">
            <RefreshIcon size={14} /> Refresh
          </button>
        </div>
      </div>

      {/* Filter Controls */}
      <div
        className="ep-card"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
          gap: '12px',
          padding: '14px 16px',
          marginBottom: '20px'
        }}
      >
        <div>
          <label htmlFor="analyticsStartDate" className="ep-label">Start Date</label>
          <input id="analyticsStartDate" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="ep-input" />
        </div>
        <div>
          <label htmlFor="analyticsEndDate" className="ep-label">End Date</label>
          <input id="analyticsEndDate" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="ep-input" />
        </div>
        <div>
          <label htmlFor="analyticsDepartment" className="ep-label">Department Scope</label>
          {canSelectAnyDepartment ? (
            <select id="analyticsDepartment" value={departmentFilter} onChange={(e) => setDepartmentFilter(e.target.value)} className="ep-select">
              <option value="ALL">All Departments</option>
              <option value="IT">IT</option>
              <option value="HR">HR</option>
              <option value="Facilities">Facilities</option>
              <option value="Finance">Finance</option>
              <option value="Engineering">Engineering</option>
            </select>
          ) : (
            <input id="analyticsDepartment" type="text" value={userDepartment || 'Assigned Department'} readOnly disabled className="ep-input" />
          )}
        </div>
        <div>
          <label htmlFor="analyticsPriority" className="ep-label">Priority</label>
          <select id="analyticsPriority" value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)} className="ep-select">
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
        <Alert type="error" title={alert.title || 'Analytics Error'} detail={alert.detail} errors={alert.errors} onClose={() => setAlert(null)} />
      )}

      {loading ? (
        <DashboardSkeleton />
      ) : !overview ? (
        <EmptyState
          icon={<ChartIcon size={26} />}
          title="No analytics data available"
          description="Adjust your date range or filters to inspect operational metrics."
        />
      ) : (
        <>
          {/* Summary KPI Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(175px, 1fr))', gap: '14px', marginBottom: '22px' }}>
            <KpiCard label="Submitted (Period)" value={overview.periodTotalTickets} subtext={`${overview.startDate} to ${overview.endDate}`} accent="var(--ep-text-primary)" />
            <KpiCard label="Open Backlog" value={overview.openTicketsSnapshot} subtext="Current live snapshot" accent="var(--ep-primary)" />
            <KpiCard label="Resolved / Closed" value={overview.resolvedTicketsInPeriod} subtext={`${overview.closedTicketsInPeriod} closed in period`} accent="var(--ep-success)" />
            <KpiCard label="Overdue Open" value={overview.overdueOpenTicketsSnapshot} subtext="Active clock past SLA" accent={overview.overdueOpenTicketsSnapshot > 0 ? 'var(--ep-danger)' : 'var(--ep-success)'} />
            <KpiCard
              label="SLA Compliance"
              value={overview.slaComplianceRate !== null && overview.slaComplianceRate !== undefined ? `${overview.slaComplianceRate}%` : 'N/A'}
              subtext={overview.averageResolutionHours !== null && overview.averageResolutionHours !== undefined ? `Avg Res: ${overview.averageResolutionHours}h` : 'No resolved tickets'}
              accent="var(--ep-secondary)"
            />
            <KpiCard label="Pending Approvals" value={overview.pendingApprovalsSnapshot} subtext={`${approvals?.overduePendingSnapshot || 0} overdue (>24h)`} accent="var(--ep-warning)" />
          </div>

          {/* Row 1: Volume Trend + SLA & Approvals */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))', gap: '20px', marginBottom: '20px' }}>
            <section className="ep-card" aria-label="Ticket volume trend chart">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', gap: '8px', flexWrap: 'wrap' }}>
                <h2 style={panelTitleStyle}>Ticket Volume Trend</h2>
                <div style={{ display: 'flex', gap: '12px', fontSize: '11px', color: 'var(--ep-text-muted)' }}>
                  <span><span style={{ display: 'inline-block', width: '10px', height: '10px', backgroundColor: 'var(--ep-primary)', borderRadius: '2px', marginRight: '4px' }} />Submitted</span>
                  <span><span style={{ display: 'inline-block', width: '10px', height: '10px', backgroundColor: 'var(--ep-success)', borderRadius: '2px', marginRight: '4px' }} />Resolved</span>
                </div>
              </div>
              {trends.length === 0 ? (
                <div style={emptyPanelStyle}>No trend data for selected window.</div>
              ) : (
                <figure style={{ margin: 0 }}>
                  <div
                    role="img"
                    tabIndex={0}
                    aria-label={`Daily ticket volume trend across ${trends.length} days, peak daily volume ${maxTrendVal}`}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-end',
                      gap: '4px',
                      height: '170px',
                      padding: '12px 12px 4px',
                      borderBottom: '1px solid var(--ep-border)',
                      overflowX: 'auto'
                    }}
                  >
                    {(() => {
                      const sliced = trends.slice(-31);
                      const showStep = sliced.length > 12 ? 5 : 1;
                      return sliced.map((pt, idx) => {
                        const subH = Math.round(((pt.submittedCount || 0) / maxTrendVal) * 126);
                        const resH = Math.round(((pt.resolvedCount || 0) / maxTrendVal) * 126);
                        const showLabel = showStep === 1 || idx === 0 || idx === sliced.length - 1 || idx % showStep === 0;
                        return (
                          <div
                            key={pt.date}
                            title={`${pt.date}: ${pt.submittedCount} submitted, ${pt.resolvedCount} resolved`}
                            style={{ flex: '1 1 12px', minWidth: '10px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}
                          >
                            <div style={{ display: 'flex', alignItems: 'flex-end', gap: '2px', height: '130px' }}>
                              <div style={{ width: '5px', height: `${Math.max(pt.submittedCount > 0 ? 4 : 0, subH)}px`, backgroundColor: 'var(--ep-primary)', borderRadius: '2px 2px 0 0' }} />
                              <div style={{ width: '5px', height: `${Math.max(pt.resolvedCount > 0 ? 4 : 0, resH)}px`, backgroundColor: 'var(--ep-success)', borderRadius: '2px 2px 0 0' }} />
                            </div>
                            <span style={{ fontSize: '9px', color: 'var(--ep-text-muted)', whiteSpace: 'nowrap', minHeight: '12px' }}>
                              {showLabel ? String(pt.date).slice(5) : ''}
                            </span>
                          </div>
                        );
                      });
                    })()}
                  </div>
                  <figcaption className="sr-only">
                    Showing submitted and resolved ticket counts across {trends.length} days from {overview.startDate} to {overview.endDate}.
                  </figcaption>
                </figure>
              )}
            </section>

            <section className="ep-card" aria-label="SLA compliance and approval overview">
              <h2 style={{ ...panelTitleStyle, marginBottom: '14px' }}>SLA Performance &amp; Approval Pipeline</h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '20px', flexWrap: 'wrap' }}>
                <svg width="110" height="110" viewBox="0 0 110 110" role="img" aria-label={`SLA compliance ${slaPct ?? 'not available'} percent`}>
                  <circle cx="55" cy="55" r={ringRadius} fill="none" stroke="var(--ep-border)" strokeWidth="10" />
                  <circle
                    cx="55"
                    cy="55"
                    r={ringRadius}
                    fill="none"
                    stroke={slaPct === null || slaPct === undefined ? 'var(--ep-text-muted)' : slaPct >= 90 ? 'var(--ep-success)' : slaPct >= 75 ? 'var(--ep-warning)' : 'var(--ep-danger)'}
                    strokeWidth="10"
                    strokeDasharray={ringCircumference}
                    strokeDashoffset={ringOffset}
                    strokeLinecap="round"
                    transform="rotate(-90 55 55)"
                  />
                  <text x="55" y="59" textAnchor="middle" fontSize="16" fontWeight="700" fill="var(--ep-text-primary)">
                    {slaPct !== null && slaPct !== undefined ? `${slaPct}%` : 'N/A'}
                  </text>
                </svg>

                <div style={{ flex: 1, fontSize: '13px', color: 'var(--ep-text-primary)', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div><span style={statMetaStyle}>Met SLA</span><strong className="ep-tabular">{sla?.metSlaCount ?? 0}</strong></div>
                  <div><span style={statMetaStyle}>Breached SLA</span><strong className="ep-tabular" style={{ color: 'var(--ep-danger)' }}>{sla?.breachedSlaCount ?? 0}</strong></div>
                  <div><span style={statMetaStyle}>Stage 1 Pending</span><strong className="ep-tabular">{approvals?.stage1PendingSnapshot ?? 0}</strong></div>
                  <div><span style={statMetaStyle}>Stage 2 Pending</span><strong className="ep-tabular">{approvals?.stage2PendingSnapshot ?? 0}</strong></div>
                  <div><span style={statMetaStyle}>Approved (Period)</span><strong className="ep-tabular" style={{ color: 'var(--ep-success)' }}>{approvals?.approvedInPeriod ?? 0}</strong></div>
                  <div><span style={statMetaStyle}>Rejected (Period)</span><strong className="ep-tabular" style={{ color: 'var(--ep-danger)' }}>{approvals?.rejectedInPeriod ?? 0}</strong></div>
                </div>
              </div>
              <div style={{ marginTop: '12px', fontSize: '11px', color: 'var(--ep-text-muted)', borderTop: '1px solid var(--ep-border-subtle)', paddingTop: '8px' }}>
                SLA excludes CANCELLED/REJECTED tickets and subtracts ON_HOLD paused minutes per Asia/Kolkata business calendar.
              </div>
            </section>
          </div>

          {/* Row 2: Distributions */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px', marginBottom: '20px' }}>
            <section className="ep-card" aria-label="Ticket status distribution">
              <h2 style={{ ...panelTitleStyle, marginBottom: '12px' }}>Ticket Status Distribution</h2>
              {statusDist.length === 0 ? (
                <div style={emptyPanelStyle}>No tickets submitted in this period.</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {statusDist.map((item) => (
                    <div key={item.status}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '4px' }}>
                        <span style={{ fontWeight: 600, color: 'var(--ep-text-primary)' }}>{item.status.replace(/_/g, ' ')}</span>
                        <span className="ep-tabular" style={{ color: 'var(--ep-text-muted)' }}>{item.count} ({item.percentage}%)</span>
                      </div>
                      <div style={{ height: '8px', backgroundColor: 'var(--ep-bg-muted)', borderRadius: '4px', overflow: 'hidden' }}>
                        <div style={{ width: `${item.percentage}%`, height: '100%', backgroundColor: 'var(--ep-primary)' }} />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section className="ep-card" aria-label="Ticket priority distribution">
              <h2 style={{ ...panelTitleStyle, marginBottom: '12px' }}>Priority Distribution (All 5 Tiers)</h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {priorityDist.map((item) => (
                  <div key={item.priority}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '4px' }}>
                      <span style={{ fontWeight: 600, color: 'var(--ep-text-primary)' }}>{item.priority}</span>
                      <span className="ep-tabular" style={{ color: 'var(--ep-text-muted)' }}>{item.count} ({item.percentage}%)</span>
                    </div>
                    <div style={{ height: '8px', backgroundColor: 'var(--ep-bg-muted)', borderRadius: '4px', overflow: 'hidden' }}>
                      <div style={{ width: `${item.percentage}%`, height: '100%', backgroundColor: PRIORITY_BAR_COLORS[item.priority] || 'var(--ep-primary)' }} />
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>

          {/* Row 3: Department Workload & Popularity */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))', gap: '20px' }}>
            <section className="ep-card" aria-label="Department workload and backlog">
              <h2 style={{ ...panelTitleStyle, marginBottom: '12px' }}>Department Workload &amp; Backlog</h2>
              {deptPerf.length === 0 ? (
                <div style={emptyPanelStyle}>No department activity recorded in this window.</div>
              ) : (
                <div style={{ overflowX: 'auto' }} tabIndex={0} role="region" aria-label="Department workload metrics table">
                  <table className="ep-table" style={{ fontSize: '12px' }}>
                    <thead>
                      <tr>
                        <th>Department</th>
                        <th>Submitted</th>
                        <th>Open Backlog</th>
                        <th>Overdue</th>
                        <th>SLA %</th>
                      </tr>
                    </thead>
                    <tbody>
                      {deptPerf.map((d) => (
                        <tr key={d.department}>
                          <td style={{ fontWeight: 700 }}>{d.department}</td>
                          <td className="ep-tabular">{d.totalSubmittedInPeriod}</td>
                          <td className="ep-tabular" style={{ fontWeight: 700, color: 'var(--ep-primary)' }}>{d.openBacklogSnapshot}</td>
                          <td className="ep-tabular" style={{ color: d.overdueOpenSnapshot > 0 ? 'var(--ep-danger)' : 'var(--ep-text-muted)' }}>
                            {d.overdueOpenSnapshot}
                          </td>
                          <td className="ep-tabular" style={{ fontWeight: 600 }}>
                            {d.slaComplianceRate !== null && d.slaComplianceRate !== undefined ? `${d.slaComplianceRate}%` : 'N/A'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section className="ep-card" aria-label="Most requested catalog services">
              <h2 style={{ ...panelTitleStyle, marginBottom: '12px' }}>Top Requested Services</h2>
              {popularity.length === 0 ? (
                <div style={emptyPanelStyle}>No service requests recorded in this period.</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {popularity.map((svc) => (
                    <div key={svc.catalogItemId}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '4px', gap: '8px' }}>
                        <span style={{ fontWeight: 600, color: 'var(--ep-text-primary)' }}>
                          {svc.title} <span style={{ color: 'var(--ep-text-muted)', fontWeight: 400 }}>({svc.department})</span>
                        </span>
                        <span className="ep-tabular" style={{ fontWeight: 700, color: 'var(--ep-primary)' }}>{svc.requestCount}</span>
                      </div>
                      <div style={{ height: '7px', backgroundColor: 'var(--ep-bg-muted)', borderRadius: '4px', overflow: 'hidden' }}>
                        <div style={{ width: `${Math.round((svc.requestCount / maxPopVal) * 100)}%`, height: '100%', backgroundColor: 'var(--ep-secondary)' }} />
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

const KpiCard = ({ label, value, subtext, accent }) => (
  <div className="ep-card" style={{ borderTop: `3px solid ${accent}` }}>
    <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
      {label}
    </div>
    <div className="ep-tabular" style={{ fontSize: '26px', fontWeight: 800, color: accent, margin: '6px 0 4px' }}>
      {value}
    </div>
    <div style={{ fontSize: '11px', color: 'var(--ep-text-muted)' }}>{subtext}</div>
  </div>
);

const panelTitleStyle = { margin: 0, fontSize: '15px', fontWeight: 700, color: 'var(--ep-text-primary)' };
const emptyPanelStyle = { padding: '28px 12px', textAlign: 'center', fontSize: '13px', color: 'var(--ep-text-muted)' };
const statMetaStyle = { color: 'var(--ep-text-muted)', display: 'block', fontSize: '11px' };

export default AnalyticsDashboard;
