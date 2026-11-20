# Contrato API — Daash ERP & DTE MVP

Base URL: `http://localhost:8080/api`

Todas las respuestas son JSON salvo `/reports/iva` que devuelve `text/csv` y `/purchases/{id}/pdf` que devuelve `application/pdf`.
Todas las peticiones autenticadas requieren `Authorization: Bearer <accessToken>`.

## Roles

`cajero`, `admin_bodega`, `gerente`, `auditor`.

## Autenticación

| Método | Ruta | Acceso | Body | Respuesta |
|---|---|---|---|---|
| POST | `/auth/login` | público | `{ email, password }` | `{ accessToken, tokenType, expiresIn, user }` + cookie `refreshToken` httpOnly |
| POST | `/auth/refresh` | cookie | — | `{ accessToken, tokenType, expiresIn, user }` |
| POST | `/auth/logout` | autenticado | — | `204` y expira cookie |
| GET | `/auth/me` | autenticado | — | `User` |

`User = { id, nombre, email, role }`. Nunca devolver `password`.

## Catálogos e inventario

| Método | Ruta | Roles | Body / query | Respuesta |
|---|---|---|---|---|
| GET | `/products` | todos | `?search=&category=&lowStockOnly=` | `Product[]` |
| POST | `/products` | `admin_bodega`, `gerente` | `ProductCreate` | `Product` |
| PATCH | `/products/{id}` | `admin_bodega`, `gerente` | `ProductPatch` | `Product` |
| GET | `/inventory/kardex?productId=` | `admin_bodega`, `gerente`, `auditor` | — | `KardexEntry[]` |
| POST | `/inventory/adjustments` | `admin_bodega`, `gerente` | `{ productId, tipo, cantidad, justificacion }` | `KardexEntry` |

`Product = { id, codigo, nombre, descripcion, precio, costo, stock, stockMinimo, categoria, activo }`
`ProductCreate` exige `codigo, nombre, categoria, precio, costo, stockMinimo`.
`ProductPatch` permite `nombre, descripcion, precio, costo, stockMinimo, categoria, activo`. No permite modificar `stock` directamente; para eso existe adjustments.

`KardexEntry = { id, fecha, productoId, productoNombre, tipo, cantidad, stockAnterior, stockPosterior, justificacion, userId, referencia }`.

## Clientes y proveedores

| Método | Ruta | Roles | Body | Respuesta |
|---|---|---|---|---|
| GET | `/customers` | `cajero`, `gerente`, `auditor` | — | `Customer[]` |
| POST | `/customers` | `cajero`, `gerente` | `CustomerCreate` | `Customer` |
| GET | `/suppliers` | `admin_bodega`, `gerente`, `auditor` | — | `Supplier[]` |
| POST | `/suppliers` | `admin_bodega`, `gerente` | `SupplierCreate` | `Supplier` |
| POST | `/purchases` | `admin_bodega`, `gerente` | `{ supplierId, customerEmail, items: [{ productId, cantidad, precioUnitario }] }` | `Purchase` |
| GET | `/purchases` | `admin_bodega`, `gerente`, `auditor` | — | `Purchase[]` |
| GET | `/purchases/{id}/pdf` | `admin_bodega`, `gerente`, `auditor` | — | PDF (`inline`) |
| POST | `/purchases/{id}/resend-email` | `admin_bodega`, `gerente` | — | `Purchase` actualizado |

`Customer = { id, nombre, email, telefono, dui, nit, nrc, direccion, tipoContribuyente }`.
Validación: DUI `00000000-0`; NIT `0614-123456-100-0`.
`Supplier = { id, nombre, nit, nrc, telefono, email, direccion, contacto }`.
La compra aumenta stock y crea movimientos Kardex tipo `compra`.

`customerEmail` es obligatorio y se valida como correo electrónico. La compra genera un crédito fiscal (emisor = proveedor, receptor = empresa configurada), calcula IVA 13% sobre el subtotal con `HALF_UP`, persiste el PDF en Base64 y registra auditoría. El PDF incluye cabecera corporativa, tablas con estilos, marca de agua `SIMULADO` y un QR con `DAASH-DTE|1.4|CF_COMPRA|número|código|fecha|total|SHA-256(firma JWT)`. El resultado de entrega se reporta en `emailEstado`.

El PDF se regenera al consultarlo o reenviarlo, de modo que las compras anteriores también muestran el diseño y QR vigentes.

`Purchase = { id, numeroCompra, fecha, proveedorId, proveedorNombre, customerEmail, emailEstado, emailFecha, emailError, pdfDisponible, codigoGeneracion, items, subtotal, iva, total }`.
`items = [{ productoId, productoNombre, cantidad, precioUnitario, subtotal }]`.
`emailEstado`: `PENDIENTE`, `ENVIADO`, `SIMULADO` o `FALLIDO`. Sin `MAIL_HOST`, el correo se marca `SIMULADO` y la factura sigue disponible para ver/descargar desde la aplicación. Con SMTP configurado se envía el PDF como adjunto; `POST /purchases/{id}/resend-email` permite reenviarlo.

> Nota fiscal: por ser una compra, el proveedor figure como emisor del crédito fiscal. El correo configurado es un destinatario operativo de la copia de la factura; no convierte la transacción en una venta.

## Caja

| Método | Ruta | Roles | Body | Respuesta |
|---|---|---|---|---|
| GET | `/cash-registers/current` | `cajero`, `gerente`, `auditor` | — | `CashRegister \| null` |
| POST | `/cash-registers/open` | `cajero`, `gerente` | `{ fondoInicial }` | `CashRegister` |
| POST | `/cash-registers/close` | `cajero`, `gerente` | `{ efectivoReal, tarjetaReal, transferenciaReal }` | `CashRegister` con `arqueo` |

`CashRegister = { id, userId, fechaApertura, fechaCierre, fondoInicial, totalEfectivo, totalTarjeta, totalTransferencia, totalVentas, abierta, arqueo? }`.
`arqueo = { fecha, efectivoEsperado, efectivoReal, tarjeta, transferencia, totalVentas, diferencia }`.

## Ventas, DTE y contingencia

| Método | Ruta | Roles | Body | Respuesta |
|---|---|---|---|---|
| POST | `/sales` | `cajero`, `gerente` | `SaleCreate` | `Sale` |
| GET | `/sales?search=&estado=` | `cajero`, `gerente`, `auditor` | — | `Sale[]` |
| POST | `/sales/{id}/anular` | `gerente` | — | `Sale` con nota de crédito |
| GET | `/dte/contingencies` | `gerente`, `auditor` | — | `ContingencyQueue[]` |
| POST | `/dte/simulation/downtime` | `gerente`, `auditor` | `{ down: boolean }` | `{ down }` |

`SaleCreate = { customerId?: string|null, metodoPago: 'efectivo'|'tarjeta'|'transferencia', items: [{ productId, cantidad, descuento }] }`.

**Regla crítica CA-05:** el servidor ignora cualquier `precio`, `subtotal`, `iva` o `total` enviado por el cliente. Recalcula desde `products.precio` en PostgreSQL. IVA = 13%. Descuento > 10% solo permitido si el token pertenece a `gerente`.

`Sale = { id, numeroFactura, fecha, clienteId, clienteNombre, items: SaleItem[], subtotal, iva, total, metodoPago, estadoDTE, uuid, contingencyReason, userId, anulada, notaCreditoId, dteJson, dteFirma }`.

`estadoDTE`: `aceptado`, `contingencia`, `rechazado`.

`ContingencyQueue = { id, saleId, intentos, maxIntentos, estado, proximoReintento, ultimoError? }`.

DTE simulado con contrato:
- JSON con `version: "1.4"`, `codigoGeneracion`, `fechaEmision`, emisor/receptor, ítems, subtotal, IVA y total.
- Firma JWT HS256 con `DTE_SIGNING_SECRET` (simulación; no sustituye certificado de Hacienda).
- `MockHaciendaClient` responde éxito/fallo según el flag de simulación.
- Si falla: venta queda en `contingencia`, se persiste el DTE y se crea fila en cola.
- Job programado cada 5 minutos reintenta; al éxito actualiza `estadoDTE=aceptado`, guarda `uuid` (sello) y marca fila `enviado`.
- Cada evento escribe `AuditLog`.

## Reportes

| Método | Ruta | Roles | Respuesta |
|---|---|---|---|
| GET | `/reports/summary` | `gerente`, `auditor` | `{ totalIngresos, totalIVA, totalVentas, lowStockProductCount, topProducts: [{ productoId, nombre, cantidad, monto }], ventasPorDia: [{ fecha, total }] }` |
| GET | `/reports/iva?tipo=ventas_consumidor\|ventas_contribuyente\|compras` | `gerente`, `auditor` | CSV |

## Salud

`GET /health` → `{ status: "UP", timestamp }`.

## Códigos de error

- `400` validación / regla de negocio.
- `401` credenciales inválidas o token ausente/expirado.
- `403` rol insuficiente.
- `404` recurso no encontrado.
- `409` Conflicto, por ejemplo caja ya abierta.
- `429` Rate limit / cuenta bloqueada temporalmente.

Formato de error: `{ timestamp, status, error, message, path }`.
