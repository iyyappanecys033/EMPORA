import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import api from './api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const checkAuth = useCallback(async () => {
    try {
      const res = await api.get('/users/me');
      setCurrentUser(res.data);
      return res.data;
    } catch (err) {
      setCurrentUser(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();

    const handleSessionExpired = () => {
      setCurrentUser(null);
    };

    window.addEventListener('session-expired', handleSessionExpired);
    return () => window.removeEventListener('session-expired', handleSessionExpired);
  }, [checkAuth]);

  const login = async (username, password) => {
    await api.post('/auth/login', { username, password });
    return checkAuth();
  };

  const register = async (userData) => {
    await api.post('/auth/register', userData);
    return checkAuth();
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (e) {
      console.error('Logout error', e);
    } finally {
      setCurrentUser(null);
    }
  };

  // Role extraction and normalized evaluation
  const userRoles = useMemo(() => {
    return Array.isArray(currentUser?.roles)
      ? currentUser.roles.map((r) => (typeof r === 'string' ? r.toUpperCase() : ''))
      : [];
  }, [currentUser]);

  const hasRole = useCallback(
    (roleName) => {
      if (!roleName) return false;
      const target = roleName.toUpperCase().startsWith('ROLE_')
        ? roleName.toUpperCase()
        : 'ROLE_' + roleName.toUpperCase();
      return userRoles.some(
        (r) => r === target || r === target.replace('ROLE_', '')
      );
    },
    [userRoles]
  );

  const isAdmin = hasRole('ADMIN');
  const isAuditor = hasRole('AUDITOR') || isAdmin;
  const isResolver =
    hasRole('RESOLVER') ||
    hasRole('IT_SUPPORT') ||
    hasRole('FACILITIES_CREW') ||
    hasRole('HR_SPECIALIST') ||
    hasRole('DEPT_HEAD') ||
    isAdmin;
  const isApprover =
    hasRole('MANAGER') ||
    hasRole('HR_MANAGER') ||
    hasRole('DEPT_HEAD') ||
    isAdmin;
  const canViewAnalytics = isAdmin || isAuditor || isApprover || isResolver;

  const department = currentUser?.department || '';

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        isAuthenticated: !!currentUser,
        loading,
        login,
        register,
        logout,
        refreshUser: checkAuth,
        roles: userRoles,
        hasRole,
        isAdmin,
        isAuditor,
        isResolver,
        isApprover,
        canViewAnalytics,
        department
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
