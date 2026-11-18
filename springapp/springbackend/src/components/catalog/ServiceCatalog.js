import React, { useState, useEffect, useCallback } from 'react';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import CreateTicketModal from '../tickets/CreateTicketModal';
import Alert from '../common/Alert';

const ServiceCatalog = () => {
  const { isAuthenticated } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState(null);
  const [alert, setAlert] = useState(null);

  const fetchCatalog = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: 9
      };
      if (departmentFilter) {
        params.department = departmentFilter;
      }
      const data = await ticketService.getCatalogItems(params);
      setItems(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setAlert(err);
    } finally {
      setLoading(false);
    }
  }, [page, departmentFilter]);

  useEffect(() => {
    fetchCatalog();
  }, [fetchCatalog]);

  const handleRequestClick = (item) => {
    if (!isAuthenticated) {
      setAlert({
        title: 'Authentication Required',
        detail: 'Please sign in to request services from the catalog.'
      });
      return;
    }
    setSelectedItem(item);
    setModalOpen(true);
  };

  const filteredItems = items.filter((item) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    const itemName = item.title || item.name || '';
    return (
      itemName.toLowerCase().includes(q) ||
      item.description?.toLowerCase().includes(q) ||
      item.itemCode?.toLowerCase().includes(q) ||
      item.category?.toLowerCase().includes(q)
    );
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 700, margin: '0 0 4px', color: '#0f172a' }}>
            Service Catalog
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: '#64748b' }}>
            Explore standardized IT, HR, and Facility service offerings.
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setSelectedItem(null);
            setModalOpen(true);
          }}
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
          ➕ Custom Request
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

      {/* Filter and Search Bar */}
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
            placeholder="Search service name, category, or code..."
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

        <div style={{ width: '180px' }}>
          <select
            value={departmentFilter}
            onChange={(e) => {
              setDepartmentFilter(e.target.value);
              setPage(0);
            }}
            style={{
              width: '100%',
              padding: '8px 12px',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              fontSize: '13px',
              boxSizing: 'border-box'
            }}
          >
            <option value="">All Departments</option>
            <option value="IT">IT Support</option>
            <option value="HR">Human Resources</option>
            <option value="Facilities">Facilities</option>
            <option value="Engineering">Engineering</option>
          </select>
        </div>
      </div>

      {/* Grid of Catalog Cards */}
      {loading ? (
        <LoadingSpinner message="Fetching catalog items..." />
      ) : filteredItems.length === 0 ? (
        <EmptyState
          icon="📋"
          title="No catalog items found"
          description="Try adjusting your department filter or search criteria."
        />
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
            gap: '18px',
            marginBottom: '24px'
          }}
        >
          {filteredItems.map((item) => (
            <div
              key={item.id}
              style={{
                backgroundColor: '#fff',
                borderRadius: '10px',
                border: '1px solid #e2e8f0',
                padding: '18px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                transition: 'box-shadow 0.2s'
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      color: '#2563eb',
                      backgroundColor: '#eff6ff',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      textTransform: 'uppercase'
                    }}
                  >
                    {item.department || item.targetDepartment}
                  </span>
                  <span style={{ fontSize: '11px', color: '#94a3b8', fontFamily: 'monospace' }}>
                    {item.itemCode}
                  </span>
                </div>

                <h3 style={{ margin: '0 0 6px', fontSize: '15px', fontWeight: 700, color: '#0f172a' }}>
                  {item.title || item.name}
                </h3>

                <p style={{ margin: '0 0 14px', fontSize: '13px', color: '#475569', lineHeight: 1.5, minHeight: '38px' }}>
                  {item.description}
                </p>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '14px' }}>
                  {item.requiresManagerApproval && (
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '2px 6px',
                        borderRadius: '4px',
                        backgroundColor: '#fef3c7',
                        color: '#92400e',
                        fontWeight: 600
                      }}
                    >
                      Manager Approval
                    </span>
                  )}
                  {(item.requiresSecondaryApproval || item.requiresDepartmentHeadApproval) && (
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '2px 6px',
                        borderRadius: '4px',
                        backgroundColor: '#ffedd5',
                        color: '#9a3412',
                        fontWeight: 600
                      }}
                    >
                      Secondary Approval
                    </span>
                  )}
                  {item.estimatedCost && (
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '2px 6px',
                        borderRadius: '4px',
                        backgroundColor: '#f1f5f9',
                        color: '#475569',
                        fontWeight: 600
                      }}
                    >
                      Est. ${Number(item.estimatedCost).toLocaleString()}
                    </span>
                  )}
                </div>
              </div>

              <div
                style={{
                  borderTop: '1px solid #f1f5f9',
                  paddingTop: '12px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}
              >
                <div>
                  <div style={{ fontSize: '11px', color: '#94a3b8' }}>Target SLA</div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#1e293b' }}>
                    {item.slaTargetHours || item.slaHours || 24} hours
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => handleRequestClick(item)}
                  style={{
                    backgroundColor: '#2563eb',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '6px',
                    padding: '7px 16px',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  Request Service
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <Pagination
          currentPage={page}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={(p) => setPage(p)}
        />
      )}

      {modalOpen && (
        <CreateTicketModal
          preselectedItem={selectedItem}
          isOpen={modalOpen}
          onClose={() => {
            setModalOpen(false);
            setSelectedItem(null);
          }}
          onTicketCreated={() => {
            setModalOpen(false);
            setSelectedItem(null);
            setAlert({
              type: 'success',
              title: 'Request Submitted',
              detail: 'Your service ticket was successfully created.'
            });
          }}
        />
      )}
    </div>
  );
};

export default ServiceCatalog;

