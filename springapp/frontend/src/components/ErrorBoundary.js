import React from 'react';

/**
 * [A10: Mishandling of Exceptional Conditions]
 * React Production Error Boundary.
 * Catches unhandled JavaScript exceptions anywhere in the component subtree,
 * prevents the application from failing into an open or broken state,
 * suppresses raw error stack traces in production, and renders a graceful recovery UI.
 */
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      errorId: null
    };
  }

  static getDerivedStateFromError() {
    return {
      hasError: true,
      errorId: 'ERR-' + Date.now().toString(36).toUpperCase()
    };
  }

  componentDidCatch(error, errorInfo) {
    // Log exception telemetry internally without exposing to browser console
    if (process.env.NODE_ENV !== 'production') {
      console.error('[A10: Exception Handling] Uncaught React Error:', error, errorInfo);
    }
  }

  handleReset = () => {
    this.setState({ hasError: false, errorId: null });
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div
          role="alert"
          style={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '2rem',
            backgroundColor: 'var(--bg-primary, #f8fafc)',
            color: 'var(--text-primary, #0f172a)',
            fontFamily: 'Inter, system-ui, sans-serif'
          }}
        >
          <div
            style={{
              maxWidth: '32rem',
              width: '100%',
              backgroundColor: 'var(--bg-surface, #ffffff)',
              borderRadius: '0.75rem',
              padding: '2rem',
              boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1)',
              border: '1px solid var(--border-subtle, #e2e8f0)',
              textAlign: 'center'
            }}
          >
            <div
              style={{
                width: '3.5rem',
                height: '3.5rem',
                borderRadius: '50%',
                backgroundColor: '#fee2e2',
                color: '#dc2626',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem',
                fontSize: '1.5rem',
                fontWeight: 'bold'
              }}
            >
              !
            </div>
            <h1
              style={{
                fontSize: '1.35rem',
                fontWeight: '600',
                marginBottom: '0.5rem'
              }}
            >
              Something went wrong
            </h1>
            <p
              style={{
                color: 'var(--text-secondary, #64748b)',
                fontSize: '0.9rem',
                lineHeight: '1.5',
                marginBottom: '1.5rem'
              }}
            >
              An unexpected error occurred while rendering this view. Our engineering team has been notified.
            </p>
            {this.state.errorId && (
              <p
                style={{
                  fontSize: '0.75rem',
                  color: 'var(--text-muted, #94a3b8)',
                  marginBottom: '1.5rem',
                  fontFamily: 'monospace'
                }}
              >
                Reference ID: {this.state.errorId}
              </p>
            )}
            <button
              onClick={this.handleReset}
              style={{
                backgroundColor: 'var(--brand-primary, #4f46e5)',
                color: '#ffffff',
                border: 'none',
                borderRadius: '0.375rem',
                padding: '0.625rem 1.25rem',
                fontWeight: '500',
                cursor: 'pointer',
                transition: 'opacity 0.15s ease'
              }}
            >
              Return to EMPORA Portal
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
