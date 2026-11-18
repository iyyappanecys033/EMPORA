import React, { useState } from 'react';
import { useAuth } from '../services/AuthContext';

const LoginModal = ({ isOpen, onClose }) => {
  const { login, register } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (isRegister) {
        await register({ username, password, email, phone });
      } else {
        await login(username, password);
      }
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || (isRegister ? 'Registration failed' : 'Invalid username or password');
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setSubmitting(false);
    }
  };

  const fillQuick = (u, p) => {
    setIsRegister(false);
    setUsername(u);
    setPassword(p);
  };

  return (
    <div style={styles.overlay}>
      <div style={styles.modal}>
        <div style={styles.header}>
          <h2 style={{ margin: 0, color: '#1e293b' }}>
            {isRegister ? 'Create EPAMS Account' : 'Sign in to EPAMS'}
          </h2>
          <button style={styles.closeBtn} onClick={onClose}>&times;</button>
        </div>

        <p style={{ color: '#64748b', fontSize: '14px', margin: '8px 0 16px' }}>
          {isRegister
            ? 'Register a new employee/administrator account'
            : 'Authenticate to manage users, roles, and profiles'}
        </p>

        {error && <div style={styles.errorAlert}>{error}</div>}

        <div style={styles.quickBox}>
          <span style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>Quick Demo Credentials:</span>
          <div style={{ display: 'flex', gap: '8px', marginTop: '6px' }}>
            <button
              type="button"
              style={styles.quickBtn}
              onClick={() => fillQuick('admin', 'admin123')}
            >
              Admin (admin / admin123)
            </button>
            <button
              type="button"
              style={styles.quickBtn}
              onClick={() => fillQuick('john_doe', 'user123')}
            >
              User (john_doe / user123)
            </button>
          </div>
        </div>

        <form onSubmit={handleSubmit} style={{ marginTop: '16px' }}>
          <div style={styles.field}>
            <label style={styles.label}>Username</label>
            <input
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. admin"
              style={styles.input}
            />
          </div>

          <div style={styles.field}>
            <label style={styles.label}>Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              style={styles.input}
            />
          </div>

          {isRegister && (
            <>
              <div style={styles.field}>
                <label style={styles.label}>Email Address</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="employee@epams.com"
                  style={styles.input}
                />
              </div>
              <div style={styles.field}>
                <label style={styles.label}>Phone</label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="9876543210"
                  style={styles.input}
                />
              </div>
            </>
          )}

          <button
            type="submit"
            disabled={submitting}
            style={styles.submitBtn}
          >
            {submitting ? 'Authenticating...' : isRegister ? 'Register & Sign In' : 'Sign In'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '16px', fontSize: '14px', color: '#64748b' }}>
          {isRegister ? (
            <span>
              Already have an account?{' '}
              <button
                type="button"
                style={styles.linkBtn}
                onClick={() => { setIsRegister(false); setError(''); }}
              >
                Sign In
              </button>
            </span>
          ) : (
            <span>
              Need a new account?{' '}
              <button
                type="button"
                style={styles.linkBtn}
                onClick={() => { setIsRegister(true); setError(''); }}
              >
                Register
              </button>
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

const styles = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(15, 23, 42, 0.65)',
    backdropFilter: 'blur(4px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: '20px'
  },
  modal: {
    backgroundColor: '#ffffff',
    borderRadius: '12px',
    maxWidth: '440px',
    width: '100%',
    padding: '28px',
    boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)'
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  closeBtn: {
    background: 'transparent',
    border: 'none',
    fontSize: '24px',
    cursor: 'pointer',
    color: '#64748b'
  },
  errorAlert: {
    backgroundColor: '#fef2f2',
    color: '#b91c1c',
    border: '1px solid #fecaca',
    padding: '10px 14px',
    borderRadius: '6px',
    fontSize: '13px',
    marginBottom: '12px'
  },
  quickBox: {
    backgroundColor: '#f8fafc',
    border: '1px solid #e2e8f0',
    borderRadius: '8px',
    padding: '10px 12px'
  },
  quickBtn: {
    background: '#ffffff',
    border: '1px solid #cbd5e1',
    borderRadius: '6px',
    padding: '6px 10px',
    fontSize: '12px',
    fontWeight: 500,
    cursor: 'pointer',
    color: '#0284c7'
  },
  field: {
    marginBottom: '14px',
    display: 'flex',
    flexDirection: 'column'
  },
  label: {
    fontSize: '13px',
    fontWeight: 600,
    marginBottom: '6px',
    color: '#334155'
  },
  input: {
    padding: '10px 12px',
    borderRadius: '6px',
    border: '1px solid #cbd5e1',
    fontSize: '14px',
    outline: 'none'
  },
  submitBtn: {
    width: '100%',
    padding: '12px',
    backgroundColor: '#0284c7',
    color: '#ffffff',
    border: 'none',
    borderRadius: '6px',
    fontWeight: 600,
    fontSize: '15px',
    cursor: 'pointer',
    marginTop: '6px'
  },
  linkBtn: {
    background: 'none',
    border: 'none',
    color: '#0284c7',
    fontWeight: 600,
    cursor: 'pointer',
    padding: 0
  }
};

export default LoginModal;
