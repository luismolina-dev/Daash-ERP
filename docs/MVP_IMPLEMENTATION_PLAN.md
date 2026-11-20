# Plan de implementación MVP — Daash ERP & DTE

## Arquitectura acordada

- Frontend: React + TypeScript + Vite + Zustand (existente en `prototipov2/src`).
- Backend: Java 25 compatible con Spring Boot 3.5+, Maven, PostgreSQL 16.
- DTE: simulación formal con contrato JSON v1.4, firma JWT HS256 y `MockHaciendaClient`.
- Seguridad: JWT de 15 min + refresh cookie httpOnly, BCrypt, RBAC por endpoint, rate limiting, cabeceras de seguridad, auditoría.
- Evidencia: tests JUnit/MockMvc, Docker Compose, CI con Maven/npm/Semgrep/Trivy.

## Usuarios semilla

| Email | Password | Rol |
|---|---|---|
| maria@adventureworks.com | Demo1234! | cajero |
| carlos@adventureworks.com | Demo1234! | admin_bodega |
| ana@adventureworks.com | Demo1234! | gerente |
| roberto@adventureworks.com | Demo1234! | auditor |

> Las contraseñas se almacenan con BCrypt en PostgreSQL. El login rápido del frontend debe rellenar el email, pero **nunca** mostrar la contraseña.

## Variables de entorno

Copiar `.env.example` a `.env`.

| Variable | Default | Uso |
|---|---|---|
| `POSTGRES_DB` | `daash` | Base de datos |
| `POSTGRES_USER` | `daash` | Usuario |
| `POSTGRES_PASSWORD` | `daash_secret` | Contraseña |
| `POSTGRES_PORT` | `5432` | Puerto host |
| `JWT_SECRET` | cambiar en producción | Firma de access tokens |
| `REFRESH_SECRET` | cambiar en producción | Firma de refresh tokens |
| `DTE_SIGNING_SECRET` | cambiar en producción | Firma simulada del DTE |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | CORS |
| `SERVER_PORT` | `8080` | Puerto API |
| `SSL_ENABLED` | `false` | Habilita TLS en el backend |

## Puertos

- Frontend: `5173`
- Backend: `8080`
- PostgreSQL: `5432`

## Propiedad de archivos

- Backend: `backend/**`, `docker-compose.yml`, `.env.example`.
- Frontend: `src/**`, `index.html`, `vite.config.ts`, `package.json`.
- DevSecOps/documentación: `.github/**`, `docs/**` excepto este archivo, `README.md`, `scripts/**`, `semgrep.yml`, `.gitignore`.

## Criterios de aceptación verificables

1. `./mvnw verify` pasa con tests de seguridad y reglas de negocio.
2. `npm run build` y `npm run lint` pasan sin errores.
3. Un cajero no puede consultar `/api/reports/**` (403).
4. Un token modificado es rechazado (401).
5. Un payload de venta con precio alterado es recalculado por el servidor.
6. Caída simulada de Hacienda genera venta en contingencia y fila de reintento.
7. El job de reintento cambia la venta a `aceptado` y guarda sello UUID.
8. No existen credenciales hardcodeadas en el frontend.
9. Cabeceras CSP, HSTS y X-Frame-Options presentes.
10. `docker compose up --build` levanta PostgreSQL, backend y frontend.
