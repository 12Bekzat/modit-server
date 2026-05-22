# Deployment security checklist

Set these values as hosting environment variables. Do not commit real secrets to Git.

Required backend variables:

```properties
DB_URL=jdbc:postgresql://HOST:5432/DB_NAME
DB_USERNAME=...
DB_PASSWORD=...
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
APP_JWT_SECRET=32+ random bytes, unique for production
APP_ADMIN_EMAIL=...
APP_ADMIN_PASSWORD=long random password
APP_ADMIN_FULL_NAME=...
APP_ADMIN_PHONE=...
```

Optional backend variables:

```properties
PORT=8081
APP_JWT_ISSUER=magazine-server
APP_JWT_ACCESS_TOKEN_TTL=PT12H
```

Never put database passwords, JWT secrets, admin passwords, API access tokens, or hosting provider tokens in source code, GitHub, or frontend `REACT_APP_*` variables.

Generate production secrets locally, then paste them only into the hosting provider's secret/environment settings:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```
