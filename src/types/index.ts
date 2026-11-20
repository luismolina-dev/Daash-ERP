export type UserRole = 'cajero' | 'admin_bodega' | 'gerente' | 'auditor';

export interface User {
  id: string;
  nombre: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Product {
  id: string;
  codigo: string;
  nombre: string;
  descripcion: string;
  precio: number;
  costo: number;
  stock: number;
  stockMinimo: number;
  categoria: string;
  activo: boolean;
}

export interface ProductCreate {
  codigo: string;
  nombre: string;
  categoria: string;
  precio: number;
  costo: number;
  stockMinimo: number;
  descripcion?: string;
}

export interface ProductPatch {
  nombre?: string;
  descripcion?: string;
  precio?: number;
  costo?: number;
  stockMinimo?: number;
  categoria?: string;
  activo?: boolean;
}

export interface CartItem {
  producto: Product;
  cantidad: number;
  descuento: number;
}

export interface Customer {
  id: string;
  nombre: string;
  email: string;
  telefono: string;
  dui: string;
  nit: string;
  nrc: string;
  direccion: string;
  tipoContribuyente: 'consumidor' | 'contribuyente';
}

export type CustomerCreate = Omit<Customer, 'id'>;

export interface Supplier {
  id: string;
  nombre: string;
  nit: string;
  nrc: string;
  telefono: string;
  email: string;
  direccion: string;
  contacto: string;
}

export type SupplierCreate = Omit<Supplier, 'id'>;

export type MetodoPago = 'efectivo' | 'tarjeta' | 'transferencia';

export type EstadoDTE = 'aceptado' | 'contingencia' | 'rechazado';

export interface SaleItem {
  productoId: string;
  nombre: string;
  codigo: string;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
  subtotal: number;
  iva: number;
}

export interface Sale {
  id: string;
  numeroFactura: string;
  fecha: string;
  clienteId: string | null;
  clienteNombre: string;
  items: SaleItem[];
  subtotal: number;
  iva: number;
  total: number;
  metodoPago: MetodoPago;
  estadoDTE: EstadoDTE;
  uuid: string | null;
  contingencyReason: string | null;
  userId: string;
  anulada: boolean;
  notaCreditoId: string | null;
  dteJson?: unknown;
  dteFirma?: string | null;
}

export interface SaleCreate {
  customerId?: string | null;
  metodoPago: MetodoPago;
  items: Array<{ productId: string; cantidad: number; descuento: number }>;
}

export interface PurchaseItem {
  productId: string;
  cantidad: number;
  precioUnitario: number;
}

export type PurchaseEmailEstado = 'PENDIENTE' | 'ENVIADO' | 'SIMULADO' | 'FALLIDO';

export interface Purchase {
  id: string;
  numeroCompra: string;
  fecha: string;
  proveedorId: string;
  proveedorNombre: string;
  customerEmail: string;
  emailEstado: PurchaseEmailEstado;
  emailFecha: string | null;
  emailError: string | null;
  pdfDisponible: boolean;
  codigoGeneracion: string;
  items: Array<PurchaseItem & { productoNombre: string; subtotal: number }>;
  subtotal: number;
  iva: number;
  total: number;
}

export interface PurchaseCreate {
  supplierId: string;
  customerEmail: string;
  items: PurchaseItem[];
}

export type TipoAjuste = 'dano' | 'perdida' | 'ajuste';

export interface AdjustmentCreate {
  productId: string;
  tipo: TipoAjuste | 'entrada' | 'salida';
  cantidad: number;
  justificacion: string;
}

export interface KardexEntry {
  id: string;
  fecha: string;
  productoId: string;
  productoNombre: string;
  tipo: 'entrada' | 'salida' | 'ajuste' | 'venta' | 'compra' | 'anulacion';
  cantidad: number;
  stockAnterior: number;
  stockPosterior: number;
  justificacion: string;
  userId: string;
  referencia: string;
}

export interface CashRegister {
  id: string;
  userId: string;
  fechaApertura: string;
  fechaCierre: string | null;
  fondoInicial: number;
  totalEfectivo: number;
  totalTarjeta: number;
  totalTransferencia: number;
  totalVentas: number;
  abierta: boolean;
  arqueo?: ArqueoZ;
}

export interface ArqueoZ {
  fecha: string;
  efectivoEsperado: number;
  efectivoReal: number;
  tarjeta: number;
  transferencia: number;
  totalVentas: number;
  diferencia: number;
}

export interface ContingencyQueue {
  id: string;
  saleId: string;
  intentos: number;
  maxIntentos: number;
  estado: 'pendiente' | 'enviado' | 'fallido';
  proximoReintento: string;
  ultimoError?: string;
}

export interface ReportSummary {
  totalIngresos: number;
  totalIVA: number;
  totalVentas: number;
  lowStockProductCount: number;
  topProducts: Array<{ productoId: string; nombre: string; cantidad: number; monto: number }>;
  ventasPorDia: Array<{ fecha: string; total: number }>;
}

export type IvaReportType = 'ventas_consumidor' | 'ventas_contribuyente' | 'compras';
