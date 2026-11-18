import React, { useState, useEffect, useCallback } from 'react';
import ticketService from '../../services/ticketService';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import Alert from '../common/Alert';

const AuditLedger = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
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

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 700, margin: '0 0 4px', color: '#0f172a' }}>
            Cryptographic Audit Ledger
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: '#64748b' }}>
            Immutable SHA-256 hash-chain verification & regulatory lifecycle audit trail.
          </p>
        </div>

        <button
          type="button"
          onClick={handleVerifyChain}
          disabled={verifying}
          style={{
            backgroundColor: '#0f172a',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            padding: '9px 18px',
            fontSize: '13px',
            fontWeight: 600,
            cursor: verifying ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
          }}
        >
          <span>🔒</span> {verifying ? 'Verifying Chain...' : 'Verify Cryptographic Chain'}
        </button>
      </div>

      {alert && (
        <Alert
          type="error"
          title={alert.title}
          detail={alert.detail}
          onClose={() => setAlert(null)}
        />
      )}

      {/* Verification Report Card */}
      {verificationReport && (
        <div
          style={{
            backgroundColor: verificationReport.valid ? '#f0fdf4' : '#fef2f2',
            border: `1px solid ${verificationReport.valid ? '#bbf7d0' : '#fecaca'}`,
            borderRadius: '10px',
            padding: '18px 22px',
            marginBottom: '24px'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span style={{ fontSize: '20px' }}>
                {verificationReport.valid ? '🛡️' : '🚨'}
              </span>
              <div>
                <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: verificationReport.valid ? '#166534' : '#991b1b' }}>
                  {verificationReport.valid
                    ? 'Cryptographic Audit Chain Intact — 100% Valid'
                    : 'Tamper Detected in Audit Chain!'}
                </h3>
                <span style={{ fontSize: '12px', color: verificationReport.valid ? '#15803d' : '#b91c1c' }}>
                  {verificationReport.valid
                    ? 'Monotonic sequence, parent hash linkage, and entry SHA-256 checksums verified.'
                    : 'Hash mismatch or sequence violation discovered.'}
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setVerificationReport(null)}
              style={{ background: 'transparent', border: 'none', fontSize: '18px', cursor: 'pointer', color: '#64748b' }}
            >
              &times;
            </button>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '12px',
              backgroundColor: '#fff',
              padding: '12px',
              borderRadius: '6px',
              fontSize: '12px'
            }}
          >
            <div>
              <span style={{ color: '#64748b', display: 'block' }}>Chained Records</span>
              <strong style={{ fontSize: '14px', color: '#0f172a' }}>
                {verificationReport.totalChainedRecords}
              </strong>
            </div>

            <div>
              <span style={{ color: '#64748b', display: 'block' }}>Unchained IAM Logs</span>
              <strong style={{ fontSize: '14px', color: '#0f172a' }}>
                {verificationReport.totalUnchainedRecords}
              </strong>
            </div>

            <div>
              <span style={{ color: '#64748b', display: 'block' }}>Last Sequence</span>
              <strong style={{ fontSize: '14px', color: '#0f172a' }}>
                #{verificationReport.lastSequence}
              </strong>
            </div>

            <div>
              <span style={{ color: '#64748b', display: 'block' }}>Head Hash</span>
              <code style={{ fontSize: '11px', color: '#2563eb', wordBreak: 'break-all' }}>
                {verificationReport.headHash
                  ? `${verificationReport.headHash.substring(0, 16)}...`
                  : 'Genesis Block'}
              </code>
            </div>
          </div>

          {verificationReport.violations && verificationReport.violations.length > 0 && (
            <div style={{ marginTop: '12px', padding: '10px', backgroundColor: '#fee2e2', borderRadius: '6px' }}>
              <strong style={{ color: '#991b1b', fontSize: '12px', display: 'block', marginBottom: '4px' }}>
                Discrepancies:
              </strong>
              <ul style={{ margin: 0, paddingLeft: '18px', color: '#b91c1c', fontSize: '12px' }}>
                {verificationReport.violations.map((v, i) => (
                  <li key={i}>{v}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Audit Logs Table */}
      {loading ? (
        <LoadingSpinner message="Loading audit event ledger..." />
      ) : logs.length === 0 ? (
        <EmptyState
          icon="📜"
          title="No audit events recorded"
          description="Operational events will appear here once service actions occur."
        />
      ) : (
        <div style={{ backgroundColor: '#fff', borderRadius: '8px', border: '1px solid #e2e8f0', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Seq #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Timestamp</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Actor</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Action</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Target</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Details</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Hash (SHA-256)</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr
                  key={log.id}
                  style={{ borderBottom: '1px solid #f1f5f9' }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f8fafc')}
                  onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                >
                  <td style={{ padding: '12px 16px', fontWeight: 700, color: log.sequenceNumber ? '#2563eb' : '#94a3b8' }}>
                    {log.sequenceNumber ? `#${log.sequenceNumber}` : '—'}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#64748b', fontSize: '12px' }}>
                    {log.timestamp ? new Date(log.timestamp).toLocaleString() : ''}
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: 600, color: '#1e293b' }}>
                    {log.actorUsername}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        backgroundColor: '#f1f5f9',
                        color: '#334155',
                        fontWeight: 700
                      }}
                    >
                      {log.action}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', color: '#475569' }}>
                    {log.targetUsername || '—'}
                  </td>
                  <td style={{ padding: '12px 16px', maxWidth: '240px', color: '#475569' }}>
                    <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {log.details || '—'}
                    </div>
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {log.currentHash ? (
                      <code style={{ fontSize: '11px', color: '#0f766e', backgroundColor: '#f0fdfa', padding: '2px 6px', borderRadius: '4px' }}>
                        {log.currentHash.substring(0, 10)}...
                      </code>
                    ) : (
                      <span style={{ color: '#cbd5e1' }}>Unchained</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              totalElements={totalElements}
              onPageChange={(p) => setPage(p)}
            />
          )}
        </div>
      )}
    </div>
  );
};

export default AuditLedger;

