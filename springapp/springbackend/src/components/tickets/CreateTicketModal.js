import React, { useState, useEffect } from 'react';
import ticketService from '../../services/ticketService';
import Alert from '../common/Alert';

const CreateTicketModal = ({ preselectedItem = null, isOpen, onClose, onTicketCreated }) => {
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
    <div style={modalOverlayStyle}>
      <div style={modalContentStyle}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ margin: 0, fontSize: '18px', color: '#0f172a', fontWeight: 700 }}>
            Submit New Service Ticket
          </h2>
          <button
            type="button"
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              fontSize: '22px',
              cursor: 'pointer',
              color: '#64748b'
            }}
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

        <form onSubmit={handleSubmit}>
          <div style={fieldGroupStyle}>
            <label htmlFor="serviceOfferingSelect" style={labelStyle}>Service Offering *</label>
            <select
              id="serviceOfferingSelect"
              value={selectedItemId}
              onChange={handleItemSelect}
              required
              disabled={loadingItems || submitting}
              style={inputStyle}
            >
              <option value="">-- Select a Service --</option>
              {catalogItems.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.title || item.name} ({item.department || item.targetDepartment}) {(item.slaTargetHours || item.slaHours) ? `[${item.slaTargetHours || item.slaHours}h SLA]` : ''}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div style={fieldGroupStyle}>
              <label htmlFor="targetDepartmentInput" style={labelStyle}>Target Department *</label>
              <input
                id="targetDepartmentInput"
                type="text"
                value={targetDepartment}
                onChange={(e) => setTargetDepartment(e.target.value)}
                required
                style={inputStyle}
              />
            </div>

            <div style={fieldGroupStyle}>
              <label htmlFor="prioritySelect" style={labelStyle}>Priority *</label>
              <select
                id="prioritySelect"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                style={inputStyle}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>
          </div>

          <div style={fieldGroupStyle}>
            <label htmlFor="justificationInput" style={labelStyle}>Business Justification & Details *</label>
            <textarea
              id="justificationInput"
              rows={4}
              value={justification}
              onChange={(e) => setJustification(e.target.value)}
              placeholder="Describe the nature of your request and reason..."
              required
              maxLength={2000}
              style={{ ...inputStyle, resize: 'vertical' }}
            />
            <div style={{ fontSize: '11px', color: '#94a3b8', textAlign: 'right', marginTop: '2px' }}>
              {justification.length} / 2000 characters
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              style={{
                padding: '9px 18px',
                borderRadius: '6px',
                border: '1px solid #cbd5e1',
                backgroundColor: '#fff',
                color: '#475569',
                fontSize: '13px',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={submitting}
              style={{
                padding: '9px 20px',
                borderRadius: '6px',
                border: 'none',
                backgroundColor: submitting ? '#93c5fd' : '#2563eb',
                color: '#fff',
                fontSize: '13px',
                fontWeight: 600,
                cursor: submitting ? 'not-allowed' : 'pointer'
              }}
            >
              {submitting ? 'Submitting...' : 'Submit Ticket'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

const modalOverlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: 'rgba(15, 23, 42, 0.6)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 100,
  padding: '16px'
};

const modalContentStyle = {
  backgroundColor: '#fff',
  borderRadius: '12px',
  width: '100%',
  maxWidth: '560px',
  padding: '24px',
  boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
  boxSizing: 'border-box'
};

const fieldGroupStyle = {
  marginBottom: '14px'
};

const labelStyle = {
  display: 'block',
  fontSize: '12px',
  fontWeight: 600,
  color: '#334155',
  marginBottom: '4px'
};

const inputStyle = {
  width: '100%',
  padding: '9px 12px',
  borderRadius: '6px',
  border: '1px solid #cbd5e1',
  fontSize: '13px',
  color: '#0f172a',
  boxSizing: 'border-box'
};

export default CreateTicketModal;

