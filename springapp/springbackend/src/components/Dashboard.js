import React, { useEffect, useState } from 'react';
import api from '../services/api';
import './Dashboard.css';

const Dashboard = () => {
  const [stats, setStats] = useState({ 
    totalUsers: 0, 
    activeUsers: 0, 
    suspendedUsers: 0, 
    disabledUsers: 0, 
    lockedUsers: 0,
    highRiskUsers: 0,
    criticalRiskUsers: 0,
    totalAuditEvents: 0
  });

  useEffect(() => {
    const fetchCounts = async () => {
      try {
        const response = await api.get('/dashboard/metrics');
        setStats(response.data);
      } catch (error) {
        console.error('Unable to fetch dashboard stats', error);
      }
    };

    fetchCounts();
  }, []);

  return (
    <div className="dashboard">
      <h2>Administration Dashboard</h2>
      <div className="cards">
        <div className="card blue">
          <h3>Total Users</h3>
          <strong>{stats.totalUsers}</strong>
        </div>
        <div className="card green">
          <h3>Active</h3>
          <strong>{stats.activeUsers}</strong>
        </div>
        <div className="card orange">
          <h3>Locked</h3>
          <strong>{stats.lockedUsers}</strong>
        </div>
        <div className="card red">
          <h3>Suspended / Disabled</h3>
          <strong>{stats.suspendedUsers + stats.disabledUsers}</strong>
        </div>
      </div>
      
      <div className="cards" style={{marginTop: '20px'}}>
        <div className="card purple">
          <h3>High Risk Users</h3>
          <strong>{stats.highRiskUsers}</strong>
        </div>
        <div className="card red">
          <h3>Critical Risk Users</h3>
          <strong>{stats.criticalRiskUsers}</strong>
        </div>
        <div className="card gray">
          <h3>Total Audit Events</h3>
          <strong>{stats.totalAuditEvents}</strong>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
