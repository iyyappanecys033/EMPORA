import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../services/AuthContext';
import Alert from './common/Alert';

const LoginModal = ({ isOpen, onClose }) => {
  const { login, register } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [errorObj, setErrorObj] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const userInputRef = useRef(null);
  const previousFocusRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return undefined;
    previousFocusRef.current = document.activeElement;
    setErrorObj(null);
    const focusTimer = setTimeout(() => userInputRef.current?.focus(), 20);
    return () => {
      clearTimeout(focusTimer);
      if (previousFocusRef.current && typeof previousFocusRef.current.focus === 'function') {
        previousFocusRef.current.focus();
      }
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape' && !submitting) onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('keydown', onKey);
    };
  }, [isOpen, submitting, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorObj(null);
    setSubmitting(true);
    try {
      if (isRegister) {
        await register({ username: username.trim(), password, email: email.trim(), phone: phone.trim() });
      } else {
        await login(username.trim(), password);
      }
      setPassword('');
      onClose();
    } catch (err) {
      const detail =
        err?.detail ||
        err?.response?.data?.detail ||
        err?.response?.data?.message ||
        (isRegister ? 'Registration failed. Please verify your details.' : 'Invalid username or password.');
      setErrorObj({
        title: err?.title || (isRegister ? 'Registration Failed' : 'Authentication Failed'),
        detail: typeof detail === 'string' ? detail : JSON.stringify(detail),
        errors: err?.errors || []
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="ep-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="epams-auth-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !submitting) onClose();
      }}
    >
      <div className="ep-modal" style={{ maxWidth: '440px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span className="ep-brand-mark" aria-hidden="true">E</span>
            <div>
              <h2 id="epams-auth-title" style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: 'var(--ep-text-primary)' }}>
                {isRegister ? 'Create EPAMS Account' : 'Sign in to EPAMS'}
              </h2>
              <p style={{ margin: '2px 0 0', fontSize: '12px', color: 'var(--ep-text-muted)' }}>
                {isRegister ? 'Register your enterprise workspace profile' : 'Enterprise Employee Lifecycle & Service Portal'}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="ep-btn ep-btn--ghost ep-btn--sm"
            onClick={onClose}
            disabled={submitting}
            aria-label="Close sign in dialog"
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

        <form onSubmit={handleSubmit} style={{ marginTop: '18px', display: 'grid', gap: '14px' }}>
          <div>
            <label htmlFor="epams-login-username" className="ep-label">Username *</label>
            <input
              ref={userInputRef}
              id="epams-login-username"
              type="text"
              autoComplete="username"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your corporate username"
              className="ep-input"
              disabled={submitting}
            />
          </div>

          <div>
            <label htmlFor="epams-login-password" className="ep-label">Password *</label>
            <input
              id="epams-login-password"
              type="password"
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              className="ep-input"
              disabled={submitting}
            />
          </div>

          {isRegister && (
            <>
              <div>
                <label htmlFor="epams-reg-email" className="ep-label">Work Email *</label>
                <input
                  id="epams-reg-email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@company.com"
                  className="ep-input"
                  disabled={submitting}
                />
              </div>
              <div>
                <label htmlFor="epams-reg-phone" className="ep-label">Phone Number</label>
                <input
                  id="epams-reg-phone"
                  type="tel"
                  autoComplete="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="Optional contact number"
                  className="ep-input"
                  disabled={submitting}
                />
              </div>
            </>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="ep-btn ep-btn--primary"
            style={{ width: '100%', minHeight: '42px', marginTop: '4px' }}
          >
            {submitting ? 'Authenticating...' : isRegister ? 'Register & Sign In' : 'Sign In'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '16px', fontSize: '13px', color: 'var(--ep-text-muted)' }}>
          {isRegister ? (
            <span>
              Already have an account?{' '}
              <button
                type="button"
                className="ep-btn ep-btn--ghost ep-btn--sm"
                onClick={() => {
                  setIsRegister(false);
                  setErrorObj(null);
                }}
                style={{ color: 'var(--ep-primary)', padding: '2px 6px' }}
              >
                Sign In
              </button>
            </span>
          ) : (
            <span>
              Need a new workspace account?{' '}
              <button
                type="button"
                className="ep-btn ep-btn--ghost ep-btn--sm"
                onClick={() => {
                  setIsRegister(true);
                  setErrorObj(null);
                }}
                style={{ color: 'var(--ep-primary)', padding: '2px 6px' }}
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

export default LoginModal;
