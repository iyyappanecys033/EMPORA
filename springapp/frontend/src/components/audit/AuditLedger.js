import React, { useState, useEffect, useCallback, useMemo } from 'react';
import ticketService from '../../services/ticketService';
import { TableSkeleton } from '../common/LoadingSkeleton';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import Alert from '../common/Alert';
import { ShieldIcon, LockIcon } from '../common/Icons';

const AuditLedger = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [verificationReport, setVerificationReport] = useState(null);
  const [verifying, setVerifying] = useState(false);
  const [alert, setAlert] = useState(null);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setAlert(null);
    try {
      const data = await ticketService.getAuditLogs(page, 15);
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setAlert(err);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleVerifyChain = async () => {
    setVerifying(true);
    setAlert(null);
    try {
      const report = await ticketService.verifyAuditChain();
      setVerificationReport(report);
    } catch (err) {
      setAlert(err);
    } finally {
      setVerifying(false);
    }
  };

  const visibleLogs = useMemo(() => {
    if (!searchQuery.trim()) return logs;
    const q = searchQuery.toLowerCase();
    return logs.filter(
      (log) =>
        log.actorUsername?.toLowerCase().includes(q) ||
        log.action?.toLowerCase().includes(q) ||
        log.targetUsername?.toLowerCase().includes(q) ||
        log.details?.toLowerCase().includes(q)
    );
  }, [logs, searchQuery]);

  return (
    <div>
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">Cryptographic Audit Ledger</h1>
          <p className="ep-page-subtitle">
            Immutable SHA-256 hash-chain verification &amp; regulatory lifecycle audit trail.
          </p>
        </div>

        <button type="button" onClick={handleVerifyChain} disabled={verifying} className="ep-btn ep-btn--primary">
          <LockIcon size={15} /> {verifying ? 'Verifying Chain...' : 'Verify Cryptographic Chain'}
        </button>
      </div>

      {alert && (
        <Alert type="error" title={alert.title} detail={alert.detail} onClose={() => setAlert(null)} />
      )}

      {verificationReport && (
        <div
          role="status"
          className="ep-card"
          style={{
            backgroundColor: verificationReport.valid ? 'var(--ep-success-bg)' : 'var(--ep-danger-bg)',
            borderColor: verificationReport.valid ? 'var(--ep-success-border)' : 'var(--ep-danger-border)',
            marginBottom: '20px'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', gap: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <ShieldIcon size={22} />
              <div>
                <h2 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: verificationReport.valid ? 'var(--ep-success-text)' : 'var(--ep-danger-text)' }}>
                  {verificationReport.valid
                    ? 'Cryptographic Audit Chain Intact — 100% Valid'
                    : 'Tamper Detected in Audit Chain!'}
                </h2>
                <span style={{ fontSize: '12px', color: verificationReport.valid ? 'var(--ep-success-text)' : 'var(--ep-danger-text)' }}>
                  {verificationReport.valid
                    ? 'Monotonic sequence, parent hash linkage, and entry SHA-256 checksums verified.'
                    : 'Hash mismatch or sequence violation discovered.'}
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setVerificationReport(null)}
              aria-label="Dismiss verification report"
              className="ep-btn ep-btn--ghost ep-btn--sm"
            >
              &times;
            </button>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
              gap: '12px',
              backgroundColor: 'var(--ep-bg-surface)',
              padding: '12px 14px',
              borderRadius: 'var(--ep-radius-sm)',
              border: '1px solid var(--ep-border)',
              fontSize: '12px'
            }}
          >
            <div>
              <span style={{ color: 'var(--ep-text-muted)', display: 'block' }}>Chained Records</span>
              <strong className="ep-tabular" style={{ fontSize: '15px', color: 'var(--ep-text-primary)' }}>
                {verificationReport.chainedRecordsCount ?? verificationReport.totalChainedRecords ?? verificationReport.totalRecordsVerified ?? 0}
              </strong>
            </div>

            <div>
              <span style={{ color: 'var(--ep-text-muted)', display: 'block' }}>Unchained IAM Logs</span>
              <strong className="ep-tabular" style={{ fontSize: '15px', color: 'var(--ep-text-primary)' }}>
                {verificationReport.unchainedLegacyCount ?? verificationReport.totalUnchainedRecords ?? 0}
              </strong>
            </div>

            <div>
              <span style={{ color: 'var(--ep-text-muted)', display: 'block' }}>Last Sequence</span>
              <strong className="ep-tabular" style={{ fontSize: '15px', color: 'var(--ep-text-primary)' }}>
                #{verificationReport.headSequence ?? verificationReport.lastSequence ?? 0}
              </strong>
            </div>

            <div>
              <span style={{ color: 'var(--ep-text-muted)', display: 'block' }}>Head Hash</span>
              <code style={{ fontSize: '11px', color: 'var(--ep-primary)', wordBreak: 'break-all' }}>
                {(verificationReport.headHash || verificationReport.calculatedHeadHash)
                  ? `${(verificationReport.headHash || verificationReport.calculatedHeadHash).substring(0, 16)}...`
                  : 'Genesis Block'}
              </code>
            </div>
          </div>

          {verificationReport.violations && verificationReport.violations.length > 0 && (
            <div style={{ marginTop: '12px', padding: '10px 12px', backgroundColor: 'var(--ep-bg-surface)', border: '1px solid var(--ep-danger-border)', borderRadius: '6px' }}>
              <strong style={{ color: 'var(--ep-danger-text)', fontSize: '12px', display: 'block', marginBottom: '4px' }}>
                Discrepancies:
              </strong>
              <ul style={{ margin: 0, paddingLeft: '18px', color: 'var(--ep-danger-text)', fontSize: '12px' }}>
                {verificationReport.violations.map((v, i) => (
                  <li key={i}>{v}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Filter Bar */}
      <div className="ep-filter-bar">
        <div style={{ flex: 1, minWidth: '220px' }}>
          <input
            type="search"
            aria-label="Filter audit events on current page"
            placeholder="Filter current page by actor, action, target, or details..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="ep-input"
          />
        </div>
        <span className="ep-tabular" style={{ fontSize: '12px', color: 'var(--ep-text-muted)' }}>
          {totalElements} total ledger events
        </span>
      </div>

      {loading ? (
        <TableSkeleton rows={6} />
      ) : visibleLogs.length === 0 ? (
        <EmptyState
          icon={<ShieldIcon size={26} />}
          title="No audit events recorded"
          description={searchQuery ? 'No events on this page match your filter.' : 'Operational events will appear here once service actions occur.'}
        />
      ) : (
        <div className="ep-table-wrap" tabIndex={0} role="region" aria-label="Cryptographic audit ledger table">
          <table className="ep-table">
            <thead>
              <tr>
                <th>Seq #</th>
                <th>Timestamp</th>
                <th>Actor</th>
                <th>Action</th>
                <th>Target</th>
                <th>Details</th>
                <th>Hash (SHA-256)</th>
              </tr>
            </thead>
            <tbody>
              {visibleLogs.map((log) => (
                <tr key={log.id}>
                  <td className="ep-tabular" style={{ fontWeight: 700, color: log.sequenceNumber ? 'var(--ep-primary)' : 'var(--ep-text-muted)' }}>
                    {log.sequenceNumber ? `#${log.sequenceNumber}` : '—'}
                  </td>
                  <td className="ep-tabular" style={{ color: 'var(--ep-text-muted)', fontSize: '12px', whiteSpace: 'nowrap' }}>
                    {log.timestamp ? new Date(log.timestamp).toLocaleString() : ''}
                  </td>
                  <td style={{ fontWeight: 600 }}>{log.actorUsername}</td>
                  <td>
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        backgroundColor: 'var(--ep-bg-muted)',
                        color: 'var(--ep-text-primary)',
                        fontWeight: 700,
                        whiteSpace: 'nowrap'
                      }}
                    >
                      {log.action}
                    </span>
                  </td>
                  <td style={{ color: 'var(--ep-text-secondary)' }}>{log.targetUsername || '—'}</td>
                  <td style={{ maxWidth: '240px', color: 'var(--ep-text-secondary)' }}>
                    <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={log.details || ''}>
                      {log.details || '—'}
                    </div>
                  </td>
                  <td>
                    {log.currentHash ? (
                      <code
                        style={{
                          fontSize: '11px',
                          color: 'var(--ep-secondary)',
                          backgroundColor: 'var(--ep-secondary-subtle)',
                          padding: '2px 6px',
                          borderRadius: '4px',
                          fontFamily: 'var(--ep-font-mono)'
                        }}
                      >
                        {log.currentHash.substring(0, 10)}...
                      </code>
                    ) : (
                      <span style={{ color: 'var(--ep-text-muted)', fontSize: '12px' }}>Unchained</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <Pagination currentPage={page} totalPages={totalPages} totalElements={totalElements} onPageChange={setPage} />
          )}
        </div>
      )}
    </div>
  );
};

export default AuditLedger;
