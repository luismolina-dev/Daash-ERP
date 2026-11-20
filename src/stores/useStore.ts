import { create } from 'zustand';
import { api, apiFetch, setAccessToken, setAuthFailureHandler, ApiError } from '../lib/api';
import type {
  User, Product, ProductCreate, ProductPatch, CartItem, Customer, CustomerCreate,
  Supplier, SupplierCreate, Sale, SaleCreate, KardexEntry, CashRegister,
  ContingencyQueue, AdjustmentCreate, PurchaseCreate, Purchase,
  ReportSummary, IvaReportType,
} from '../types';

export type ActionStatus = 'idle' | 'loading' | 'error' | 'success';

interface AppState {
  // Auth
  currentUser: User | null;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;

  // Catálogos e inventario
  products: Product[];
  loadProducts: (params?: { search?: string; category?: string; lowStockOnly?: boolean }) => Promise<void>;
  createProduct: (data: ProductCreate) => Promise<Product | null>;
  updateProduct: (id: string, updates: ProductPatch) => Promise<Product | null>;
  addInventoryAdjustment: (data: AdjustmentCreate) => Promise<KardexEntry | null>;
  kardex: KardexEntry[];
  loadKardex: (productId?: string) => Promise<void>;

  // Carrito (local, solo cantidades y descuento; precios se recalculan en el servidor)
  cart: CartItem[];
  addToCart: (product: Product) => boolean;
  removeFromCart: (productId: string) => void;
  updateCartQuantity: (productId: string, cantidad: number) => boolean;
  updateCartDiscount: (productId: string, descuento: number) => void;
  clearCart: () => void;

  // Clientes y proveedores
  customers: Customer[];
  loadCustomers: () => Promise<void>;
  addCustomer: (data: CustomerCreate) => Promise<Customer | null>;
  suppliers: Supplier[];
  loadSuppliers: () => Promise<void>;
  addSupplier: (data: SupplierCreate) => Promise<Supplier | null>;
  purchases: Purchase[];
  loadPurchases: () => Promise<void>;
  addPurchase: (data: PurchaseCreate) => Promise<Purchase | null>;
  downloadPurchasePdf: (id: string, open?: boolean) => Promise<boolean>;
  resendPurchaseEmail: (id: string) => Promise<boolean>;

  // Caja
  cashRegister: CashRegister | null;
  loadCashRegister: () => Promise<void>;
  openCashRegister: (fondoInicial: number) => Promise<boolean>;
  closeCashRegister: (efectivoReal: number, tarjetaReal: number, transferenciaReal: number) => Promise<boolean>;
  getCurrentCashRegister: () => CashRegister | null;

  // Ventas / DTE
  sales: Sale[];
  loadSales: (params?: { search?: string; estado?: string }) => Promise<void>;
  createSale: (data: SaleCreate) => Promise<Sale | null>;
  anularSale: (saleId: string) => Promise<boolean>;
  contingencyQueue: ContingencyQueue[];
  loadContingencies: () => Promise<void>;
  simulatedDowntime: boolean;
  toggleDowntime: (down?: boolean) => Promise<void>;

  // Reportes
  reportSummary: ReportSummary | null;
  loadReportSummary: () => Promise<void>;
  loadIvaReport: (tipo: IvaReportType) => Promise<string | null>;

  // Estado UI compartido
  status: ActionStatus;
  error: string | null;
  message: string | null;
  setMessage: (message: string | null) => void;
  clearError: () => void;

  // Helpers
  getStock: (productId: string) => number;
}

const errorMessage = (err: unknown): string =>
  err instanceof ApiError || err instanceof Error ? err.message : 'Error inesperado';

function paramsToQuery(params?: Record<string, string | number | boolean | undefined>): string {
  if (!params) return '';
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value));
  });
  const result = search.toString();
  return result ? `?${result}` : '';
}

export const useStore = create<AppState>((set, get) => ({
  currentUser: null,
  products: [],
  kardex: [],
  cart: [],
  customers: [],
  suppliers: [],
  purchases: [],
  cashRegister: null,
  sales: [],
  contingencyQueue: [],
  simulatedDowntime: false,
  reportSummary: null,
  status: 'idle',
  error: null,
  message: null,

  setMessage: (message) => set({ message }),
  clearError: () => set({ error: null }),

  login: async (email, password) => {
    set({ status: 'loading', error: null, message: null });
    try {
      const data = await apiFetch<import('../types').AuthResponse>('/auth/login', {
        method: 'POST',
        body: { email, password },
        skipRefresh: true,
      });
      setAccessToken(data.accessToken);
      set({ currentUser: data.user, status: 'success', message: `Bienvenido, ${data.user.nombre}` });
      return true;
    } catch (err) {
      setAccessToken(null);
      set({ status: 'error', error: errorMessage(err), currentUser: null });
      return false;
    }
  },

  logout: async () => {
    try {
      await api.post<void>('/auth/logout');
    } catch {
      // El logout local se ejecuta incluso si el servidor no responde.
    }
    setAccessToken(null);
    set({
      currentUser: null, cart: [], cashRegister: null, sales: [], products: [],
      customers: [], suppliers: [], purchases: [], kardex: [], contingencyQueue: [],
      reportSummary: null, simulatedDowntime: false,
      status: 'idle', error: null, message: 'Sesión cerrada',
    });
  },

  loadProducts: async (params) => {
    set({ status: 'loading', error: null });
    try {
      const products = await api.get<Product[]>(`/products${paramsToQuery(params)}`);
      set({ products, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  createProduct: async (data) => {
    set({ status: 'loading', error: null });
    try {
      const product = await api.post<Product>('/products', data);
      set(state => ({
        products: [...state.products, product],
        status: 'success', message: `Producto ${product.nombre} creado`,
      }));
      return product;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return null;
    }
  },

  updateProduct: async (id, updates) => {
    set({ status: 'loading', error: null });
    try {
      const product = await api.patch<Product>(`/products/${encodeURIComponent(id)}`, updates);
      set(state => ({
        products: state.products.map(p => (p.id === id ? product : p)),
        status: 'success', message: `Producto ${product.nombre} actualizado`,
      }));
      return product;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return null;
    }
  },

  addInventoryAdjustment: async (data) => {
    set({ status: 'loading', error: null });
    try {
      const entry = await api.post<KardexEntry>('/inventory/adjustments', data);
      set(state => ({ kardex: [entry, ...state.kardex], status: 'success', message: 'Ajuste registrado' }));
      await get().loadProducts();
      return entry;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return null;
    }
  },

  loadKardex: async (productId) => {
    set({ status: 'loading', error: null });
    try {
      const kardex = await api.get<KardexEntry[]>(`/inventory/kardex${paramsToQuery({ productId })}`);
      set({ kardex, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  addToCart: (product) => {
    const state = get();
    if (product.stock <= 0) return false;
    const existing = state.cart.find(i => i.producto.id === product.id);
    if (existing) {
      if (existing.cantidad >= product.stock) return false;
      set({ cart: state.cart.map(i => i.producto.id === product.id ? { ...i, cantidad: i.cantidad + 1 } : i) });
    } else {
      set({ cart: [...state.cart, { producto: product, cantidad: 1, descuento: 0 }] });
    }
    return true;
  },

  removeFromCart: (productId) => set(state => ({ cart: state.cart.filter(i => i.producto.id !== productId) })),

  updateCartQuantity: (productId, cantidad) => {
    const item = get().cart.find(i => i.producto.id === productId);
    if (!item || cantidad > item.producto.stock || cantidad < 1) return false;
    set(state => ({ cart: state.cart.map(i => i.producto.id === productId ? { ...i, cantidad } : i) }));
    return true;
  },

  updateCartDiscount: (productId, descuento) => set(state => ({
    cart: state.cart.map(i => i.producto.id === productId ? { ...i, descuento } : i),
  })),

  clearCart: () => set({ cart: [] }),

  loadCustomers: async () => {
    set({ status: 'loading', error: null });
    try {
      const customers = await api.get<Customer[]>('/customers');
      set({ customers, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  addCustomer: async (data) => {
    set({ status: 'loading', error: null });
    try {
      const customer = await api.post<Customer>('/customers', data);
      set(state => ({ customers: [...state.customers, customer], status: 'success', message: `Cliente ${customer.nombre} registrado` }));
      return customer;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return null;
    }
  },

  loadSuppliers: async () => {
    set({ status: 'loading', error: null });
    try {
      const suppliers = await api.get<Supplier[]>('/suppliers');
      set({ suppliers, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  addSupplier: async (data) => {
    set({ status: 'loading', error: null });
    try {
      const supplier = await api.post<Supplier>('/suppliers', data);
      set(state => ({ suppliers: [...state.suppliers, supplier], status: 'success', message: `Proveedor ${supplier.nombre} registrado` }));
      return supplier;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return null;
    }
  },

  loadPurchases: async () => {
    set({ status: 'loading', error: null });
    try {
      const purchases = await api.get<Purchase[]>('/purchases');
      set({ purchases, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  addPurchase: async (data) => {
    set({ status: 'loading', error: null });
    try {
      const purchase = await api.post<Purchase>('/purchases', data);
      set(state => ({
        purchases: [purchase, ...state.purchases],
        status: 'success',
        message: `Compra ${purchase.numeroCompra} registrada; correo ${purchase.emailEstado.toLowerCase()}`,
      }));
      await Promise.all([get().loadProducts(), get().loadSuppliers()]);
      return purchase;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return null;
    }
  },

  downloadPurchasePdf: async (id, open = false) => {
    set({ status: 'loading', error: null });
    try {
      const blob = await apiFetch<Blob>(`/purchases/${encodeURIComponent(id)}/pdf`, { blob: true });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      if (open) {
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
      } else {
        link.download = `factura-compra-${id}.pdf`;
      }
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 2000);
      set({ status: 'success', message: open ? 'Factura abierta en el navegador' : 'Factura descargada' });
      return true;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return false;
    }
  },

  resendPurchaseEmail: async (id) => {
    set({ status: 'loading', error: null });
    try {
      const updated = await api.post<Purchase>(`/purchases/${encodeURIComponent(id)}/resend-email`);
      set(state => ({
        purchases: state.purchases.map(p => (p.id === id ? updated : p)),
        status: 'success',
        message: `Correo reenviado: ${updated.emailEstado.toLowerCase()}`,
      }));
      return true;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return false;
    }
  },

  loadCashRegister: async () => {
    set({ status: 'loading', error: null });
    try {
      const cashRegister = await api.get<CashRegister | null>('/cash-registers/current');
      set({ cashRegister, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  openCashRegister: async (fondoInicial) => {
    set({ status: 'loading', error: null });
    try {
      const cashRegister = await api.post<CashRegister>('/cash-registers/open', { fondoInicial });
      set({ cashRegister, status: 'success', message: 'Caja abierta correctamente' });
      return true;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return false;
    }
  },

  closeCashRegister: async (efectivoReal, tarjetaReal, transferenciaReal) => {
    set({ status: 'loading', error: null });
    try {
      const cashRegister = await api.post<CashRegister>('/cash-registers/close', {
        efectivoReal, tarjetaReal, transferenciaReal,
      });
      set({ cashRegister, status: 'success', message: 'Corte de caja realizado' });
      return true;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return false;
    }
  },

  getCurrentCashRegister: () => get().cashRegister,

  loadSales: async (params) => {
    set({ status: 'loading', error: null });
    try {
      const sales = await api.get<Sale[]>(`/sales${paramsToQuery(params)}`);
      set({ sales, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  createSale: async (data: SaleCreate) => {
    set({ status: 'loading', error: null });
    try {
      // Nunca se envían precios, subtotales, IVA ni totales calculados en el cliente.
      const payload: SaleCreate = {
        customerId: data.customerId ?? null,
        metodoPago: data.metodoPago,
        items: data.items.map(item => ({
          productId: item.productId,
          cantidad: item.cantidad,
          descuento: item.descuento,
        })),
      };
      const sale = await api.post<Sale>('/sales', payload);
      set(state => ({
        sales: [sale, ...state.sales],
        cart: [],
        status: 'success',
        message: sale.estadoDTE === 'contingencia'
          ? `Venta ${sale.numeroFactura} registrada en CONTINGENCIA`
          : `Factura ${sale.numeroFactura} emitida (DTE aceptado)`,
      }));
      await Promise.all([get().loadProducts(), get().loadCashRegister()]);
      if (get().currentUser?.role === 'gerente' || get().currentUser?.role === 'auditor') {
        await get().loadContingencies();
      }
      return sale;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return null;
    }
  },

  anularSale: async (saleId) => {
    set({ status: 'loading', error: null });
    try {
      const updated = await api.post<Sale>(`/sales/${encodeURIComponent(saleId)}/anular`);
      set(state => ({
        sales: state.sales.map(s => (s.id === saleId ? updated : s)),
        status: 'success',
        message: `Factura anulada. Nota de crédito: ${updated.notaCreditoId ?? 'N/A'}`,
      }));
      await get().loadProducts();
      return true;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return false;
    }
  },

  loadContingencies: async () => {
    set({ status: 'loading', error: null });
    try {
      const contingencyQueue = await api.get<ContingencyQueue[]>('/dte/contingencies');
      set({ contingencyQueue, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  toggleDowntime: async (down) => {
    set({ status: 'loading', error: null });
    try {
      const next = down ?? !get().simulatedDowntime;
      const result = await api.post<{ down: boolean }>('/dte/simulation/downtime', { down: next });
      set({ simulatedDowntime: result.down, status: 'success', message: result.down ? 'Hacienda simulada caída' : 'Hacienda disponible' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  loadReportSummary: async () => {
    set({ status: 'loading', error: null });
    try {
      const reportSummary = await api.get<ReportSummary>('/reports/summary');
      set({ reportSummary, status: 'success' });
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
    }
  },

  loadIvaReport: async (tipo) => {
    set({ status: 'loading', error: null });
    try {
      const csv = await apiFetch<string>(`/reports/iva${paramsToQuery({ tipo })}`, { rawText: true });
      set({ status: 'success', message: `Reporte ${tipo} generado` });
      return csv;
    } catch (err) {
      set({ status: 'error', error: errorMessage(err) });
      return null;
    }
  },

  getStock: (productId) => get().products.find(p => p.id === productId)?.stock ?? 0,
}));

// Si el refresh falla, la API limpia el token y aquí limpiamos también la sesión local.
setAuthFailureHandler(() => {
  useStore.setState({
    currentUser: null, cart: [], cashRegister: null, sales: [], products: [],
    customers: [], suppliers: [], purchases: [], kardex: [], contingencyQueue: [],
    reportSummary: null, simulatedDowntime: false,
    status: 'idle', error: null, message: 'Sesión expirada. Inicie sesión nuevamente.',
  });
});
