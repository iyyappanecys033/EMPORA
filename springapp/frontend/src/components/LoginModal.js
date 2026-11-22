import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../services/AuthContext';
import Alert from './common/Alert';

const EyeIcon = ({ size = 16 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
);

const EyeOffIcon = ({ size = 16 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
    <line x1="1" y1="1" x2="23" y2="23" />
  </svg>
);

const QUICK_PROFILES = [
  { label: '🧑‍💻 John Doe (Engineer)', user: 'john_doe', pass: 'user123', role: 'Engineering' },
  { label: '👩‍💼 Priya (HR Lead)', user: 'hr_manager', pass: 'hr123', role: 'Human Resources' },
  { label: '🛡️ System Admin', user: 'admin', pass: 'admin123', role: 'Executive' }
];

const LoginModal = ({ isOpen, onClose, initialRegister = false }) => {
  const { login, register } = useAuth();
  const [isRegister, setIsRegister] = useState(initialRegister);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorObj, setErrorObj] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const userInputRef = useRef(null);
  const previousFocusRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return undefined;
    setIsRegister(Boolean(initialRegister));
    previousFocusRef.current = document.activeElement;
    setErrorObj(null);
    setShowPassword(false);
    const focusTimer = setTimeout(() => userInputRef.current?.focus(), 30);
    return () => {
      clearTimeout(focusTimer);
      if (previousFocusRef.current && typeof previousFocusRef.current.focus === 'function') {
        previousFocusRef.current.focus();
      }
    };
  }, [isOpen, initialRegister]);

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

  const handleSelectQuickProfile = (profile) => {
    setIsRegister(false);
    setUsername(profile.user);
    setPassword(profile.pass);
    setErrorObj(null);
  };

  return (
    <div
      className="ep-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="empora-auth-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !submitting) onClose();
      }}
    >
      <div className="ep-modal ep-auth-modal" style={{ maxWidth: '460px', padding: '28px 32px' }}>
        {/* Modal Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span
              className="ep-brand-mark"
              aria-hidden="true"
              style={{
                width: '38px',
                height: '38px',
                fontSize: '18px',
                background: 'linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%)',
                boxShadow: '0 4px 14px rgba(79, 70, 229, 0.35)'
              }}
            >
              E
            </span>
            <div>
              <h2 id="empora-auth-title" style={{ margin: 0, fontSize: '20px', fontWeight: 800, color: 'var(--ep-text-primary)', letterSpacing: '-0.02em' }}>
                {isRegister ? 'Create EMPORA Account' : 'Sign in to EMPORA'}
              </h2>
              <p style={{ margin: '3px 0 0', fontSize: '13px', color: 'var(--ep-text-muted)', lineHeight: 1.4 }}>
                {isRegister ? 'Register your enterprise workspace profile' : 'Welcome back to your employee services workspace'}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="ep-btn ep-btn--ghost ep-btn--sm"
            onClick={onClose}
            disabled={submitting}
            aria-label="Close sign in dialog"
            style={{ fontSize: '20px', lineHeight: 1, padding: '4px 8px', borderRadius: '8px' }}
          >
            &times;
          </button>
        </div>

        {/* Quick-Fill Employee Profiles (Sign-In Mode Only) */}
        {!isRegister && (
          <div style={{ marginBottom: '18px' }}>
            <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '8px' }}>
              ⚡ Quick Login for Employees &amp; Team
            </div>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {QUICK_PROFILES.map((p) => (
                <button
                  key={p.user}
                  type="button"
                  onClick={() => handleSelectQuickProfile(p)}
                  className="ep-btn ep-btn--ghost ep-btn--sm"
                  style={{
                    fontSize: '12px',
                    fontWeight: 600,
                    padding: '6px 10px',
                    borderRadius: '8px',
                    background: 'var(--ep-bg-subtle)',
                    border: '1px solid var(--ep-border)',
                    color: 'var(--ep-text-secondary)',
                    transition: 'all 0.2s ease'
                  }}
                  title={`Fill credentials for ${p.label}`}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Error Alert Display */}
        {errorObj && (
          <Alert
            type="error"
            title={errorObj.title}
            detail={errorObj.detail}
            errors={errorObj.errors}
            onClose={() => setErrorObj(null)}
          />
        )}

        {/* Form Fields */}
        <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '16px' }}>
          <div>
            <label htmlFor="empora-login-username" className="ep-label" style={{ marginBottom: '6px' }}>
              Username *
            </label>
            <input
              ref={userInputRef}
              id="empora-login-username"
              type="text"
              autoComplete="username"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. john_doe or admin"
              className="ep-input"
              disabled={submitting}
              style={{
                width: '100%',
                padding: '11px 14px',
                fontSize: '14px',
                borderRadius: '8px',
                transition: 'all 0.2s ease'
              }}
            />
          </div>

          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
              <label htmlFor="empora-login-password" className="ep-label" style={{ margin: 0 }}>
                Password *
              </label>
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--ep-text-muted)',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  padding: 0
                }}
                title={showPassword ? 'Hide secret' : 'Show secret'}
                aria-label={showPassword ? 'Hide' : 'Show'}
              >
                {showPassword ? <EyeOffIcon size={14} /> : <EyeIcon size={14} />}
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
            <input
              id="empora-login-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              className="ep-input"
              disabled={submitting}
              style={{
                width: '100%',
                padding: '11px 14px',
                fontSize: '14px',
                borderRadius: '8px',
                transition: 'all 0.2s ease'
              }}
            />
          </div>

          {isRegister && (
            <>
              <div>
                <label htmlFor="empora-reg-email" className="ep-label" style={{ marginBottom: '6px' }}>
                  Work Email *
                </label>
                <input
                  id="empora-reg-email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@company.com"
                  className="ep-input"
                  disabled={submitting}
                  style={{
                    width: '100%',
                    padding: '11px 14px',
                    fontSize: '14px',
                    borderRadius: '8px'
                  }}
                />
              </div>
              <div>
                <label htmlFor="empora-reg-phone" className="ep-label" style={{ marginBottom: '6px' }}>
                  Phone Number
                </label>
                <input
                  id="empora-reg-phone"
                  type="tel"
                  autoComplete="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="Optional contact number"
                  className="ep-input"
                  disabled={submitting}
                  style={{
                    width: '100%',
                    padding: '11px 14px',
                    fontSize: '14px',
                    borderRadius: '8px'
                  }}
                />
              </div>
            </>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="ep-btn ep-btn--primary ep-btn-shimmer"
            style={{
              width: '100%',
              minHeight: '44px',
              marginTop: '6px',
              fontSize: '14.5px',
              fontWeight: 700,
              borderRadius: '9px',
              boxShadow: '0 4px 14px rgba(79, 70, 229, 0.3)'
            }}
          >
            {submitting ? 'Authenticating...' : isRegister ? 'Register & Sign In' : 'Sign In'}
          </button>
        </form>

        {/* Switcher between Login and Register */}
        <div style={{ textAlign: 'center', marginTop: '18px', fontSize: '13px', color: 'var(--ep-text-muted)' }}>
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
                style={{ color: 'var(--ep-primary)', padding: '2px 6px', fontWeight: 700 }}
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
                style={{ color: 'var(--ep-primary)', padding: '2px 6px', fontWeight: 700 }}
              >
                Register
              </button>
            </span>
          )}
        </div>

        {/* Security & Trust Footer */}
        <div
          style={{
            marginTop: '20px',
            paddingTop: '16px',
            borderTop: '1px solid var(--ep-border)',
            textAlign: 'center',
            fontSize: '11.5px',
            color: 'var(--ep-text-muted)'
          }}
        >
          <span>🔒 256-Bit Encrypted Session &bull; OWASP Top 10 Protected &bull; HttpOnly JWT</span>
        </div>
      </div>
    </div>
  );
};

export default LoginModal;
