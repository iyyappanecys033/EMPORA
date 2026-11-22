import axios from 'axios';

// Centralized Axios instance with automatic Cookie credentials and CSRF protection
const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Helper to read cookie value by name
export function getCookie(name) {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
  return match ? decodeURIComponent(match[2]) : null;
}

// Request interceptor: attach X-XSRF-TOKEN header on mutating requests
api.interceptors.request.use(
  (config) => {
    const method = (config.method || '').toUpperCase();
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
      const xsrfToken = getCookie('XSRF-TOKEN');
      if (xsrfToken) {
        config.headers['X-XSRF-TOKEN'] = xsrfToken;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let refreshSubscribers = [];

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb);
}

function onRefreshed() {
  refreshSubscribers.forEach((cb) => cb());
  refreshSubscribers = [];
}

// Response interceptor: RFC 7807 problem details parsing and silent 401 token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Handle 401 Unauthorized with silent token refresh
    if (
      error.response &&
      error.response.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh') &&
      !originalRequest.url?.includes('/auth/login')
    ) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          subscribeTokenRefresh(() => {
            resolve(api(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await axios.post('/api/auth/refresh', {}, { withCredentials: true });
        isRefreshing = false;
        onRefreshed();
        return api(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        refreshSubscribers = [];
        window.dispatchEvent(new Event('session-expired'));
        return Promise.reject(formatApiError(refreshError));
      }
    }

    return Promise.reject(formatApiError(error));
  }
);

// Standard RFC 7807 problem details extractor
export function formatApiError(error) {
  if (!error.response) {
    return {
      title: 'Network Error',
      detail: error.message || 'Unable to reach EMPORA server. Please check your connectivity.',
      status: 0,
      errors: []
    };
  }

  const data = error.response.data;
  const status = error.response.status;

  if (typeof data === 'object' && data !== null) {
    const detail = data.detail || data.message || (status === 403 ? 'Access Denied: Insufficient permissions' : 'An error occurred');
    const title = data.title || (status === 409 ? 'Conflict' : status === 400 ? 'Bad Request' : 'Error');
    const fieldErrors = Array.isArray(data.errors)
      ? data.errors
      : typeof data.errors === 'object' && data.errors !== null
      ? Object.entries(data.errors).map(([field, msg]) => ({ field, message: msg }))
      : [];

    return {
      title,
      detail,
      status,
      errors: fieldErrors,
      raw: data
    };
  }

  return {
    title: `Error (${status})`,
    detail: typeof data === 'string' ? data : error.message,
    status,
    errors: []
  };
}

export default api;
