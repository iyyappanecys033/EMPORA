import React from 'react';
import Navbar from './Navbar';
import Sidebar from './Sidebar';

const AppLayout = ({ children, onOpenLogin }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', backgroundColor: '#f8fafc' }}>
      <Navbar onOpenLogin={onOpenLogin} />
      <div style={{ display: 'flex', flex: 1 }}>
        <Sidebar />
        <main
          style={{
            flex: 1,
            padding: '24px 32px',
            maxWidth: '1400px',
            width: '100%',
            margin: '0 auto',
            boxSizing: 'border-box'
          }}
        >
          {children}
        </main>
      </div>
    </div>
  );
};

export default AppLayout;

