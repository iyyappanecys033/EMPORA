import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

const ProfileList = () => {
  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProfiles();
  }, []);

  const fetchProfiles = async () => {
    try {
      const response = await api.get('/profiles');
      setProfiles(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching profiles:', error);
      setLoading(false);
    }
  };

  const deleteProfile = async (id) => {
    if (window.confirm('Are you sure you want to delete this profile?')) {
      try {
        await api.delete(`/profiles/${id}`);
        fetchProfiles();
      } catch (error) {
        console.error('Error deleting profile:', error);
      }
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      <h2>Profiles</h2>
      <Link to="/profiles/new" className="btn btn-primary">Add New Profile</Link>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
            <th>First Name</th>
            <th>Last Name</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {profiles.map(profile => (
            <tr key={profile.id}>
              <td>{profile.id}</td>
              <td>{profile.user?.username || 'Unlinked'}</td>
              <td>{profile.firstName}</td>
              <td>{profile.lastName}</td>
              <td>{profile.email}</td>
              <td>{profile.phone}</td>
              <td>
                <Link to={`/profiles/${profile.id}/edit`} className="btn btn-secondary">Edit</Link>
                <button onClick={() => deleteProfile(profile.id)} className="btn btn-danger">Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default ProfileList;