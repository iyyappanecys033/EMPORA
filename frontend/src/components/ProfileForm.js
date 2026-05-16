import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';

const ProfileForm = () => {
  const [users, setUsers] = useState([]);
  const [profile, setProfile] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    dob: '',
    gender: 'female',
    userId: ''
  });
  const navigate = useNavigate();
  const { id } = useParams();

  useEffect(() => {
    fetchUsers();
    if (id) {
      fetchProfile();
    }
  }, [id]);

  const fetchUsers = async () => {
    try {
      const response = await api.get('/users');
      setUsers(response.data);
    } catch (error) {
      console.error('Error fetching users:', error);
    }
  };

  const fetchProfile = async () => {
    try {
      const response = await api.get(`/profiles/${id}`);
      const data = response.data;
      setProfile({
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        email: data.email || '',
        phone: data.phone || '',
        address: data.address || '',
        dob: data.dob || '',
        gender: data.gender || 'female',
        userId: data.user?.id || ''
      });
    } catch (error) {
      console.error('Error fetching profile:', error);
    }
  };

  const handleChange = (e) => {
    setProfile({ ...profile, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      firstName: profile.firstName,
      lastName: profile.lastName,
      email: profile.email,
      phone: profile.phone,
      address: profile.address,
      dob: profile.dob,
      gender: profile.gender,
      user: profile.userId ? { id: profile.userId } : null
    };

    try {
      if (id) {
        await api.put(`/profiles/${id}`, payload);
      } else {
        await api.post('/profiles', payload);
      }
      navigate('/profiles');
    } catch (error) {
      console.error('Error saving profile:', error);
    }
  };

  return (
    <div>
      <h2>{id ? 'Edit Profile' : 'Add New Profile'}</h2>
      <form onSubmit={handleSubmit} className="profile-form">
        <div>
          <label>Linked User</label>
          <select name="userId" value={profile.userId} onChange={handleChange} required>
            <option value="">Select user</option>
            {users.map((user) => (
              <option key={user.id} value={user.id}>{user.username}</option>
            ))}
          </select>
        </div>
        <div>
          <label>First Name:</label>
          <input type="text" name="firstName" value={profile.firstName} onChange={handleChange} required />
        </div>
        <div>
          <label>Last Name:</label>
          <input type="text" name="lastName" value={profile.lastName} onChange={handleChange} required />
        </div>
        <div>
          <label>Email:</label>
          <input type="email" name="email" value={profile.email} onChange={handleChange} required />
        </div>
        <div>
          <label>Phone:</label>
          <input type="text" name="phone" value={profile.phone} onChange={handleChange} />
        </div>
        <div>
          <label>Date of Birth:</label>
          <input type="date" name="dob" value={profile.dob} onChange={handleChange} />
        </div>
        <div>
          <label>Gender:</label>
          <select name="gender" value={profile.gender} onChange={handleChange}>
            <option value="female">Female</option>
            <option value="male">Male</option>
            <option value="other">Other</option>
          </select>
        </div>
        <div>
          <label>Address:</label>
          <textarea name="address" value={profile.address} onChange={handleChange} />
        </div>
        <button type="submit" className="btn btn-primary">Save</button>
        <button type="button" onClick={() => navigate('/profiles')} className="btn btn-secondary">Cancel</button>
      </form>
    </div>
  );
};

export default ProfileForm;