import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const UserRoleMappingForm = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [mapping, setMapping] = useState({ userId: '', roleId: '' });
  const navigate = useNavigate();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [userResponse, roleResponse] = await Promise.all([
          api.get('/users'),
          api.get('/roles')
        ]);
        setUsers(userResponse.data);
        setRoles(roleResponse.data);
      } catch (error) {
        console.error('Error loading users or roles', error);
      }
    };

    fetchData();
  }, []);

  const handleChange = (e) => {
    setMapping({ ...mapping, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!mapping.userId || !mapping.roleId) {
      return;
    }

    try {
      await api.post('/userRoleMappings', {
        user: { id: mapping.userId },
        role: { id: mapping.roleId }
      });
      navigate('/user-role-mappings');
    } catch (error) {
      console.error('Error creating user role mapping', error);
    }
  };

  return (
    <div>
      <h2>Create User Role Mapping</h2>
      <form onSubmit={handleSubmit} className="mapping-form">
        <div>
          <label htmlFor="userId">User</label>
          <select name="userId" id="userId" value={mapping.userId} onChange={handleChange} required>
            <option value="">Select user</option>
            {users.map(user => (
              <option key={user.id} value={user.id}>{user.username}</option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="roleId">Role</label>
          <select name="roleId" id="roleId" value={mapping.roleId} onChange={handleChange} required>
            <option value="">Select role</option>
            {roles.map(role => (
              <option key={role.id} value={role.id}>{role.roleName}</option>
            ))}
          </select>
        </div>
        <button type="submit" className="btn btn-primary">Save Mapping</button>
        <button type="button" className="btn btn-secondary" onClick={() => navigate('/user-role-mappings')}>Cancel</button>
      </form>
    </div>
  );
};

export default UserRoleMappingForm;
