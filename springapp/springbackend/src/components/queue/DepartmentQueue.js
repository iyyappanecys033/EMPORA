import React, { useState, useEffect, useCallback } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import { StatusBadge, PriorityBadge } from '../common/Badge';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import Alert from '../common/Alert';

const DepartmentQueue = () => {
  const { department: routeDept } = useParams();
  const navigate = useNavigate();
  const { currentUser, department: userDept } = useAuth();
  
  const activeDepartment = routeDept || userDept || 'IT';
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [alert, setAlert] = useState(null);

  const fetchQueue = useCallback(async () => {
    setLoading(true);
    setAlert(null);
    try {
      const params = {
        page,
        size: 15
      };
      if (statusFilter) {
        params.status = statusFilter;
      }
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

  const handleDeptChange = (e) => {
    const nextDept = e.target.value;
    navigate(`/queue/${nextDept}`);
  };

  const handleSelfAssign = async (ticketId) => {
    if (!currentUser?.id) return;
    try {
      await ticketService.assignTicket(ticketId, currentUser.id);
      fetchQueue();
    } catch (err) {
      setAlert(err);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 700, margin: '0 0 4px', color: '#0f172a' }}>
            {activeDepartment} Department Queue
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: '#64748b' }}>
            Specialist ticket dispatch, triage, assignment, and SLA fulfillment board.
          </p>
        </div>

        {/* Department Switcher */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#475569' }}>
            Queue:
          </label>
          <select
            value={activeDepartment}
            onChange={handleDeptChange}
            style={{
              padding: '8px 12px',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              fontSize: '13px',
              fontWeight: 600,
              backgroundColor: '#fff'
            }}
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
        <Alert
          type="error"
          title={alert.title}
          detail={alert.detail}
          onClose={() => setAlert(null)}
        />
      )}

      {/* Filter Bar */}
      <div
        style={{
          display: 'flex',
          gap: '12px',
          backgroundColor: '#fff',
          padding: '14px',
          borderRadius: '8px',
          border: '1px solid #e2e8f0',
          marginBottom: '20px',
          alignItems: 'center'
        }}
      >
        <span style={{ fontSize: '13px', fontWeight: 600, color: '#475569' }}>
          Status Filter:
        </span>
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPage(0);
          }}
          style={{
            padding: '7px 12px',
            borderRadius: '6px',
            border: '1px solid #cbd5e1',
            fontSize: '13px',
            backgroundColor: '#fff',
            minWidth: '160px'
          }}
        >
          <option value="">All Active Statuses</option>
          <option value="SUBMITTED">Submitted (New)</option>
          <option value="ASSIGNED">Assigned</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="ON_HOLD">On Hold</option>
          <option value="RESOLVED">Resolved</option>
          <option value="CLOSED">Closed</option>
        </select>
      </div>

      {loading ? (
        <LoadingSpinner message={`Loading ${activeDepartment} queue...`} />
      ) : tickets.length === 0 ? (
        <EmptyState
          icon="📥"
          title={`No tickets in ${activeDepartment} queue`}
          description={
            statusFilter
              ? `No tickets currently match the "${statusFilter}" filter.`
              : `All service requests for ${activeDepartment} are currently resolved or assigned.`
          }
        />
      ) : (
        <div style={{ backgroundColor: '#fff', borderRadius: '8px', border: '1px solid #e2e8f0', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Ticket #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Priority</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Description</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Requester</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Assignee</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>SLA Target</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((t) => (
                <tr
                  key={t.id}
                  style={{ borderBottom: '1px solid #f1f5f9', transition: 'background-color 0.15s' }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f8fafc')}
                  onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                >
                  <td style={{ padding: '12px 16px', fontWeight: 700, color: '#0f172a' }}>
                    {t.ticketNumber}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <PriorityBadge priority={t.priority} />
                  </td>
                  <td style={{ padding: '12px 16px', maxWidth: '260px' }}>
                    <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', color: '#1e293b' }}>
                      {t.description}
                    </div>
                  </td>
                  <td style={{ padding: '12px 16px', color: '#475569' }}>
                    👤 {t.requesterUsername}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {t.assigneeUsername ? (
                      <span style={{ color: '#1e293b', fontWeight: 500 }}>🛠️ {t.assigneeUsername}</span>
                    ) : (
                      <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Unassigned</span>
                    )}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <StatusBadge status={t.status} />
                  </td>
                  <td style={{ padding: '12px 16px', color: '#64748b', fontSize: '12px' }}>
                    {t.slaTargetDate ? new Date(t.slaTargetDate).toLocaleDateString() : 'N/A'}
                  </td>
                  <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                      {!t.assigneeUsername && (
                        <button
                          type="button"
                          onClick={() => handleSelfAssign(t.id)}
                          style={{
                            padding: '4px 8px',
                            borderRadius: '4px',
                            border: '1px solid #2563eb',
                            backgroundColor: '#2563eb',
                            color: '#fff',
                            fontSize: '11px',
                            fontWeight: 600,
                            cursor: 'pointer'
                          }}
                        >
                          Assign Me
                        </button>
                      )}
                      <Link
                        to={`/tickets/${t.id}`}
                        style={{
                          color: '#2563eb',
                          textDecoration: 'none',
                          fontWeight: 600,
                          fontSize: '11px',
                          padding: '4px 8px',
                          borderRadius: '4px',
                          backgroundColor: '#eff6ff',
                          display: 'inline-block'
                        }}
                      >
                        Manage &rarr;
                      </Link>
                    </div>
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

export default DepartmentQueue;
