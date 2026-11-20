# Validación de seguridad del MVP Daash ERP & DTE

Este documento registra el estado **real** de los controles del MVP al cierre de la integración. Distingue entre evidencia ejecutada localmente, evidencia pendiente y alcance de la simulación DTE.

## 1. Estados usados

| Estado | Significado |
|---|---|
| **Verificado en entorno local** | Control implementado y comprobado con build, test, petición HTTP o escaneo en este entorno. |
| **Pendiente de ZAP autenticado** | Control implementado, pero requiere DAST con sesión válida para darlo por cerrado. |
| **Pendiente de prueba de carga** | Control implementado, pero no se han medido latencias p95 en entorno desplegado. |
| **Simulación / No aplicable** | Parte del contrato MVP no dependiente de una integración externa real. |

## 2. Matriz de casos de abuso

| Caso | Amenaza | Control implementado | Evidencia en código | Cómo verificarlo | Estado |
|---|---|---|---|---|---|
| **CA-01** | Inyección SQL en búsquedas de clientes/productos | Acceso a datos con JPA/Hibernate y repositorios parametrizados; sin concatenación de SQL; regla SAST `sql-query-string-concatenation`. | `backend/src/main/java/com/daash/erp/repository/*.java`, `backend/src/main/java/com/daash/erp/service/*.java`, `backend/src/main/java/com/daash/erp/controller/*.java`, `semgrep.yml`. | 1) `cd backend && ./mvnw verify` (12 tests). 2) `semgrep scan --config semgrep.yml ...` → 0 hallazgos. 3) `curl -G --data-urlencode "search=' OR 1=1--" http://localhost:5173/api/products` → sin error SQL. | **Verificado en entorno local** (SAST + tests + repositorios); queda ZAP autenticado como refuerzo. |
| **CA-02** | Escalada de privilegios alterando claims/firma del JWT | Access token HS256 de 15 min firmado con `JWT_SECRET`; refresh token en cookie httpOnly firmado con `REFRESH_SECRET`; RBAC por endpoint; BCrypt; token en memoria en el cliente. | `backend/src/main/java/com/daash/erp/security/JwtAuthenticationFilter.java`, `.../config/SecurityConfig.java`, `.../service/AuthService.java`, `src/lib/api.ts`, `src/lib/roles.ts`, `src/App.tsx`, `src/components/Layout.tsx`. | 1) Modificar token → `401`. 2) Cajero → `GET /api/reports/summary` = `403`; gerente = `200` (comprobado en vivo). 3) `localStorage`/`sessionStorage` sin tokens. 4) Semgrep sin `jwt-parsed-without-signature-verification`. | **Verificado en entorno local**; queda ZAP autenticado como refuerzo. |
| **CA-03** | XSS almacenado en descripción de productos | React escapa HTML, sin `dangerouslySetInnerHTML`, JSON sin HTML, validación bean en DTO y cabecera CSP. | `src/**/*.tsx`, `src/lib/api.ts`, `backend/src/main/java/com/daash/erp/dto/*.java`, `backend/src/main/java/com/daash/erp/config/SecurityHeadersConfig.java`, `semgrep.yml`. | 1) `grep -RIn dangerouslySetInnerHTML src --include='*.tsx'` → sin resultados. 2) Semgrep → 0 hallazgos. 3) `curl -I http://localhost:5173/api/health` → cabeceras de seguridad. | **Verificado en entorno local** (frontend + SAST + cabeceras); queda ZAP autenticado extremo a extremo. |
| **CA-04** | Fuerza bruta/DDoS contra `/auth/login` | 5 intentos fallidos por IP en 15 min → `429`; bloqueo temporal de cuenta; BCrypt. | `backend/src/main/java/com/daash/erp/security/RateLimitFilter.java`, `.../service/AuthService.java`, `.../config/SecurityConfig.java`, `docs/API_CONTRACT.md`. | 1) `for i in {1..6}; do curl ... -d '{"email":"maria@adventureworks.com","password":"wrong"}'; done` → 6º `429`. 2) Test `AuthSecurityTest`. 3) Prueba de carga controlada. | **Verificado en entorno local** (tests + filtro); queda prueba de carga / ZAP. No se afirma protección de gateway externo. |
| **CA-05** | Manipulación del payload POS para alterar precios | Servidor ignora `precio`, `subtotal`, `iva` y `total`; recalcula desde `products.precio` con IVA 13%; descuento > 10% solo gerente; auditoría de cada venta. | `backend/src/main/java/com/daash/erp/service/SaleService.java`, `.../dto/SaleDtos.java`, `.../controller/SaleController.java`, `src/stores/useStore.ts`, `docs/API_CONTRACT.md`. | 1) Test de recálculo en `./mvnw verify`. 2) Comprobado en vivo: payload con `precioUnitario=0.01` → servidor guardó `459.99`. | **Verificado en entorno local**; queda interceptación con ZAP como refuerzo. |

## 3. Requerimientos no funcionales

| Requisito | Estado | Evidencia |
|---|---|---|
| **RNF-01** Contenedores | **Verificado en entorno local** | `docker compose up -d --build` levantó `daash-postgres`, `daash-backend` y `daash-frontend` en estado `healthy`. SPA con fallback en Nginx. |
| **RNF-02** Latencia ≤ 300 ms | **Pendiente de prueba de carga** | Existe el recálculo en servidor, pero no se ha medido p95 con herramienta de carga. |
| **RNF-03** Disponibilidad y contingencia | **Verificado en entorno local (simulación)** | `ContingencyQueue`, `MockHaciendaClient`, job cada 5 min y tests `DteContingencyTest`. No es integración real con Hacienda. |
| **RNF-04** HTTPS/TLS 1.2+ | **Pendiente en despliegue** | Cabeceras HSTS y bandera `SSL_ENABLED` previstas; en local se usa HTTP por diseño. Requiere terminal TLS/keystore en producción. |
| **RNF-05** Respaldos y retención 10 años | **Procedimiento documentado** | `docs/INCIDENT_RESPONSE.md` define respaldo diario y retención tributaria. No hay evidencia de ejecución en entorno real. |

## 4. Evidencia ejecutada

### 4.1 Build, tests y contenedores

```text
cd backend && ./mvnw verify  -> 12/12 tests OK
npm run build                -> OK
npm run lint                 -> OK, 0 warnings
docker compose up -d --build -> 3/3 servicios healthy
curl /api/health             -> {"status":"UP",...}
curl localhost:5173/api/health -> {"status":"UP",...}
curl localhost:5173/reportes -> 200 text/html (SPA fallback)
```

Pruebas funcionales sobre la imagen Docker:

```text
login cajero                    -> rol cajero
cajero -> /api/reports/summary  -> 403
gerente -> /api/reports/summary -> 200
CA-05 precio enviado 0.01       -> servidor guardó 459.99
```

### 4.2 Trivy

Ejecutado con `trivy fs --scanners vuln,secret,misconfig .`:

- 0 secretos hardcodeados.
- 0 misconfiguraciones en el corte inicial; al incorporarse los Dockerfiles se debe volver a ejecutar con Trivy ≥ 0.72.0.
- `package-lock.json`: 7 hallazgos (2 HIGH, 4 MEDIUM, 1 LOW) originados en dependencias de desarrollo de Vite; todos con corrección disponible. No bloquean el arranque del MVP, pero deben resolverse antes de endurecer la pipeline.

### 4.3 Semgrep

Ejecutado con Semgrep **1.186.0** en entorno virtual aislado:

```text
semgrep scan --config semgrep.yml --exclude node_modules --exclude target \
  --exclude dist --exclude evidence --metrics=off --timeout 120 .
```

Resultado: **8 reglas, 114 archivos, 0 hallazgos**. Evidencia completa en `evidence/semgrep.log`.

### 4.4 OWASP ZAP

ZAP no está disponible localmente. El workflow de CI incluye un baseline opcional controlado por `ZAP_TARGET`, que **no sustituye** el Active Scan autenticado recomendado por el informe de seguridad original. Para cerrar totalmente CA-01 a CA-05 se debe ejecutar ZAP contra `http://localhost:5173` con credenciales de gerente y cajero.

## 5. Limitaciones honestas

1. El DTE cumple un **contrato formal v1.4 simulado**; no hay credenciales, certificados ni endpoints reales de Hacienda.
2. No se ha medido RNF-02 con prueba de carga.
3. TLS real y respaldos automáticos dependen del entorno de despliegue, no del código del MVP.
4. ZAP autenticado queda como actividad pendiente; Semgrep y Trivy sí fueron ejecutados localmente.
