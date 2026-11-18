import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import ticketService from '../../services/ticketService';
import { StatusBadge, PriorityBadge } from '../common/Badge';
import { TableSkeleton } from '../common/LoadingSkeleton';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import Alert from '../common/Alert';
import CreateTicketModal from './CreateTicketModal';
import { PlusIcon, TicketIcon, ListIcon, GridIcon } from '../common/Icons';

const PRIORITY_WEIGHT = { CRITICAL: 5, URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };

const MyTickets = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [sortBy, setSortBy] = useState('newest');
  const [viewMode, setViewMode] = useState('table'); // 'table' | 'cards'
  const [searchQuery, setSearchQuery] = useState('');
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [alert, setAlert] = useState(null);

  const fetchTickets = useCallback(async () => {
    setLoading(true);
    try {
      const data = await ticketService.getMyTickets(page, 10);
      setTickets(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setAlert(err);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  const filteredTickets = useMemo(() => {
    const filtered = tickets.filter((t) => {
      if (statusFilter && t.status !== statusFilter) return false;
      if (priorityFilter && t.priority !== priorityFilter) return false;
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        return (
          t.ticketNumber?.toLowerCase().includes(q) ||
          t.description?.toLowerCase().includes(q) ||
          t.department?.toLowerCase().includes(q)
        );
      }
      return true;
    });

    return [...filtered].sort((a, b) => {
      if (sortBy === 'oldest') {
        return new Date(a.createdAt || 0) - new Date(b.createdAt || 0);
      }
      if (sortBy === 'priority') {
        return (PRIORITY_WEIGHT[b.priority] || 0) - (PRIORITY_WEIGHT[a.priority] || 0);
      }
      return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
    });
  }, [tickets, statusFilter, priorityFilter, searchQuery, sortBy]);

  return (
    <div>
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">My Service Tickets</h1>
          <p className="ep-page-subtitle">
            Track status, assignments, and SLA progress across your <strong className="ep-tabular">{totalElements}</strong> total requests.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <div role="group" aria-label="Ticket layout view" style={{ display: 'inline-flex', border: '1px solid var(--ep-border)', borderRadius: 'var(--ep-radius-sm)', overflow: 'hidden' }}>
            <button
              type="button"
              onClick={() => setViewMode('table')}
              aria-pressed={viewMode === 'table'}
              aria-label="Table view"
              className="ep-btn ep-btn--ghost ep-btn--sm"
              style={{
                borderRadius: 0,
                background: viewMode === 'table' ? 'var(--ep-selected)' : 'var(--ep-bg-surface)',
                color: viewMode === 'table' ? 'var(--ep-selected-text)' : 'var(--ep-text-muted)'
              }}
            >
              <ListIcon size={15} />
            </button>
            <button
              type="button"
              onClick={() => setViewMode('cards')}
              aria-pressed={viewMode === 'cards'}
              aria-label="Card view"
              className="ep-btn ep-btn--ghost ep-btn--sm"
              style={{
                borderRadius: 0,
                background: viewMode === 'cards' ? 'var(--ep-selected)' : 'var(--ep-bg-surface)',
                color: viewMode === 'cards' ? 'var(--ep-selected-text)' : 'var(--ep-text-muted)'
              }}
            >
              <GridIcon size={15} />
            </button>
          </div>

          <button type="button" onClick={() => setCreateModalOpen(true)} className="ep-btn ep-btn--primary">
            <PlusIcon size={16} /> New Request
          </button>
        </div>
      </div>

      {alert && (
        <Alert type="error" title={alert.title} detail={alert.detail} onClose={() => setAlert(null)} />
      )}

      {/* Filter & Sort Toolbar */}
      <div className="ep-filter-bar">
        <div style={{ flex: 1, minWidth: '210px' }}>
          <input
            type="search"
            aria-label="Search my tickets"
            placeholder="Search by Ticket #, description, or department..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="ep-input"
          />
        </div>

        <div style={{ width: '175px' }}>
          <select
            aria-label="Filter by ticket status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="ep-select"
          >
            <option value="">All Statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="PENDING_APPROVAL">Pending Approval</option>
            <option value="PENDING_SECONDARY_APPROVAL">Secondary Approval</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="ON_HOLD">On Hold</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
            <option value="REJECTED">Rejected</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        <div style={{ width: '150px' }}>
          <select
            aria-label="Filter by priority"
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
            className="ep-select"
          >
            <option value="">All Priorities</option>
            <option value="CRITICAL">Critical</option>
            <option value="URGENT">Urgent</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>

        <div style={{ width: '150px' }}>
          <select
            aria-label="Sort tickets"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="ep-select"
          >
            <option value="newest">Newest First</option>
            <option value="oldest">Oldest First</option>
            <option value="priority">Highest Priority</option>
          </select>
        </div>
      </div>

      {loading ? (
        <TableSkeleton rows={5} />
      ) : filteredTickets.length === 0 ? (
        <EmptyState
          icon={<TicketIcon size={26} />}
          title="No service tickets found"
          description={
            statusFilter || priorityFilter || searchQuery
              ? 'No tickets on this page match your selected filters.'
              : 'You have not submitted any service tickets yet.'
          }
          action={
            <button type="button" onClick={() => setCreateModalOpen(true)} className="ep-btn ep-btn--primary">
              Submit Your First Ticket
            </button>
          }
        />
      ) : viewMode === 'cards' ? (
        <div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))', gap: '16px', marginBottom: '16px' }}>
            {filteredTickets.map((t) => (
              <article key={t.id} className="ep-card ep-card--interactive" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: '12px' }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                    <span style={{ fontWeight: 800, fontSize: '13px', fontFamily: 'var(--ep-font-mono)' }}>{t.ticketNumber}</span>
                    <PriorityBadge priority={t.priority} />
                  </div>
                  <p style={{ margin: '0 0 10px', fontSize: '13px', color: 'var(--ep-text-secondary)', lineHeight: 1.5 }}>
                    {t.description}
                  </p>
                  <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)' }}>
                    {t.department} • Submitted {t.createdAt ? new Date(t.createdAt).toLocaleDateString() : 'N/A'}
                  </div>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '10px', borderTop: '1px solid var(--ep-border-subtle)' }}>
                  <StatusBadge status={t.status} />
                  <Link to={`/tickets/${t.id}`} className="ep-btn ep-btn--subtle ep-btn--sm">
                    View &rarr;
                  </Link>
                </div>
              </article>
            ))}
          </div>
          {totalPages > 1 && (
            <div className="ep-table-wrap">
              <Pagination currentPage={page} totalPages={totalPages} totalElements={totalElements} onPageChange={setPage} />
            </div>
          )}
        </div>
      ) : (
        <div className="ep-table-wrap" tabIndex={0} role="region" aria-label="My service tickets table">
          <table className="ep-table">
            <thead>
              <tr>
                <th>Ticket #</th>
                <th>Department</th>
                <th>Description</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Submitted</th>
                <th style={{ textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredTickets.map((t) => (
                <tr key={t.id}>
                  <td style={{ fontWeight: 700, fontFamily: 'var(--ep-font-mono)', whiteSpace: 'nowrap' }}>{t.ticketNumber}</td>
                  <td style={{ color: 'var(--ep-text-secondary)' }}>{t.department}</td>
                  <td style={{ maxWidth: '300px' }}>
                    <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {t.description}
                    </div>
                  </td>
                  <td><PriorityBadge priority={t.priority} /></td>
                  <td><StatusBadge status={t.status} /></td>
                  <td className="ep-tabular" style={{ color: 'var(--ep-text-muted)', fontSize: '12px', whiteSpace: 'nowrap' }}>
                    {t.createdAt ? new Date(t.createdAt).toLocaleDateString() : 'N/A'}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <Link to={`/tickets/${t.id}`} className="ep-btn ep-btn--subtle ep-btn--sm">
                      View &rarr;
                    </Link>
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

      {createModalOpen && (
        <CreateTicketModal
          isOpen={createModalOpen}
          onClose={() => setCreateModalOpen(false)}
          onTicketCreated={() => {
            setCreateModalOpen(false);
            fetchTickets();
          }}
        />
      )}
    </div>
  );
};

export default MyTickets;
