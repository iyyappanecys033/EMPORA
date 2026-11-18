import React, { useEffect, useState } from 'react';
import api from '../services/api';

const AuditLogs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    try {
      const response = await api.get('/audit');
      setLogs(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching audit logs:', error);
      setLoading(false);
    }
  };

  if (loading) return <div>Loading Audit Logs...</div>;

  return (
    <div>
      <h2>Audit Logs</h2>
      <table>
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Actor</th>
            <th>Action</th>
            <th>Target</th>
            <th>Details</th>
          </tr>
        </thead>
        <tbody>
          {logs.length === 0 ? (
            <tr>
              <td colSpan="5" style={{ textAlign: 'center', color: '#64748b', padding: '24px' }}>
                No audit logs recorded yet.
              </td>
            </tr>
          ) : (
            logs.map(log => (
              <tr key={log.id}>
                <td>{new Date(log.timestamp).toLocaleString()}</td>
                <td>{log.actorUsername}</td>
                <td>{log.action}</td>
                <td>{log.targetUsername}</td>
                <td>{log.details}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};

export default AuditLogs;
