import api from './api';

const ticketService = {
  // Service Catalog Endpoints
  getCatalogItems: async (params = {}) => {
    const res = await api.get('/v1/catalog', { params });
    return res.data;
  },

  getCatalogItemById: async (id) => {
    const res = await api.get(`/v1/catalog/${id}`);
    return res.data;
  },

  // Ticket Operations
  submitTicket: async (ticketData) => {
    // ticketData: { catalogItemId, department, priority, description }
    const res = await api.post('/v1/tickets', ticketData);
    return res.data;
  },

  getMyTickets: async (page = 0, size = 10) => {
    const res = await api.get('/v1/tickets/my', { params: { page, size } });
    return res.data;
  },

  getTicketById: async (id) => {
    const res = await api.get(`/v1/tickets/${id}`);
    return res.data;
  },

  assignTicket: async (ticketId, assigneeId) => {
    const res = await api.post(`/v1/tickets/${ticketId}/assign`, { assigneeId });
    return res.data;
  },

  updateTicketStatus: async (ticketId, newStatus, reason, version) => {
    const res = await api.post(`/v1/tickets/${ticketId}/status`, {
      status: newStatus,
      reason,
      version
    });
    return res.data;
  },

  cancelTicket: async (ticketId, reason, version) => {
    const res = await api.post(`/v1/tickets/${ticketId}/cancel`, {
      reason,
      version
    });
    return res.data;
  },

  reopenTicket: async (ticketId, reason, version) => {
    const res = await api.post(`/v1/tickets/${ticketId}/reopen`, {
      reason,
      version
    });
    return res.data;
  },

  // Contextual Comments
  getComments: async (ticketId) => {
    const res = await api.get(`/v1/tickets/${ticketId}/comments`);
    return res.data;
  },

  addComment: async (ticketId, commentText, isInternal = false) => {
    const res = await api.post(`/v1/tickets/${ticketId}/comments`, {
      commentText,
      isInternal
    });
    return res.data;
  },

  // Department Queue
  getDepartmentQueue: async (department, params = {}) => {
    const res = await api.get(`/v1/departments/${encodeURIComponent(department)}/queue`, { params });
    return res.data;
  },

  // Approval Engine
  getPendingApprovals: async () => {
    const res = await api.get('/v1/approvals/pending');
    return res.data;
  },

  getApprovalHistory: async (ticketId) => {
    const res = await api.get(`/v1/approvals/tickets/${ticketId}`);
    return res.data;
  },

  decideApproval: async (taskId, decision, decisionNotes = '') => {
    // decision: 'APPROVED' | 'REJECTED'
    const res = await api.post(`/v1/approvals/${taskId}/decide`, {
      decision,
      reason: decisionNotes
    });
    return res.data;
  },

  // Audit Ledger & Cryptographic Verification
  getAuditLogs: async (page = 0, size = 20) => {
    const res = await api.get('/v1/audit/logs', { params: { page, size } });
    return res.data;
  },

  verifyAuditChain: async () => {
    const res = await api.get('/v1/audit/verify');
    return res.data;
  }
};

export default ticketService;

