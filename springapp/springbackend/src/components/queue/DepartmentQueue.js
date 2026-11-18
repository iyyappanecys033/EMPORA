import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import { useToast } from '../../services/ToastContext';
import { StatusBadge, PriorityBadge } from '../common/Badge';
import { TableSkeleton } from '../common/LoadingSkeleton';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import Alert from '../common/Alert';
import { QueueIcon } from '../common/Icons';

function formatTicketAge(createdAt) {
  if (!createdAt) return '—';
  const diffMs = Math.max(0, Date.now() - new Date(createdAt).getTime());
  const hrs = Math.floor(diffMs / 3600000);
  if (hrs < 24) return `${hrs}h`;
  const days = Math.floor(hrs / 24);
  return `${days}d ${hrs % 24}h`;
}

const DepartmentQueue = () => {
  const { department: routeDept } = useParams();
  const navigate = useNavigate();
  const { currentUser, department: userDept } = useAuth();
  const { showToast } = useToast();

  const activeDepartment = routeDept || userDept || 'IT';
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [alert, setAlert] = useState(null);

  const fetchQueue = useCallback(async () => {
    setLoading(true);
    setAlert(null);
    try {
      const params = { page, size: 15 };
      if (statusFilter) params.status = statusFilter;
      const data = await ticketService.getDepartmentQueue(activeDepartment, params);
      setTickets(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setAlert(err);
      setTickets([]);
    } finally {
      setLoading(false);
    }
  }, [activeDepartment, page, statusFilter]);

  useEffect(() => {
    fetchQueue();
  }, [fetchQueue]);

  const handleSelfAssign = async (ticketId, ticketNumber) => {
    if (!currentUser?.id) return;
    try {
      await ticketService.assignTicket(ticketId, currentUser.id);
      showToast({ type: 'success', title: 'Assigned to You', message: `${ticketNumber || `Ticket #${ticketId}`} claimed from queue.` });
      fetchQueue();
    } catch (err) {
      setAlert(err);
    }
  };

  const visibleTickets = useMemo(() => {
    return tickets.filter((t) => {
      if (priorityFilter && t.priority !== priorityFilter) return false;
      if (!searchQuery) return true;
      const q = searchQuery.toLowerCase();
      return (
        t.ticketNumber?.toLowerCase().includes(q) ||
        t.description?.toLowerCase().includes(q) ||
        t.requesterUsername?.toLowerCase().includes(q) ||
        t.assigneeUsername?.toLowerCase().includes(q)
      );
    });
  }, [tickets, priorityFilter, searchQuery]);

  return (
    <div>
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">{activeDepartment} Department Queue</h1>
          <p className="ep-page-subtitle">
            Specialist ticket triage, assignment, ticket age, and SLA fulfillment board (<strong className="ep-tabular">{totalElements}</strong> total).
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label htmlFor="dept-queue-switcher" style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ep-text-muted)' }}>
            Queue:
          </label>
          <select
            id="dept-queue-switcher"
            value={activeDepartment}
            onChange={(e) => navigate(`/queue/${e.target.value}`)}
            className="ep-select"
            style={{ width: '175px', fontWeight: 600 }}
          >
            <option value="IT">IT Support</option>
            <option value="HR">Human Resources</option>
            <option value="Facilities">Facilities</option>
            <option value="Engineering">Engineering</option>
            <option value="Finance">Finance</option>
          </select>
        </div>
      </div>

      {alert && (
        <Alert type="error" title={alert.title} detail={alert.detail} onClose={() => setAlert(null)} />
      )}

      <div className="ep-filter-bar">
        <div style={{ flex: 1, minWidth: '200px' }}>
          <input
            type="search"
            aria-label="Search department queue"
            placeholder="Search ticket #, requester, assignee, or summary..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="ep-input"
          />
        </div>

        <div style={{ width: '175px' }}>
          <select
            aria-label="Filter queue by status"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="ep-select"
          >
            <option value="">All Statuses</option>
            <option value="SUBMITTED">Submitted (New)</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="ON_HOLD">On Hold</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
          </select>
        </div>

        <div style={{ width: '150px' }}>
          <select
            aria-label="Filter queue by priority"
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
      </div>

      {loading ? (
        <TableSkeleton rows={6} />
      ) : visibleTickets.length === 0 ? (
        <EmptyState
          icon={<QueueIcon size={26} />}
          title={`No tickets in ${activeDepartment} queue`}
          description={
            statusFilter || priorityFilter || searchQuery
              ? 'No tickets match your current queue filters.'
              : `All service requests for ${activeDepartment} are currently resolved or assigned.`
          }
        />
      ) : (
        <div className="ep-table-wrap" tabIndex={0} role="region" aria-label={`${activeDepartment} department queue table`}>
          <table className="ep-table">
            <thead>
              <tr>
                <th>Ticket #</th>
                <th>Priority</th>
                <th>Description</th>
                <th>Requester</th>
                <th>Assignee</th>
                <th>Status</th>
                <th>Age</th>
                <th>SLA Target</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visibleTickets.map((t) => {
                const overdue =
                  t.slaTargetDate &&
                  !['RESOLVED', 'CLOSED', 'REJECTED', 'CANCELLED', 'ON_HOLD'].includes(t.status) &&
                  new Date(t.slaTargetDate) < new Date();
                return (
                  <tr key={t.id}>
                    <td style={{ fontWeight: 700, fontFamily: 'var(--ep-font-mono)', whiteSpace: 'nowrap' }}>{t.ticketNumber}</td>
                    <td><PriorityBadge priority={t.priority} /></td>
                    <td style={{ maxWidth: '240px' }}>
                      <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {t.description}
                      </div>
                    </td>
                    <td style={{ color: 'var(--ep-text-secondary)', whiteSpace: 'nowrap' }}>{t.requesterUsername}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      {t.assigneeUsername ? (
                        <span style={{ fontWeight: 600 }}>{t.assigneeUsername}</span>
                      ) : (
                        <span style={{ color: 'var(--ep-text-muted)', fontStyle: 'italic' }}>Unassigned</span>
                      )}
                    </td>
                    <td><StatusBadge status={t.status} /></td>
                    <td className="ep-tabular" style={{ fontSize: '12px', color: 'var(--ep-text-muted)', whiteSpace: 'nowrap' }}>
                      {formatTicketAge(t.createdAt)}
                    </td>
                    <td className="ep-tabular" style={{ fontSize: '12px', whiteSpace: 'nowrap', color: overdue ? 'var(--ep-danger-text)' : 'var(--ep-text-muted)', fontWeight: overdue ? 700 : 500 }}>
                      {t.slaTargetDate ? new Date(t.slaTargetDate).toLocaleDateString() : 'N/A'}
                      {overdue && ' (Overdue)'}
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        {!t.assigneeUsername && (
                          <button
                            type="button"
                            onClick={() => handleSelfAssign(t.id, t.ticketNumber)}
                            className="ep-btn ep-btn--primary ep-btn--sm"
                          >
                            Assign Me
                          </button>
                        )}
                        <Link to={`/tickets/${t.id}`} className="ep-btn ep-btn--subtle ep-btn--sm">
                          Manage &rarr;
                        </Link>
                      </div>
                    </td>
                  </tr>
                );
              })}
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

export default DepartmentQueue;
