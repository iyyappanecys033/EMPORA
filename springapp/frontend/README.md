# SpringApp Frontend

This is the React frontend for the SpringApp project.

## Getting Started

1. Install dependencies:
   ```
   npm install
   ```

2. Start the development server:
   ```
   npm start
   ```

The app will run on `http://localhost:3000` and proxy API requests to `http://localhost:8080`.

## Features

- User management (CRUD)
- Role management (CRUD)
- Profile management (CRUD)
- User-Role mapping display

## API Integration

The frontend communicates with the Spring Boot backend via REST APIs:
- `/api/users`
- `/api/roles`
- `/api/profiles`
- `/api/user-role-mappings`