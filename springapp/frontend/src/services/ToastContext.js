import React, { createContext, useContext, useState, useCallback, useRef } from 'react';

const ToastContext = createContext({
  showToast: () => {}
});

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);
  const idRef = useRef(1);

  const dismissToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    ({ type = 'info', title, message, duration = 4500 }) => {
      const id = idRef.current++;
      setToasts((prev) => [...prev.slice(-3), { id, type, title, message }]);
      if (duration > 0) {
        setTimeout(() => {
          setToasts((prev) => prev.filter((t) => t.id !== id));
        }, duration);
      }
      return id;
    },
    []
  );

  return (
    <ToastContext.Provider value={{ showToast, dismissToast }}>
      {children}
      <div className="ep-toast-stack" role="status" aria-live="polite" aria-atomic="false">
        {toasts.map((t) => (
          <div key={t.id} className={`ep-toast ep-toast--${t.type}`}>
            <div style={{ minWidth: 0 }}>
              {t.title && (
                <div style={{ fontWeight: 700, fontSize: '13px', color: 'var(--ep-text-primary)' }}>
                  {t.title}
                </div>
              )}
              {t.message && (
                <div style={{ fontSize: '12px', color: 'var(--ep-text-secondary)', marginTop: '2px' }}>
                  {t.message}
                </div>
              )}
            </div>
            <button
              type="button"
              onClick={() => dismissToast(t.id)}
              aria-label="Dismiss notification"
              className="ep-btn ep-btn--ghost ep-btn--sm"
              style={{ padding: '2px 6px', minHeight: '24px' }}
            >
              &times;
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = () => useContext(ToastContext);
export default ToastContext;
