import React, { useState, useEffect } from 'react';
import ticketService from '../../services/ticketService';
import { useToast } from '../../services/ToastContext';
import Alert from '../common/Alert';

const CreateTicketModal = ({ preselectedItem = null, isOpen, onClose, onTicketCreated }) => {
  const { showToast } = useToast();
  const [catalogItems, setCatalogItems] = useState([]);
  const [selectedItemId, setSelectedItemId] = useState(preselectedItem?.id || '');
  const [priority, setPriority] = useState('MEDIUM');
  const [targetDepartment, setTargetDepartment] = useState(
    preselectedItem?.department || preselectedItem?.targetDepartment || 'IT'
  );
  const [justification, setJustification] = useState('');
  const [loadingItems, setLoadingItems] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorObj, setErrorObj] = useState(null);

  useEffect(() => {
    if (preselectedItem) {
      setSelectedItemId(preselectedItem.id);
      setTargetDepartment(preselectedItem.department || preselectedItem.targetDepartment || 'IT');
    }
  }, [preselectedItem]);

  useEffect(() => {
    if (!isOpen) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape' && !submitting) onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [isOpen, submitting, onClose]);

  useEffect(() => {
    const fetchCatalog = async () => {
      setLoadingItems(true);
      try {
        const res = await ticketService.getCatalogItems({ size: 100 });
        setCatalogItems(res.content || []);
      } catch (err) {
        console.error('Failed to load catalog items for dropdown', err);
      } finally {
        setLoadingItems(false);
      }
    };

    if (isOpen) {
      fetchCatalog();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const selectedOffering =
    catalogItems.find((item) => String(item.id) === String(selectedItemId)) || preselectedItem;

  const handleItemSelect = (e) => {
    const id = e.target.value;
    setSelectedItemId(id);
    const found = catalogItems.find((item) => String(item.id) === String(id));
    if (found) {
      setTargetDepartment(found.department || found.targetDepartment || 'IT');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setErrorObj(null);

    if (!selectedItemId) {
      setErrorObj({ title: 'Validation Error', detail: 'Please select a service catalog item.' });
      return;
    }

    if (!justification.trim()) {
      setErrorObj({ title: 'Validation Error', detail: 'Justification must not be blank.' });
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        catalogItemId: Number(selectedItemId),
        department: targetDepartment,
        priority,
        description: justification.trim()
      };
      const created = await ticketService.submitTicket(payload);
      showToast({
        type: 'success',
        title: 'Service Ticket Submitted',
        message: created?.ticketNumber ? `Reference: ${created.ticketNumber}` : 'Request created successfully.'
      });
      if (onTicketCreated) {
        onTicketCreated(created);
      }
      onClose();
    } catch (err) {
      setErrorObj(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="ep-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="create-ticket-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !submitting) onClose();
      }}
    >
      <div className="ep-modal">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h2 id="create-ticket-modal-title" style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: 'var(--ep-text-primary)' }}>
              Submit New Service Ticket
            </h2>
            <p style={{ margin: '2px 0 0', fontSize: '12px', color: 'var(--ep-text-muted)' }}>
              Select a catalog offering and provide clear business justification for faster fulfillment.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            aria-label="Close ticket modal"
            className="ep-btn ep-btn--ghost ep-btn--sm"
          >
            &times;
          </button>
        </div>

        {errorObj && (
          <Alert
            type="error"
            title={errorObj.title}
            detail={errorObj.detail}
            errors={errorObj.errors}
            onClose={() => setErrorObj(null)}
          />
        )}

        <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '14px' }}>
          <div>
            <label htmlFor="serviceOfferingSelect" className="ep-label">Service Offering *</label>
            <select
              id="serviceOfferingSelect"
              value={selectedItemId}
              onChange={handleItemSelect}
              required
              disabled={loadingItems || submitting}
              className="ep-select"
            >
              <option value="">-- Select a Service --</option>
              {catalogItems.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.title || item.name} ({item.department || item.targetDepartment}){' '}
                  {item.slaTargetHours || item.slaHours ? `[${item.slaTargetHours || item.slaHours}h SLA]` : ''}
                </option>
              ))}
            </select>
          </div>

          {selectedOffering && selectedItemId && (
            <div
              style={{
                padding: '10px 12px',
                borderRadius: 'var(--ep-radius-sm)',
                background: 'var(--ep-bg-subtle)',
                border: '1px solid var(--ep-border)',
                fontSize: '12px',
                color: 'var(--ep-text-secondary)'
              }}
            >
              <strong>Selected Service:</strong> {selectedOffering.title || selectedOffering.name || 'Catalog Item'}
              {' • '}SLA: <strong>{selectedOffering.slaTargetHours || selectedOffering.slaHours || 24}h</strong>
              {selectedOffering.requiresManagerApproval ? ' • Requires Manager Approval' : ' • Direct Queue Routing'}
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
            <div>
              <label htmlFor="targetDepartmentInput" className="ep-label">Target Department *</label>
              <input
                id="targetDepartmentInput"
                type="text"
                value={targetDepartment}
                onChange={(e) => setTargetDepartment(e.target.value)}
                required
                disabled={submitting}
                className="ep-input"
              />
            </div>

            <div>
              <label htmlFor="prioritySelect" className="ep-label">Priority *</label>
              <select
                id="prioritySelect"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                disabled={submitting}
                className="ep-select"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>
          </div>

          <div>
            <label htmlFor="justificationInput" className="ep-label">Business Justification &amp; Details *</label>
            <textarea
              id="justificationInput"
              rows={4}
              value={justification}
              onChange={(e) => setJustification(e.target.value)}
              placeholder="Describe the nature of your request and reason..."
              required
              maxLength={2000}
              disabled={submitting}
              className="ep-textarea"
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: 'var(--ep-text-muted)', marginTop: '4px' }}>
              <span>Include relevant context, deadlines, or asset references.</span>
              <span className="ep-tabular">{justification.length} / 2000 characters</span>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '6px' }}>
            <button type="button" onClick={onClose} disabled={submitting} className="ep-btn ep-btn--secondary">
              Cancel
            </button>
            <button type="submit" disabled={submitting} className="ep-btn ep-btn--primary">
              {submitting ? 'Submitting...' : 'Submit Ticket'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateTicketModal;
