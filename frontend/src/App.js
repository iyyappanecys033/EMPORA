import React from 'react';
import { BrowserRouter as Router, Routes, Route, NavLink } from 'react-router-dom';
import UserList from './components/UserList';
import UserForm from './components/UserForm';
import RoleList from './components/RoleList';
import RoleForm from './components/RoleForm';
import ProfileList from './components/ProfileList';
import ProfileForm from './components/ProfileForm';
import UserRoleMapping from './components/UserRoleMapping';
import UserRoleMappingForm from './components/UserRoleMappingForm';
import Dashboard from './components/Dashboard';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <nav className="navbar">
          <div className="brand">
            <h1>EMPLOYEE PORTAL ACCOUNT MANAGEMENT</h1>
            <p>A modern user & role administration platform</p>
          </div>
          <ul>
            <li><NavLink to="/" end>Dashboard</NavLink></li>
            <li><NavLink to="/users">Users</NavLink></li>
            <li><NavLink to="/roles">Roles</NavLink></li>
            <li><NavLink to="/profiles">Profiles</NavLink></li>
            <li><NavLink to="/user-role-mappings">Mappings</NavLink></li>
          </ul>
        </nav>
        <main>
          <Routes>
            <Route path="/users" element={<UserList />} />
            <Route path="/users/new" element={<UserForm />} />
            <Route path="/users/:id/edit" element={<UserForm />} />
            <Route path="/roles" element={<RoleList />} />
            <Route path="/roles/new" element={<RoleForm />} />
            <Route path="/roles/:id/edit" element={<RoleForm />} />
            <Route path="/profiles" element={<ProfileList />} />
            <Route path="/profiles/new" element={<ProfileForm />} />
            <Route path="/profiles/:id/edit" element={<ProfileForm />} />
            <Route path="/user-role-mappings" element={<UserRoleMapping />} />
            <Route path="/user-role-mappings/new" element={<UserRoleMappingForm />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;