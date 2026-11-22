import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

const UserRoleMapping = () => {
  const [mappings, setMappings] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMappings();
  }, []);

  const fetchMappings = async () => {
    try {
      const response = await api.get('/userRoleMappings/expanded');
      setMappings(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching mappings:', error);
      setLoading(false);
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      <div className="list-header">
        <h2>User Role Mappings</h2>
        <Link to="/user-role-mappings/new" className="btn btn-primary">Create Mapping</Link>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>User Name</th>
            <th>Role Name</th>
          </tr>
        </thead>
        <tbody>
          {mappings.map(mapping => (
            <tr key={mapping.id}>
              <td>{mapping.id}</td>
              <td>{mapping.username}</td>
              <td>{mapping.roleName}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default UserRoleMapping;