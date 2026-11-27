import React from 'react';
import '@testing-library/jest-dom';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import api from './services/api';
import UserForm from './components/UserForm';
import ProfileForm from './components/ProfileForm';
import RoleForm from './components/RoleForm';
import UserRoleMappingForm from './components/UserRoleMappingForm';

jest.mock('./services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn()
  }
}));

const renderRoute = (path, route, element) => render(
  <MemoryRouter initialEntries={[path]}>
    <Routes>
      <Route path={route} element={element} />
    </Routes>
  </MemoryRouter>
);

const submitForm = (container) => fireEvent.submit(container.querySelector('form'));
const submitAndFlush = async (container) => {
  await act(async () => {
    submitForm(container);
    await Promise.resolve();
  });
};

describe('P96 legacy administration forms', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('UserForm requires username, email, and create password; submits optional phone and selected status', async () => {
    api.post.mockResolvedValueOnce({ data: { id: 8 } });
    const { container } = renderRoute('/users/new', '/users/new', <UserForm />);

    const username = container.querySelector('input[name="username"]');
    const password = container.querySelector('input[name="passwordHash"]');
    const email = container.querySelector('input[name="email"]');
    const phone = container.querySelector('input[name="phone"]');
    const status = container.querySelector('select[name="status"]');
    expect(username).toBeRequired();
    expect(password).toBeRequired();
    expect(email).toBeRequired();
    expect(phone).not.toBeRequired();
    expect(email).toHaveAttribute('type', 'email');
    expect(Array.from(status.options).map((option) => option.value)).toEqual(['active', 'inactive']);

    fireEvent.change(username, { target: { value: 'qa.user.01' } });
    fireEvent.change(password, { target: { value: 'QA-test-only-Password!' } });
    fireEvent.change(email, { target: { value: 'qa.user.01@example.test' } });
    fireEvent.change(phone, { target: { value: '+1 202 555 0101' } });
    fireEvent.change(status, { target: { value: 'inactive' } });
    await submitAndFlush(container);

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/users', {
      username: 'qa.user.01',
      passwordHash: 'QA-test-only-Password!',
      email: 'qa.user.01@example.test',
      phone: '+1 202 555 0101',
      status: 'inactive'
    }));
  });

  test('UserForm makes password optional while editing an existing user', async () => {
    api.get.mockResolvedValueOnce({
      data: { username: 'qa.user.02', passwordHash: '', email: 'qa.user.02@example.test', phone: '', status: 'active' }
    });
    const { container } = renderRoute('/users/2/edit', '/users/:id/edit', <UserForm />);

    await waitFor(() => expect(screen.getByDisplayValue('qa.user.02')).toBeInTheDocument());
    expect(container.querySelector('input[name="passwordHash"]')).not.toBeRequired();
  });

  test('ProfileForm requires linked user, first name, last name, and email; preserves optional Unicode fields', async () => {
    api.get.mockResolvedValueOnce({ data: [{ id: 12, username: 'qa.employee.12' }] });
    api.post.mockResolvedValueOnce({ data: { id: 15 } });
    const { container } = renderRoute('/profiles/new', '/profiles/new', <ProfileForm />);

    await waitFor(() => expect(container.querySelector('select[name="userId"] option[value="12"]')).toBeInTheDocument());
    const linkedUser = container.querySelector('select[name="userId"]');
    const firstName = container.querySelector('input[name="firstName"]');
    const lastName = container.querySelector('input[name="lastName"]');
    const email = container.querySelector('input[name="email"]');
    const phone = container.querySelector('input[name="phone"]');
    const dob = container.querySelector('input[name="dob"]');
    const gender = container.querySelector('select[name="gender"]');
    const address = container.querySelector('textarea[name="address"]');

    [linkedUser, firstName, lastName, email].forEach((field) => expect(field).toBeRequired());
    expect(phone).not.toBeRequired();
    expect(dob).toHaveAttribute('type', 'date');
    expect(Array.from(gender.options).map((option) => option.value)).toEqual(['female', 'male', 'other']);

    fireEvent.change(linkedUser, { target: { value: '12' } });
    fireEvent.change(firstName, { target: { value: 'Zoë 東京 😀' } });
    fireEvent.change(lastName, { target: { value: "O'Neil" } });
    fireEvent.change(email, { target: { value: 'qa.profile@example.test' } });
    fireEvent.change(phone, { target: { value: '+1 202 555 0101' } });
    fireEvent.change(dob, { target: { value: '1990-02-28' } });
    fireEvent.change(gender, { target: { value: 'other' } });
    fireEvent.change(address, { target: { value: '12 QA Street\n東京' } });
    await submitAndFlush(container);

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/profiles', {
      firstName: 'Zoë 東京 😀',
      lastName: "O'Neil",
      email: 'qa.profile@example.test',
      phone: '+1 202 555 0101',
      address: '12 QA Street\n東京',
      dob: '1990-02-28',
      gender: 'other',
      user: { id: '12' }
    }));
  });

  test('RoleForm requires role name and permits an optional multiline description', async () => {
    api.post.mockResolvedValueOnce({ data: { id: 4 } });
    const { container } = renderRoute('/roles/new', '/roles/new', <RoleForm />);
    const roleName = container.querySelector('input[name="roleName"]');
    const description = container.querySelector('textarea[name="description"]');

    expect(roleName).toBeRequired();
    expect(description).not.toBeRequired();
    fireEvent.change(roleName, { target: { value: 'QA Resolver' } });
    fireEvent.change(description, { target: { value: 'Synthetic QA role\nUsed only for test runs.' } });
    await submitAndFlush(container);

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/roles', {
      roleName: 'QA Resolver',
      description: 'Synthetic QA role\nUsed only for test runs.'
    }));
  });

  test('UserRoleMappingForm rejects empty selections and submits only the selected user-role pair', async () => {
    api.get
      .mockResolvedValueOnce({ data: [{ id: 7, username: 'qa.employee.07' }] })
      .mockResolvedValueOnce({ data: [{ id: 3, roleName: 'QA_RESOLVER' }] });
    api.post.mockResolvedValueOnce({ data: { id: 20 } });
    const { container } = renderRoute('/user-role-mappings/new', '/user-role-mappings/new', <UserRoleMappingForm />);

    await waitFor(() => expect(container.querySelector('select[name="userId"] option[value="7"]')).toBeInTheDocument());
    const user = container.querySelector('select[name="userId"]');
    const role = container.querySelector('select[name="roleId"]');
    expect(user).toBeRequired();
    expect(role).toBeRequired();

    await submitAndFlush(container);
    expect(api.post).not.toHaveBeenCalled();

    fireEvent.change(user, { target: { value: '7' } });
    fireEvent.change(role, { target: { value: '3' } });
    await submitAndFlush(container);

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/userRoleMappings', {
      user: { id: '7' },
      role: { id: '3' }
    }));
    expect(api.post).toHaveBeenCalledTimes(1);
  });
});
