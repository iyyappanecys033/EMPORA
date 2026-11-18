import api from './api';

const analyticsService = {
  getDashboardBundle: async (params = {}) => {
    const res = await api.get('/v1/analytics/dashboard', { params });
    return res.data;
  },

  getOverview: async (params = {}) => {
    const res = await api.get('/v1/analytics/overview', { params });
    return res.data;
  },

  getStatusDistribution: async (params = {}) => {
    const res = await api.get('/v1/analytics/tickets/status-distribution', { params });
    return res.data;
  },

  getPriorityDistribution: async (params = {}) => {
    const res = await api.get('/v1/analytics/tickets/priority-distribution', { params });
    return res.data;
  },

  getTrends: async (params = {}) => {
    const res = await api.get('/v1/analytics/tickets/trends', { params });
    return res.data;
  },

  getDepartmentPerformance: async (params = {}) => {
    const res = await api.get('/v1/analytics/departments/performance', { params });
    return res.data;
  },

  getServicePopularity: async (params = {}) => {
    const res = await api.get('/v1/analytics/services/popularity', { params });
    return res.data;
  },

  getSlaCompliance: async (params = {}) => {
    const res = await api.get('/v1/analytics/sla/compliance', { params });
    return res.data;
  },

  getApprovalsOverview: async (params = {}) => {
    const res = await api.get('/v1/analytics/approvals/overview', { params });
    return res.data;
  }
};

export default analyticsService;

