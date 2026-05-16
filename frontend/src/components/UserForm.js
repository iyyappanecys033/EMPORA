import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';

const UserForm = () => {
  const [user, setUser] = useState({
    username: '',
    passwordHash: '',
    email: '',
    phone: '',
    status: 'active'
  });
  const navigate = useNavigate();
  const { id } = useParams();

  useEffect(() => {
    if (id) {
      fetchUser();
    }
  }, [id]);

  const fetchUser = async () => {
    try {
      const response = await api.get(`/users/${id}`);
      setUser(response.data);
    } catch (error) {
      console.error('Error fetching user:', error);
    }
  };

  const handleChange = (e) => {
    setUser({ ...user, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (id) {
        await api.put(`/users/${id}`, user);
      } else {
        await api.post('/users', user);
      }
      navigate('/users');
    } catch (error) {
      console.error('Error saving user:', error);
    }
  };

  return (
    <div>
      <h2>{id ? 'Edit User' : 'Add New User'}</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Username:</label>
          <input type="text" name="username" value={user.username} onChange={handleChange} required />
        </div>
        <div>
          <label>Password:</label>
          <input type="password" name="passwordHash" value={user.passwordHash} onChange={handleChange} required={!id} />
        </div>
        <div>
          <label>Email:</label>
          <input type="email" name="email" value={user.email} onChange={handleChange} required />
        </div>
        <div>
          <label>Phone:</label>
          <input type="text" name="phone" value={user.phone} onChange={handleChange} />
        </div>
        <div>
          <label>Status:</label>
          <select name="status" value={user.status} onChange={handleChange}>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </div>
        <button type="submit" className="btn btn-primary">Save</button>
        <button type="button" onClick={() => navigate('/users')} className="btn btn-secondary">Cancel</button>
      </form>
    </div>
  );
};

export default UserForm;