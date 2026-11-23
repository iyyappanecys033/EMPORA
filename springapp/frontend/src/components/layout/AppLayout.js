import React, { useState, useEffect } from 'react';
import Navbar from './Navbar';
import Sidebar from './Sidebar';
import Breadcrumb from '../common/Breadcrumb';
import GlobalSearchModal from '../common/GlobalSearchModal';
import { useAuth } from '../../services/AuthContext';

const AppLayout = ({ children, onOpenLogin }) => {
  const { isAuthenticated } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) return undefined;
    const handleGlobalKeys = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setSearchOpen((prev) => !prev);
      }
      if (e.key === 'Escape' && mobileMenuOpen) {
        setMobileMenuOpen(false);
      }
    };
    window.addEventListener('keydown', handleGlobalKeys);
    return () => window.removeEventListener('keydown', handleGlobalKeys);
  }, [mobileMenuOpen, isAuthenticated]);

  return (
    <div className="ep-shell">
      <a href="#main-content" className="ep-skip-link">
        Skip to main content
      </a>

      <Navbar
        onOpenLogin={onOpenLogin}
        onToggleMobileMenu={() => setMobileMenuOpen((prev) => !prev)}
        onOpenSearch={() => setSearchOpen(true)}
        mobileMenuOpen={mobileMenuOpen}
      />

      <div className={`ep-body ${!isAuthenticated ? 'ep-body--public' : ''}`}>
        {isAuthenticated && (
          <Sidebar
            mobileOpen={mobileMenuOpen}
            onCloseMobile={() => setMobileMenuOpen(false)}
          />
        )}

        <main
          id="main-content"
          className={`ep-main ${!isAuthenticated ? 'ep-main--full' : ''}`}
          tabIndex={-1}
          style={!isAuthenticated ? { padding: 0, width: '100%', maxWidth: '100%' } : undefined}
        >
          {isAuthenticated && <Breadcrumb />}
          {children}
        </main>
      </div>

      {isAuthenticated && <GlobalSearchModal isOpen={searchOpen} onClose={() => setSearchOpen(false)} />}
    </div>
  );
};

export default AppLayout;
