import api from './api';

const notificationService = {
  getMyNotifications: async (params = {}) => {
    const res = await api.get('/v1/notifications/my', { params });
    return res.data;
  },

  getUnreadCount: async () => {
    const res = await api.get('/v1/notifications/my/unread-count');
    return res.data;
  },

  markAsRead: async (notificationId) => {
    const res = await api.put(`/v1/notifications/${notificationId}/read`);
    return res.data;
  },

  markAllAsRead: async () => {
    const res = await api.put('/v1/notifications/my/read-all');
    return res.data;
  }
};

export default notificationService;

