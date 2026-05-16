import React, { useEffect, useState } from 'react';
import api from '../services/api';
import './Dashboard.css';

const Dashboard = () => {
  const [stats, setStats] = useState({ users: 0, roles: 0, profiles: 0, mappings: 0 });

  useEffect(() => {
    const fetchCounts = async () => {
      try {
        const [users, roles, profiles, mappings] = await Promise.all([
          api.get('/users'),
          api.get('/roles'),
          api.get('/profiles'),
          api.get('/userRoleMappings/expanded')
        ]);
        setStats({
          users: users.data.length,
          roles: roles.data.length,
          profiles: profiles.data.length,
          mappings: mappings.data.length
        });
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
          <h3>Users</h3>
          <strong>{stats.users}</strong>
          <p>Active user accounts in the system.</p>
        </div>
        <div className="card green">
          <h3>Roles</h3>
          <strong>{stats.roles}</strong>
          <p>Defined permission roles.</p>
        </div>
        <div className="card orange">
          <h3>Profiles</h3>
          <strong>{stats.profiles}</strong>
          <p>Personal profile records.</p>
        </div>
        <div className="card purple">
          <h3>Mappings</h3>
          <strong>{stats.mappings}</strong>
          <p>User to role relationships.</p>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
