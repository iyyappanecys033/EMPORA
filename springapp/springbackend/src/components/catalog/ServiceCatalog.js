import React, { useState, useEffect, useCallback, useMemo } from 'react';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import { CardGridSkeleton } from '../common/LoadingSkeleton';
import EmptyState from '../common/EmptyState';
import Pagination from '../common/Pagination';
import CreateTicketModal from '../tickets/CreateTicketModal';
import Alert from '../common/Alert';
import { PlusIcon, ClockIcon, CatalogIcon } from '../common/Icons';

const ServiceCatalog = () => {
  const { isAuthenticated } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState(null);
  const [alert, setAlert] = useState(null);

  const fetchCatalog = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 9 };
      if (departmentFilter) params.department = departmentFilter;
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

  // Derive real categories only from returned catalog data (never fabricate unsupported categories)
  const categories = useMemo(() => {
    const set = new Set();
    items.forEach((i) => {
      if (i.category) set.add(i.category);
    });
    return Array.from(set).sort();
  }, [items]);

  const handleRequestClick = (item) => {
    if (!isAuthenticated) {
      setAlert({
        type: 'error',
        title: 'Authentication Required',
        detail: 'Please sign in to request services from the catalog.'
      });
      return;
    }
    setSelectedItem(item);
    setModalOpen(true);
  };

  const filteredItems = items.filter((item) => {
    if (categoryFilter && item.category !== categoryFilter) return false;
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
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">Service Catalog</h1>
          <p className="ep-page-subtitle">
            Explore standardized IT, HR, and Facility service offerings with transparent SLA targets.
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setSelectedItem(null);
            setModalOpen(true);
          }}
          className="ep-btn ep-btn--primary"
        >
          <PlusIcon size={16} /> Custom Request
        </button>
      </div>

      {alert && (
        <Alert
          type={alert.type || 'error'}
          title={alert.title}
          detail={alert.detail}
          onClose={() => setAlert(null)}
        />
      )}

      {/* Search & Filter Toolbar */}
      <div className="ep-filter-bar">
        <div style={{ flex: 1, minWidth: '220px' }}>
          <input
            type="search"
            aria-label="Search service catalog"
            placeholder="Search service name, category, or code..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="ep-input"
          />
        </div>

        <div style={{ width: '190px' }}>
          <select
            aria-label="Filter catalog by department"
            value={departmentFilter}
            onChange={(e) => {
              setDepartmentFilter(e.target.value);
              setCategoryFilter('');
              setPage(0);
            }}
            className="ep-select"
          >
            <option value="">All Departments</option>
            <option value="IT">IT Support</option>
            <option value="HR">Human Resources</option>
            <option value="Facilities">Facilities</option>
            <option value="Engineering">Engineering</option>
            <option value="Finance">Finance</option>
          </select>
        </div>
      </div>

      {/* Real Category Chips */}
      {categories.length > 0 && (
        <div className="ep-pill-group" role="group" aria-label="Filter by category" style={{ marginBottom: '18px' }}>
          <button
            type="button"
            onClick={() => setCategoryFilter('')}
            className={`ep-pill ${categoryFilter === '' ? 'ep-pill--active' : ''}`}
          >
            All Categories
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setCategoryFilter(cat)}
              className={`ep-pill ${categoryFilter === cat ? 'ep-pill--active' : ''}`}
            >
              {cat}
            </button>
          ))}
        </div>
      )}

      {/* Catalog Grid */}
      {loading ? (
        <CardGridSkeleton count={6} />
      ) : filteredItems.length === 0 ? (
        <EmptyState
          icon={<CatalogIcon size={26} />}
          title="No catalog items found"
          description="Try adjusting your department, category, or keyword search criteria."
          action={
            (searchQuery || departmentFilter || categoryFilter) && (
              <button
                type="button"
                className="ep-btn ep-btn--secondary"
                onClick={() => {
                  setSearchQuery('');
                  setDepartmentFilter('');
                  setCategoryFilter('');
                  setPage(0);
                }}
              >
                Clear Filters
              </button>
            )
          }
        />
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(295px, 1fr))',
            gap: '18px',
            marginBottom: '24px'
          }}
        >
          {filteredItems.map((item) => (
            <article
              key={item.id}
              className="ep-card ep-card--interactive"
              style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px', gap: '8px' }}>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      color: 'var(--ep-selected-text)',
                      backgroundColor: 'var(--ep-primary-subtle)',
                      border: '1px solid var(--ep-primary-border)',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      textTransform: 'uppercase'
                    }}
                  >
                    {item.department || item.targetDepartment}
                  </span>
                  <span style={{ fontSize: '11px', color: 'var(--ep-text-muted)', fontFamily: 'var(--ep-font-mono)' }}>
                    {item.itemCode}
                  </span>
                </div>

                <h3 style={{ margin: '0 0 6px', fontSize: '15px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
                  {item.title || item.name}
                </h3>

                <p style={{ margin: '0 0 14px', fontSize: '13px', color: 'var(--ep-text-secondary)', lineHeight: 1.5, minHeight: '38px' }}>
                  {item.description}
                </p>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '14px' }}>
                  {item.requiresManagerApproval && (
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '2px 7px',
                        borderRadius: '4px',
                        backgroundColor: 'var(--ep-warning-bg)',
                        color: 'var(--ep-warning-text)',
                        border: '1px solid var(--ep-warning-border)',
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
                        padding: '2px 7px',
                        borderRadius: '4px',
                        backgroundColor: 'var(--ep-st-pending2-bg)',
                        color: 'var(--ep-st-pending2-fg)',
                        border: '1px solid var(--ep-st-pending2-bd)',
                        fontWeight: 600
                      }}
                    >
                      Secondary Approval
                    </span>
                  )}
                  {Number(item.estimatedCost) > 0 && (
                    <span
                      style={{
                        fontSize: '11px',
                        padding: '2px 7px',
                        borderRadius: '4px',
                        backgroundColor: 'var(--ep-bg-muted)',
                        color: 'var(--ep-text-secondary)',
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
                  borderTop: '1px solid var(--ep-border-subtle)',
                  paddingTop: '12px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '10px'
                }}
              >
                <div>
                  <div style={{ fontSize: '11px', color: 'var(--ep-text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <ClockIcon size={12} /> Target SLA
                  </div>
                  <div className="ep-tabular" style={{ fontSize: '13px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
                    {item.slaTargetHours || item.slaHours || 24} hours
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => handleRequestClick(item)}
                  className="ep-btn ep-btn--primary ep-btn--sm"
                >
                  Request Service
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="ep-table-wrap">
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={(p) => setPage(p)}
          />
        </div>
      )}

      {modalOpen && (
        <CreateTicketModal
          preselectedItem={selectedItem}
          isOpen={modalOpen}
          onClose={() => {
            setModalOpen(false);
            setSelectedItem(null);
          }}
          onTicketCreated={(created) => {
            setModalOpen(false);
            setSelectedItem(null);
            setAlert({
              type: 'success',
              title: 'Request Submitted',
              detail: created?.ticketNumber
                ? `Service ticket ${created.ticketNumber} was successfully created.`
                : 'Your service ticket was successfully created.'
            });
          }}
        />
      )}
    </div>
  );
};

export default ServiceCatalog;
