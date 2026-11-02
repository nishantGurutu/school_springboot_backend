# Dashboard CORS on Render

The current dashboard origin is `https://school-dashboard-eight-kappa.vercel.app`.
Spring Security uses `app.cors.allowed-origins` for preflight and API responses.

On the Render backend service, set `APP_CORS_ALLOWED_ORIGINS` to the comma-separated origins allowed to call the backend:

```text
http://localhost:3000,http://127.0.0.1:3000,https://school-dashboard-one-sepia.vercel.app,https://school-dashboard-tawny-six.vercel.app,https://school-dashboard-eight-kappa.vercel.app
```

Save and redeploy/restart the backend service. Environment variables override the source defaults, so updating only the Java/properties default does not change a previously configured Render environment variable.

Origins must contain only the scheme and hostname (and port, if present). Do not add a trailing slash, `/api`, or a page path. Add each new production/custom domain explicitly.

Verify after deployment:

```sh
curl -i -X OPTIONS 'https://school-springboot-backend.onrender.com/api/auth/login' \
  -H 'Origin: https://school-dashboard-eight-kappa.vercel.app' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type'
```

Expect HTTP 200 and `Access-Control-Allow-Origin: https://school-dashboard-eight-kappa.vercel.app`. Before this fix, this request returned HTTP 403 with `Invalid CORS request`; the older Vercel origin succeeded. The public runtime configuration already points to the correct HTTPS Render API. No dashboard API-URL change or duplicate login API is needed for this error.

Regression tests: `./mvnw -Dtest=DashboardCorsTest test`.

Redeploy the same Vercel project to preserve its production domain. Creating a new project changes the origin and requires a new explicit CORS entry. Updating Vercel alone does not deploy Spring Boot source changes to Render.
