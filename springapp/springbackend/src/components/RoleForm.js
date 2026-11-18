import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';

const RoleForm = () => {
  const [role, setRole] = useState({
    roleName: '',
    description: ''
  });
  const navigate = useNavigate();
  const { id } = useParams();

  useEffect(() => {
    if (id) {
      const fetchRole = async () => {
        try {
          const response = await api.get(`/roles/${id}`);
          setRole(response.data);
        } catch (error) {
          console.error('Error fetching role:', error);
        }
      };
      fetchRole();
    }
  }, [id]);

  const handleChange = (e) => {
    setRole({ ...role, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (id) {
        await api.put(`/roles/${id}`, role);
      } else {
        await api.post('/roles', role);
      }
      navigate('/roles');
    } catch (error) {
      console.error('Error saving role:', error);
    }
  };

  return (
    <div>
      <h2>{id ? 'Edit Role' : 'Add New Role'}</h2>
      <form onSubmit={handleSubmit} className="admin-form">
        <div>
          <label>Role Name:</label>
          <input type="text" name="roleName" value={role.roleName} onChange={handleChange} required />
        </div>
        <div>
          <label>Description:</label>
          <textarea name="description" value={role.description} onChange={handleChange} />
        </div>
        <button type="submit" className="btn btn-primary">Save</button>
        <button type="button" onClick={() => navigate('/roles')} className="btn btn-secondary">Cancel</button>
      </form>
    </div>
  );
};

export default RoleForm;
