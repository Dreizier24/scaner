/** PM2: scanner API (FastAPI / uvicorn). Path: /opt/scaner/server */
module.exports = {
  apps: [
    {
      name: 'scanner-server',
      cwd: '/opt/scaner/server',
      script: '/opt/scaner/server/.venv/bin/uvicorn',
      args: 'app.main:app --host 127.0.0.1 --port 8020',
      interpreter: 'none',
      autorestart: true,
      max_restarts: 10,
      min_uptime: '10s',
    },
  ],
};