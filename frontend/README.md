# Frontend

This is the Vite React frontend for the ticket reservation application.

## Local development

- The frontend dev server runs on `http://127.0.0.1:5173`
- The Vite proxy forwards `/api` requests to `http://127.0.0.1:8081`
- The backend should be started from the repository root with `.\start-dev.ps1`

## Production deployment

Set `VITE_API_BASE_URL` in Vercel to the deployed backend base URL, for example:

```bash
VITE_API_BASE_URL=https://your-backend.example.com
```

If `VITE_API_BASE_URL` is empty, the frontend falls back to relative `/api` requests. That works in local development because Vite proxies those calls to the backend.
