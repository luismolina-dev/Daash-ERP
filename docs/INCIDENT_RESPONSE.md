# Protocolo de Respuesta a Incidentes y Respaldos — Daash ERP & DTE

**Versión:** 1.0 · **Fecha:** 20 de noviembre de 2026 · **Ciclo:** cierre Sprint 4 / Sprint 5
**Aplicación:** MVP Daash ERP & DTE (Adventure Works — Daash Devs, Práctica Profesional II, UCA)
**Relación:** cierra la recomendación 4 de la sección 5.2 del *INFORME TÉCNICO DE RESULTADOS ÁREA DE SEGURIDAD (DEVSECOPS)* y el requerimiento **RNF-05** del Documento de Requerimientos.

> Estado del documento: **protocolo formalizado**. La ejecución de respaldos y la conservación de evidencia en el entorno de pruebas deben registrarse en `evidence/` a medida que se despliega el entorno. Donde se indica "pendiente", la configuración aún no ha sido verificada en local; no se afirma lo contrario.

## 1. Objetivo y alcance

Establecer una respuesta repetible ante incidentes de seguridad que afecten la confidencialidad, integridad o disponibilidad del MVP (API Spring Boot, PostgreSQL, frontend React y el módulo DTE simulado), preservar la evidencia y garantizar la continuidad fiscal mediante respaldos diarios con **retención de 10 años** para comprobantes DTE y libros tributarios.

## 2. Responsables

| Rol | Responsabilidad durante el incidente | Usuarios demo del plan (referencia) |
|---|---|---|
| **Líder de Respuesta (DevSecOps / Daash Devs)** | Declarar el incidente, dirigir la contención, coordinar el equipo y cerrar el reporte. | Equipo de desarrollo (rotación) |
| **Gerente / Product Owner** | Autorizar interrupción controlada del servicio, decidir comunicación al patrocinador Adventure Works. | `ana@adventureworks.com` (`gerente`) |
| **Administrador de infraestructura / DBA** | Aislar red, rotar secretos, ejecutar restauración desde respaldo, verificar integridad de PostgreSQL. | No corresponde a un usuario demo |
| **Auditor** | Verificar integridad de `AuditLog`, conservar cadena de custodia y revisar el post-mortem. | `roberto@adventureworks.com` (`auditor`) |
| **Operación de caja / bodega** | Reportar anomalías (ventas alteradas, arqueos, inventario) y preservar comprobantes. | `maria@adventureworks.com`, `carlos@adventureworks.com` |

**Escalamiento:** todo hallazgo SEV-1/SEV-2 se escala al líder de respuesta en un máximo de **1 hora**; el seguimiento se registra en Jira con la misma regla de impedimentos de 48 horas del informe de seguridad.

## 3. Clasificación

| Nivel | Ejemplo | Objetivo de contención |
|---|---|---|
| **SEV-1 (crítico)** | Fuga de credenciales, modificación no autorizada de ventas/DTE, pérdida de base de datos. | ≤ 1 h |
| **SEV-2 (alto)** | Token alterado aceptado, escalada de rol, denegación de servicio del API. | ≤ 4 h |
| **SEV-3 (medio/bajo)** | Hallazgo SAST/SCA sin explotación, alerta de configuración, intento fallido de login. | ≤ 1 día hábil |

## 4. Fases del protocolo

### 4.1 Detección e identificación

1. Registrar la fuente: alerta de Semgrep/Trivy/ZAP, `AuditLog`, reporte de usuario o anomalía de arqueo.
2. Asignar ID `INC-AAAA-NNN`, nivel SEV, hora UTC de detección y responsable.
3. **Preservar evidencia antes de cualquier cambio**: volcado de logs, copia de la fila(s) de `AuditLog`, `git rev-parse HEAD` del despliegue, capturas de respuesta HTTP y hashes SHA-256 de los archivos recolectados.
4. No reiniciar servicios ni borrar archivos hasta que el líder de respuesta lo autorice.

### 4.2 Contención

| Acción | Evidencia esperada |
|---|---|
| Revocar/rotar `JWT_SECRET`, `REFRESH_SECRET` y, si aplica, `DTE_SIGNING_SECRET` en `.env` del entorno afectado. | Registro de cambio de secretos con fecha/hora y responsable; nuevo login de prueba exitoso; tokens antiguos rechazados (401). |
| Bloquear IP o cuenta de origen; mantener el bloqueo temporal de 15 min tras 5 fallos. | Salida del mecanismo de rate limit y/o registros del gateway; códigos 401/403/429 observados. |
| Deshabilitar temporalmente el endpoint o rol implicado (por ejemplo `/api/reports/**`). | Configuración aplicada y respuesta 403 verificada para el rol `cajero`. |
| Aislar el contenedor o snapshot de PostgreSQL si hay indicios de manipulación de datos. | ID del contenedor/snapshot, timestamp y cadena de custodia. |
| Poner en contingencia el envío DTE simulado si se sospecha firma comprometida. | Filas en `ContingencyQueue` y eventos en `AuditLog`. |

### 4.3 Erradicación

1. Aplicar el parche o corrección en una rama, con prueba de regresión que reproduzca el ataque (CA-01 a CA-05 según corresponda).
2. Ejecutar `cd backend && ./mvnw verify`, `npm run build`, `npm run lint` y `./scripts/security-scan.sh`; adjuntar `evidence/summary.txt` y los logs.
3. Eliminar secretos expuestos del historial (si llegaron a Git), rotar todas las credenciales afectadas y confirmar con Trivy `--scanners secret`.
4. Verificar que la causa raíz desapareció: reintento del escaneo SAST/DAST y nueva prueba controlada del caso de abuso.

**Evidencia esperada:** commit(s) correctivos, salida de tests, logs de escaneo, diff de configuración y acta de rotación de secretos.

### 4.4 Recuperación

1. Restaurar desde el respaldo íntegro más reciente **anterior al incidente**; nunca desde un respaldo con evidencia de manipulación.
2. Validar integridad: conteo de ventas, inconsistencias de Kardex, arqueo de caja, estado de `estadoDTE` y sello UUID de los comprobantes.
3. Confirmar cabeceras CSP, HSTS y X-Frame-Options y health check `GET /api/health`.
4. Rehabilitar servicios por etapas: API → frontend → reanudación del job de reintento DTE.
5. Obtener aceptación formal del gerente/product owner y del auditor antes de cerrar.

**Evidencia esperada:** registro de restauración (hora, respaldo utilizado, hash), resultados de validación de datos, salida de `curl -i` con cabeceras y acta de rehabilitación firmada por gerente y auditor.

### 4.5 Cierre y lecciones aprendidas

- Post-mortem en un máximo de **5 días hábiles**: cronología, causa raíz, impacto, controles que fallaron, acciones correctivas con responsable y fecha.
- Actualizar `docs/SECURITY_VALIDATION.md`, `docs/SECURITY_REPORT_ADDENDUM.md` y las reglas de `semgrep.yml` si el incidente reveló un patrón nuevo.
- Conservar el expediente `INC-AAAA-NNN` junto con el resto de la trazabilidad EV-SEC-01 a EV-SEC-05 del informe de seguridad.

## 5. Respaldo diario y retención tributaria (RNF-05)

| Elemento | Política |
|---|---|
| **Frecuencia** | Respaldo automático **diario** de PostgreSQL (volcado lógico `pg_dump` o snapshot del volumen), programado fuera del horario de operación. |
| **Alcance** | Todas las tablas, en especial `sales`, `sale_items`, `dte_documents`, `contingency_queue`, `audit_logs`, `kardex_entries` y catálogos. |
| **Retención** | **10 años** para DTE, comprobantes y libros de IVA, conforme al requerimiento tributario (RNF-05). Los respaldos operativos de corto plazo pueden rotar antes, pero el archivo fiscal debe conservarse durante el decenio. |
| **Ubicación** | Copia cifrada en almacenamiento separado del servidor de producción; acceso restringido a DBA, gerente y auditor. **Nunca** dentro del repositorio Git. |
| **Integridad** | Hash SHA-256 por archivo, manifiesto de respaldos y prueba de restauración **trimestral** (o antes de cada despliegue relevante). |
| **Secretos** | Las credenciales de base de datos se toman de `.env`/Docker secrets; no se incrustan en el script ni en el respaldo. |
| **Registro** | Bitácora de respaldos con fecha, tamaño, hash, resultado y responsable; adjuntar al expediente de evidencia. |

> **Estado honesto:** la política está definida en este documento. La evidencia de ejecución (bitácora, hashes y prueba de restauración) queda **pendiente de generar en el entorno de pruebas/despliegue**; no debe declararse como verificada hasta que exista.

### Comando de referencia (no ejecutado desde este repositorio)

```bash
# Ejemplo orientativo, ejecutar en el host/servicio de respaldos con las variables del .env
pg_dump -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  | gzip > "/backup/daash_$(date +%Y%m%d).sql.gz"
sha256sum "/backup/daash_$(date +%Y%m%d).sql.gz" >> /backup/manifiesto.sha256
```

## 6. Matriz mínima de evidencia del incidente

| ID | Evidencia | Productor |
|---|---|---|
| EV-INC-01 | Registro de detección y clasificación del incidente | Líder de respuesta |
| EV-INC-02 | Copia de logs y `AuditLog` con hashes SHA-256 | DBA / Auditor |
| EV-INC-03 | Actas de contención, erradicación y rotación de secretos | DevSecOps |
| EV-INC-04 | `evidence/summary.txt` y logs de `scripts/security-scan.sh` | CI / DevSecOps |
| EV-INC-05 | Bitácora de respaldo, hash y registro de restauración | DBA |
| EV-INC-06 | Acta de rehabilitación y post-mortem | Gerente / Auditor |

## 7. Referencias

- Daash Devs (2026). *INFORME TÉCNICO DE RESULTADOS ÁREA DE SEGURIDAD (DEVSECOPS)*, secciones 5.1 y 5.2. Práctica Profesional II, UCA.
- Daash Devs (2026). *Documento de Requerimientos — DAASH ERP & DTE*, RNF-05 y casos de abuso CA-01 a CA-05.
- Microsoft (s. f.). *Microsoft Security Development Lifecycle (SDL)*.
- OWASP Foundation (2021). *OWASP Top 10:2021* — A09 Security Logging and Monitoring Failures.
- NIST (2023). *NIST SP 800-61 Rev. 2 — Computer Security Incident Handling Guide* (marco de referencia para las fases del protocolo).
