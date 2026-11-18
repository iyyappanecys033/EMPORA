import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import ticketService from '../../services/ticketService';
import { StatusBadge, PriorityBadge } from '../common/Badge';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import Alert from '../common/Alert';
import CreateTicketModal from './CreateTicketModal';

const MyTickets = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
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

  const filteredTickets = tickets.filter((t) => {
    if (statusFilter && t.status !== statusFilter) return false;
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

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 700, margin: '0 0 4px', color: '#0f172a' }}>
            My Service Tickets
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: '#64748b' }}>
            Track the status, assignments, and resolution progress of your requests.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setCreateModalOpen(true)}
          style={{
            backgroundColor: '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            padding: '9px 16px',
            fontSize: '13px',
            fontWeight: 600,
            cursor: 'pointer'
          }}
        >
          ➕ New Request
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

      {/* Filter Toolbar */}
      <div
        style={{
          display: 'flex',
          gap: '12px',
          backgroundColor: '#fff',
          padding: '14px',
          borderRadius: '8px',
          border: '1px solid #e2e8f0',
          marginBottom: '20px',
          flexWrap: 'wrap'
        }}
      >
        <div style={{ flex: 1, minWidth: '220px' }}>
          <input
            type="text"
            placeholder="Search by Ticket #, description or department..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              padding: '8px 12px',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              fontSize: '13px',
              boxSizing: 'border-box'
            }}
          />
        </div>

        <div style={{ width: '200px' }}>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{
              width: '100%',
              padding: '8px 12px',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              fontSize: '13px',
              boxSizing: 'border-box'
            }}
          >
            <option value="">All Statuses</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="PENDING_APPROVAL">Pending Approval</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="ON_HOLD">On Hold</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
            <option value="REJECTED">Rejected</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner message="Fetching your tickets..." />
      ) : filteredTickets.length === 0 ? (
        <EmptyState
          icon="🎫"
          title="No service tickets found"
          description={
            statusFilter || searchQuery
              ? 'No tickets match the selected filters.'
              : 'You have not submitted any service tickets yet.'
          }
          action={
            <button
              type="button"
              onClick={() => setCreateModalOpen(true)}
              style={{
                backgroundColor: '#2563eb',
                color: '#fff',
                border: 'none',
                borderRadius: '6px',
                padding: '8px 16px',
                fontSize: '13px',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              Submit Your First Ticket
            </button>
          }
        />
      ) : (
        <div style={{ backgroundColor: '#fff', borderRadius: '8px', border: '1px solid #e2e8f0', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Ticket #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Department</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Description</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Priority</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Submitted</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredTickets.map((t) => (
                <tr
                  key={t.id}
                  style={{ borderBottom: '1px solid #f1f5f9', transition: 'background-color 0.15s' }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f8fafc')}
                  onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                >
                  <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0f172a' }}>
                    {t.ticketNumber}
                  </td>
                  <td style={{ padding: '14px 16px', color: '#475569' }}>
                    {t.department}
                  </td>
                  <td style={{ padding: '14px 16px', maxWidth: '300px', color: '#1e293b' }}>
                    <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {t.description}
                    </div>
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <PriorityBadge priority={t.priority} />
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <StatusBadge status={t.status} />
                  </td>
                  <td style={{ padding: '14px 16px', color: '#64748b', fontSize: '12px' }}>
                    {t.createdAt ? new Date(t.createdAt).toLocaleDateString() : 'N/A'}
                  </td>
                  <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                    <Link
                      to={`/tickets/${t.id}`}
                      style={{
                        color: '#2563eb',
                        textDecoration: 'none',
                        fontWeight: 600,
                        fontSize: '12px',
                        padding: '4px 10px',
                        borderRadius: '4px',
                        backgroundColor: '#eff6ff',
                        display: 'inline-block'
                      }}
                    >
                      View &rarr;
                    </Link>
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

