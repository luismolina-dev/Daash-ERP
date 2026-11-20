# Aditamento al INFORME TÉCNICO DE RESULTADOS ÁREA DE SEGURIDAD (DEVSECOPS)

**Proyecto:** DAASH ERP & DTE — Adventure Works / Daash Devs
**Documento base:** *INFORME TÉCNICO DE RESULTADOS ÁREA DE SEGURIDAD (DEVSECOPS)*, emitido el 20 de noviembre de 2026 (cierre del Sprint 4).
**Fecha del aditamento:** 20 de noviembre de 2026 · **Sprint:** 4 → 5
**Destinatarios:** Catedrática evaluadora, Product Owner y Scrum Master.

> Este aditamento **no modifica, reemplaza ni reescribe el PDF original**. Se entrega como documento complementario para ajustar los estados de los casos de abuso y de los requerimientos no funcionales a la arquitectura que finalmente se implementó. La matriz detallada de verificación y las rutas de código están en [`SECURITY_VALIDATION.md`](SECURITY_VALIDATION.md).

## 1. Motivo del aditamento

El informe base evaluó un prototipo **frontend-only**: una SPA React/TypeScript con estado en el navegador, sin API persistente, con controles de acceso representados principalmente en la interfaz y con un DTE simulado en el cliente. Bajo ese escenario era coherente marcar como *Parcial* la validación criptográfica de JWT y el rate limiting, y como dependientes de integración los controles de reportería y de firma.

Durante la transición al MVP se incorporó un **backend Spring Boot (Java 25) con PostgreSQL 16**, una API REST con contrato v1.4, autenticación JWT con refresh cookie `httpOnly`, BCrypt, RBAC por endpoint, rate limiting, cabeceras de seguridad, auditoría y un módulo DTE simulado con cola de contingencia y reintento automático. Por tanto, **las conclusiones del informe base quedan condicionadas a la nueva arquitectura** y requieren la siguiente actualización.

## 2. Estado de integración verificado

- `docker compose up -d --build` levantó `daash-postgres`, `daash-backend` y `daash-frontend` en estado **healthy**.
- `cd backend && ./mvnw verify`: **12/12 tests OK**.
- `npm run build` y `npm run lint`: **OK, 0 errores y 0 advertencias**.
- Semgrep 1.186.0: **8 reglas, 114 archivos, 0 hallazgos** (`evidence/semgrep.log`).
- Trivy: 0 secretos; 7 hallazgos en dependencias de desarrollo de Vite (2 HIGH, 4 MEDIUM, 1 LOW) con corrección disponible.
- Pruebas funcionales en vivo: cajero → reportes `403`; gerente → reportes `200`; payload con `precioUnitario=0.01` recalculado a `459.99` por el servidor.

## 3. Cambio de arquitectura que motiva el ajuste

| Dimensión | Prototipo previo (frontend-only) | MVP actual |
|---|---|---|
| Autorización | Rutas protegidas por estado visual en React; sin verificación server-side. | RBAC en cada endpoint del backend (`cajero`, `admin_bodega`, `gerente`, `auditor`), conforme a `docs/API_CONTRACT.md`. |
| Sesión | Tokens y datos manipulados en el cliente. | Access token en memoria (`src/lib/api.ts`), refresh en cookie `httpOnly`, firma HS256 con secretos de entorno. |
| Datos y precios | Cálculo reproducible en el navegador. | El backend recalcula subtotal e IVA 13 % desde `products.precio` en PostgreSQL; ignora totales del cliente. |
| DTE | Simulación local sin persistencia ni reintento. | Contrato JSON **v1.4**, firma JWT HS256 con `DTE_SIGNING_SECRET`, `MockHaciendaClient`, persistencia del DTE, cola de contingencia y job de reintento cada 5 min con sello UUID. |
| Evidencia | SAST/DAST sobre SPA y manifiestos. | JUnit/MockMvc, `./mvnw verify`, Semgrep, Trivy, `scripts/security-scan.sh` y CI con tres jobs. |

**Aclaración fiscal:** se mantiene la simulación formal. No hay integración real con el Ministerio de Hacienda; la firma del DTE no sustituye un certificado digital. Esto debe leerse conjuntamente con el riesgo **RS-02** del informe base: el esquema quedó congelado en **v1.4 para el MVP**, pero la validación fiscal real sigue fuera de alcance.

## 4. Ajustes a la sección 3.1 — Estado de controles

| Dominio (informe base) | Texto original | Ajuste |
|---|---|---|
| Autenticación y RBAC | "faltan filtros de autorización por rol en el 100 % de los controladores REST" | El contrato v1.4 define RBAC por endpoint y el backend lo implementa en filtros/anotaciones. **Estado: verificado en entorno local** con pruebas 401/403 y llamadas HTTP reales. |
| Seguridad transaccional | Recálculo en servidor "mitigando la manipulación"; firma/custodia *Parcial* | El recálculo y la auditoría quedan en el backend; la custodia de llaves y el cifrado extremo a extremo **siguen Parciales** por tratarse de una firma simulada. |
| Comunicaciones y secretos | TLS 1.2+ "definido, pendiente de despliegue" | Sin cambios: `SSL_ENABLED` y HSTS están definidos, pero **sin verificación en entorno desplegado**. Las credenciales se leen de variables de entorno; Trivy no detectó secretos en el corte. |

## 5. Ajustes a la Tabla 3 — Casos de abuso CA-01 a CA-05

| ID | Estado en el informe base | Estado ajustado a la implementación real | Justificación | Evidencia / verificación |
|---|---|---|---|---|
| **CA-01** | **Verificado** (Semgrep + ZAP activo) | **Verificado en entorno local**; ZAP autenticado pendiente como refuerzo | Repositorios JPA/Hibernate sin concatenación de SQL. Semgrep ejecutado con 0 hallazgos y `./mvnw verify` en verde. | `SECURITY_VALIDATION.md` §2 y §4; `semgrep.yml`; `cd backend && ./mvnw verify`. |
| **CA-02** | **Parcial** (401 observado; faltaban pruebas en reportes) | **Verificado en entorno local**; ZAP autenticado pendiente como refuerzo | Validación de firma, refresh httpOnly, RBAC por endpoint y token solo en memoria. Comprobado: tokenalterado → 401, cajero → 403, gerente → 200. | `SECURITY_VALIDATION.md` §2 y §4; `src/lib/api.ts`; `src/lib/roles.ts`. |
| **CA-03** | **Verificado** (escape React + inyección manual) | **Verificado en entorno local en frontend y cabeceras**; ZAP extremo a extremo pendiente | Sin `dangerouslySetInnerHTML`, sin logs sensibles, validación en DTO y CSP presente. | `grep -RIn dangerouslySetInnerHTML src` sin resultados; Semgrep 0 hallazgos. |
| **CA-04** | **Parcial** (bloqueo por cuenta correcto; gateway pendiente) | **Verificado en entorno local**; prueba de carga/ZAP y gateway externo pendientes | Filtro con 5 intentos por IP, bloqueo de 15 min y `429`, cubierto por `AuthSecurityTest`. No se afirma protección ante 500 intentos/s sin gateway. | `docs/API_CONTRACT.md`; `SECURITY_VALIDATION.md` §2 y §4. |
| **CA-05** | **Verificado** (rechazo de `precioUnitario` alterado) | **Verificado en entorno local**; interceptación con ZAP pendiente como refuerzo | Test de recálculo y comprobación en vivo: payload con `0.01` → servidor guardó `459.99`, IVA 13 % y auditoría. | `SECURITY_VALIDATION.md` §2 y §4; `POST /api/sales`. |

**Síntesis de cobertura ajustada:** 5/5 controles implementados y verificados en entorno local; 5/5 con ZAP autenticado pendiente como refuerzo de DAST. La afirmación original de "60 % verificado" queda superada por la evidencia ejecutable de este corte.

## 6. Ajustes a la Tabla 2 — Escaneos automatizados

| Herramienta | Resultado reportado en el informe | Resultado real de este corte |
|---|---|---|
| Semgrep | 0 críticos, 1 alto, 3 medios, 5 bajos | **Ejecutado**: 8 reglas, 114 archivos, **0 hallazgos**. Evidencia en `evidence/semgrep.log`. |
| Trivy | 0 críticos, 2 altos, 4 medios, 8 bajos | `trivy fs --scanners vuln,secret,misconfig .`: **0 secretos**; `package-lock.json` **7** (2 HIGH, 4 MEDIUM, 1 LOW, 0 CRITICAL), todos con fix disponible. Aviso: Trivy 0.69.3 omite un módulo rego que requiere ≥ 0.72.0. |
| OWASP ZAP | 0 críticos, 0 altos, 3 medios, 6 bajos (pasivo) | **No ejecutado**: ZAP no disponible y el paso de CI es baseline opcional (`ZAP_TARGET`); no sustituye el Active Scan autenticado. |

Las cifras del informe base corresponden a otro momento y a otro conjunto de artefactos; **no deben promediarse ni sumarse** con las de este aditamento.

## 7. Ajustes a la Tabla 4 — Indicadores

| Indicador | Informe base | Ajuste |
|---|---|---|
| Tasa de remediación Crítica/Alta | 100 % (3/3) | **No confirmado**: Trivy reporta 2 hallazgos HIGH con corrección disponible en dependencias de desarrollo del frontend; pendientes de remediación. |
| Cobertura de casos de abuso | 60 % (3/5 verificados) | **5/5 verificados en entorno local**; 5/5 con DAST autenticado pendiente como refuerzo. |
| Tiempo de resolución de impedimentos | 48 h meta / 36 h medidos | Sin cambio; se mantiene la regla de escalamiento. |
| Cumplimiento de DoD de seguridad | Parcial | **Parcial**: `./mvnw verify`, `npm run build`, `npm run lint`, Semgrep y Docker están en verde; faltan remediar los 2 HIGH, ejecutar ZAP autenticado y generar evidencia de respaldos/RNF-02. |

## 8. Cierre de la brecha operativa (recomendación 4 del informe)

El informe base señalaba la "ausencia de un protocolo formal de respuesta a incidentes". Esa brecha queda **cerrada en el plano documental** con [`INCIDENT_RESPONSE.md`](INCIDENT_RESPONSE.md), que define responsables, clasificación SEV-1 a SEV-3, contención, erradicación, recuperación, post-mortem, respaldo diario, cifrado, hash SHA-256, prueba trimestral de restauración y **retención tributaria de 10 años** (RNF-05).

**Estado honrado:** protocolo **formalizado**; evidencia de ejecución de respaldos **pendiente** de generar en el entorno de pruebas/despliegue.

## 9. Conclusión revisada

La incorporación del backend cambia el escenario descrito en el informe base: los controles CA-01 a CA-05 ya no dependen únicamente del frontend y el DTE cuenta con contrato v1.4, persistencia y contingencia simulada. Con la evidencia de este corte, los cinco casos están **verificados en entorno local**. Para declarar DoD de seguridad completo faltan: ZAP autenticado, remediación de los 2 HIGH de Trivy, medición de RNF-02 y evidencia real de respaldos. La firma y transmisión a Hacienda permanecen como **simulación formal**, no integración real.

## 10. Referencias

- Daash Devs (2026). *INFORME TÉCNICO DE RESULTADOS ÁREA DE SEGURIDAD (DEVSECOPS)*. Práctica Profesional II, UCA. **Documento base, no modificado.**
- Daash Devs (2026). *Documento de Requerimientos — DAASH ERP & DTE* (CA-01 a CA-05, RNF-01 a RNF-05).
- Daash Devs (2026). *Contrato API — Daash ERP & DTE MVP* (`docs/API_CONTRACT.md`, contrato v1.4).
- Daash Devs (2026). *Validación de Seguridad — MVP Daash ERP & DTE* (`docs/SECURITY_VALIDATION.md`).
- Daash Devs (2026). *Protocolo de Respuesta a Incidentes y Respaldos* (`docs/INCIDENT_RESPONSE.md`).
- OWASP Foundation (2021). *OWASP Top 10:2021*. https://owasp.org/Top10/
- Microsoft (s. f.). *Microsoft Security Development Lifecycle (SDL)*. https://learn.microsoft.com/en-us/windows/security/threat-protection/msft-security-dev-lifecycle
