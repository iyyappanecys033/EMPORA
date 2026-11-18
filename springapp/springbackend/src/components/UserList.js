import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

const UserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [riskData, setRiskData] = useState({});

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await api.get('/users');
      setUsers(response.data);
      
      // Fetch risk for each user
      const risks = {};
      await Promise.all(response.data.map(async (user) => {
        try {
          const riskRes = await api.get(`/users/${user.id}/risk`);
          risks[user.id] = riskRes.data;
        } catch (err) {
          console.error(`Failed to fetch risk for ${user.id}`);
        }
      }));
      setRiskData(risks);
      
      setLoading(false);
    } catch (error) {
      console.error('Error fetching users:', error);
      setLoading(false);
    }
  };

  const handleAction = async (id, action) => {
    try {
      if (action === 'suspend') await api.put(`/users/${id}/suspend`);
      else if (action === 'disable') await api.put(`/users/${id}/disable`);
      else if (action === 'reactivate') await api.put(`/users/${id}/reactivate`);
      else if (action === 'unlock') await api.put(`/users/${id}/unlock`);
      
      fetchUsers();
    } catch (err) {
      console.error(`Error performing ${action}:`, err);
    }
  };

  const deleteUser = async (id) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      try {
        await api.delete(`/users/${id}`);
        fetchUsers();
      } catch (error) {
        console.error('Error deleting user:', error);
      }
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      <h2>Users</h2>
      <Link to="/users/new" className="btn btn-primary">Add New User</Link>
      <table>
        <thead>
          <tr>
            <th>Username</th>
            <th>Email</th>
            <th>Status</th>
            <th>Failed Logins</th>
            <th>Risk Level</th>
            <th>Risk Score</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {users.map(user => {
            const risk = riskData[user.id] || { riskLevel: 'UNKNOWN', score: 0 };
            return (
              <tr key={user.id}>
                <td>{user.username}</td>
                <td>{user.email}</td>
                <td>{user.status}</td>
                <td>{user.failedLoginAttempts}</td>
                <td>
                  <span className={`badge risk-${risk.riskLevel.toLowerCase()}`}>
                    {risk.riskLevel}
                  </span>
                </td>
                <td>{risk.score}</td>
                <td>
                  <Link to={`/users/${user.id}/edit`} className="btn btn-secondary" style={{marginRight: '5px'}}>Edit</Link>
                  {user.status === 'ACTIVE' && <button onClick={() => handleAction(user.id, 'suspend')} className="btn btn-warning" style={{marginRight: '5px'}}>Suspend</button>}
                  {user.status !== 'DISABLED' && <button onClick={() => handleAction(user.id, 'disable')} className="btn btn-danger" style={{marginRight: '5px'}}>Disable</button>}
                  {user.status !== 'ACTIVE' && <button onClick={() => handleAction(user.id, 'reactivate')} className="btn btn-success" style={{marginRight: '5px'}}>Reactivate</button>}
                  <button onClick={() => handleAction(user.id, 'unlock')} className="btn btn-primary" style={{marginRight: '5px'}}>Unlock</button>
                  <button onClick={() => deleteUser(user.id)} className="btn btn-danger" style={{marginRight: '5px', backgroundColor: '#b91c1c'}}>Delete</button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default UserList;