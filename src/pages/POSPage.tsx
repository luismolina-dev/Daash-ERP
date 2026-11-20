import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useStore } from '../stores/useStore';
import { formatCurrency } from '../utils/helpers';
import {
  ShoppingCart, Search, Plus, Minus, Trash2, AlertTriangle, Lock, X, Check,
  Package, Loader2, UserRound, WifiOff,
} from 'lucide-react';
import type { MetodoPago, Sale } from '../types';

const DISCOUNT_LIMIT = 10;

export default function POSPage() {
  const {
    products, cart, customers, cashRegister, currentUser, status,
    addToCart, removeFromCart, updateCartQuantity, updateCartDiscount, clearCart,
    createSale, loadProducts, loadCustomers, loadCashRegister,
  } = useStore();
  const navigate = useNavigate();

  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('Todas');
  const [showPayment, setShowPayment] = useState(false);
  const [showManagerAuth, setShowManagerAuth] = useState(false);
  const [pendingDiscount, setPendingDiscount] = useState<{ productId: string; descuento: number } | null>(null);
  const [managerEmail, setManagerEmail] = useState('');
  const [managerPassword, setManagerPassword] = useState('');
  const [managerError, setManagerError] = useState('');
  const [managerAuthorized, setManagerAuthorized] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState<string | null>(null);
  const [saleResult, setSaleResult] = useState<Sale | null>(null);

  useEffect(() => {
    void loadProducts();
    void loadCustomers();
    void loadCashRegister();
  }, [loadProducts, loadCustomers, loadCashRegister]);

  const categories = useMemo(() => ['Todas', ...new Set(products.map(p => p.categoria))], [products]);
  const filteredProducts = useMemo(() => products.filter(p =>
    p.activo &&
    (category === 'Todas' || p.categoria === category) &&
    (p.nombre.toLowerCase().includes(search.toLowerCase()) || p.codigo.toLowerCase().includes(search.toLowerCase()))
  ), [products, search, category]);

  const cartSubtotal = cart.reduce((sum, item) =>
    sum + item.producto.precio * item.cantidad * (1 - item.descuento / 100), 0);
  const cartIVA = cartSubtotal * 0.13;
  const cartTotal = cartSubtotal + cartIVA;

  const isManager = currentUser?.role === 'gerente';
  const isBlocked = !cashRegister?.abierta;
  const canGrantDiscount = isManager;

  const handleDiscountChange = (productId: string, value: number) => {
    const descuento = Math.min(100, Math.max(0, value));
    if (descuento > DISCOUNT_LIMIT && !managerAuthorized && !isManager) {
      setPendingDiscount({ productId, descuento });
      setShowManagerAuth(true);
      return;
    }
    updateCartDiscount(productId, descuento);
  };

  const handleManagerAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setManagerError('');
    const ok = await useStore.getState().login(managerEmail, managerPassword);
    if (!ok) {
      setManagerError('Credenciales de gerente inválidas');
      return;
    }
    const user = useStore.getState().currentUser;
    if (user?.role !== 'gerente') {
      setManagerError('El usuario autenticado no es gerente');
      return;
    }
    setManagerAuthorized(true);
    setShowManagerAuth(false);
    setManagerEmail('');
    setManagerPassword('');
    if (pendingDiscount) {
      updateCartDiscount(pendingDiscount.productId, pendingDiscount.descuento);
      setPendingDiscount(null);
    }
  };

  const handlePayment = async (metodoPago: MetodoPago) => {
    const sale = await createSale({
      customerId: selectedCustomer,
      metodoPago,
      items: cart.map(item => ({
        productId: item.producto.id,
        cantidad: item.cantidad,
        descuento: item.descuento,
      })),
    });
    if (sale) {
      setShowPayment(false);
      setSelectedCustomer(null);
      setManagerAuthorized(false);
      setSaleResult(sale);
      setTimeout(() => setSaleResult(null), 8000);
    }
  };

  if (isBlocked) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-4">
        <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center">
          <Lock size={36} className="text-red-500" />
        </div>
        <h2 className="text-xl font-semibold text-slate-800">POS Bloqueado</h2>
        <p className="text-slate-500 text-center max-w-md">
          No se pueden realizar ventas. Debe realizar la <strong>Apertura de Turno</strong> en Control de Caja primero.
        </p>
        <button
          onClick={() => navigate('/caja')}
          className="px-6 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 transition-colors"
        >
          Ir a Control de Caja
        </button>
      </div>
    );
  }

  return (
    <div className="flex gap-4 h-[calc(100vh-5rem)]">
      {saleResult && (
        <div className={`fixed top-4 right-4 z-50 px-4 py-3 rounded-lg shadow-lg text-white max-w-sm ${
          saleResult.estadoDTE === 'contingencia' ? 'bg-amber-600' : 'bg-emerald-600'
        }`}>
          <p className="flex items-center gap-2 font-medium">
            {saleResult.estadoDTE === 'contingencia' ? <WifiOff size={18} /> : <Check size={18} />}
            {saleResult.numeroFactura} — DTE {saleResult.estadoDTE.toUpperCase()}
          </p>
          <p className="text-xs mt-1 opacity-90">
            {saleResult.estadoDTE === 'contingencia'
              ? `En contingencia. Será reenviada automáticamente. ${saleResult.contingencyReason ?? ''}`
              : `Sello: ${saleResult.uuid ?? 'N/A'}`}
          </p>
        </div>
      )}

      {/* Products Grid */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="flex gap-3 mb-4 flex-wrap">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text" value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Buscar producto por nombre o código..."
              className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none text-sm"
            />
          </div>
          <div className="flex gap-1 flex-wrap">
            {categories.map(cat => (
              <button
                key={cat}
                onClick={() => setCategory(cat)}
                className={`px-3 py-2 rounded-lg text-xs font-medium transition-colors ${category === cat ? 'bg-emerald-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'}`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {status === 'loading' && products.length === 0 && (
          <div className="flex items-center justify-center py-12 text-slate-500">
            <Loader2 size={20} className="animate-spin mr-2" /> Cargando productos...
          </div>
        )}

        <div className="flex-1 overflow-y-auto grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3 content-start">
          {filteredProducts.map(product => (
            <div
              key={product.id}
              className={`bg-white rounded-xl border border-slate-200 p-3 transition-all hover:shadow-md ${product.stock === 0 ? 'opacity-50' : 'cursor-pointer hover:border-emerald-300'}`}
              onClick={() => { if (product.stock > 0) addToCart(product) }}
            >
              <div className="aspect-square bg-slate-100 rounded-lg mb-2 flex items-center justify-center">
                <Package size={32} className="text-slate-400" />
              </div>
              <h3 className="font-medium text-sm text-slate-800 line-clamp-1">{product.nombre}</h3>
              <p className="text-xs text-slate-500 mt-0.5">{product.codigo}</p>
              <div className="flex items-center justify-between mt-2">
                <span className="text-emerald-600 font-bold text-sm">{formatCurrency(product.precio)}</span>
                <span className={`text-xs px-1.5 py-0.5 rounded ${product.stock === 0 ? 'bg-red-100 text-red-600' : product.stock <= product.stockMinimo ? 'bg-amber-100 text-amber-600' : 'bg-slate-100 text-slate-600'}`}>
                  {product.stock === 0 ? 'Sin stock' : `Stock: ${product.stock}`}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Cart */}
      <div className="w-80 bg-white rounded-xl border border-slate-200 flex flex-col overflow-hidden">
        <div className="px-4 py-3 border-b border-slate-200 flex items-center gap-2">
          <ShoppingCart size={18} className="text-emerald-600" />
          <h3 className="font-semibold text-sm">Carrito ({cart.length})</h3>
          {cart.length > 0 && (
            <button onClick={clearCart} className="ml-auto text-xs text-red-500 hover:text-red-700">Limpiar</button>
          )}
        </div>

        <div className="px-4 py-2 border-b border-slate-200">
          <label className="text-xs text-slate-500 flex items-center gap-1 mb-1">
            <UserRound size={13} /> Cliente
          </label>
          <select
            value={selectedCustomer ?? ''}
            onChange={e => setSelectedCustomer(e.target.value || null)}
            className="w-full px-2 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-emerald-500"
          >
            <option value="">Consumidor Final</option>
            {customers.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
        </div>

        <div className="flex-1 overflow-y-auto px-4 py-2 space-y-2">
          {cart.length === 0 && (
            <div className="text-center py-8 text-slate-400 text-sm">
              <ShoppingCart size={32} className="mx-auto mb-2 text-slate-300" />
              Agregue productos
            </div>
          )}
          {cart.map(item => (
            <div key={item.producto.id} className="bg-slate-50 rounded-lg p-3">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <p className="text-sm font-medium text-slate-800 line-clamp-1">{item.producto.nombre}</p>
                  <p className="text-xs text-slate-500">{formatCurrency(item.producto.precio)} c/u</p>
                </div>
                <button onClick={() => removeFromCart(item.producto.id)} className="text-slate-400 hover:text-red-500 ml-2">
                  <Trash2 size={14} />
                </button>
              </div>
              <div className="flex items-center gap-2 mt-2">
                <button
                  onClick={() => updateCartQuantity(item.producto.id, item.cantidad - 1)}
                  className="w-7 h-7 rounded bg-slate-200 flex items-center justify-center hover:bg-slate-300"
                >
                  <Minus size={12} />
                </button>
                <span className="text-sm font-medium w-8 text-center">{item.cantidad}</span>
                <button
                  onClick={() => updateCartQuantity(item.producto.id, item.cantidad + 1)}
                  className="w-7 h-7 rounded bg-slate-200 flex items-center justify-center hover:bg-slate-300"
                >
                  <Plus size={12} />
                </button>
                <span className="ml-auto text-sm font-semibold">
                  {formatCurrency(item.producto.precio * item.cantidad * (1 - item.descuento / 100))}
                </span>
              </div>
              <div className="flex items-center gap-2 mt-1.5">
                <label className="text-xs text-slate-500">Dto:</label>
                <input
                  type="number" min={0} max={100} step="1" value={item.descuento}
                  onChange={e => handleDiscountChange(item.producto.id, Number(e.target.value))}
                  className="w-14 px-2 py-1 border border-slate-200 rounded text-xs text-center"
                />
                <span className="text-xs text-slate-500">%</span>
                {item.descuento > DISCOUNT_LIMIT && !isManager && !managerAuthorized && (
                  <AlertTriangle size={12} className="text-amber-500" />
                )}
              </div>
            </div>
          ))}
        </div>

        <div className="border-t border-slate-200 px-4 py-3 space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">Subtotal</span>
            <span>{formatCurrency(cartSubtotal)}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">IVA (13%)</span>
            <span>{formatCurrency(cartIVA)}</span>
          </div>
          <div className="flex justify-between text-base font-bold border-t border-slate-200 pt-2">
            <span>Total</span>
            <span className="text-emerald-600">{formatCurrency(cartTotal)}</span>
          </div>
          <p className="text-[10px] text-slate-400">Totales referenciales; el backend recalcula precios e IVA.</p>

          {cart.length > 0 && (
            <button
              onClick={() => setShowPayment(true)}
              className="w-full py-3 bg-emerald-600 text-white rounded-lg font-semibold hover:bg-emerald-700 transition-colors"
            >
              Cobrar {formatCurrency(cartTotal)}
            </button>
          )}
        </div>
      </div>

      {/* Payment Modal */}
      {showPayment && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Método de Pago</h3>
              <button onClick={() => setShowPayment(false)} className="text-slate-400 hover:text-slate-600">
                <X size={20} />
              </button>
            </div>

            <div className="bg-slate-50 rounded-lg p-4 mb-4 text-center">
              <p className="text-sm text-slate-500">Total estimado</p>
              <p className="text-3xl font-bold text-emerald-600">{formatCurrency(cartTotal)}</p>
              <p className="text-xs text-slate-400 mt-1">Recálculo verificado por Backend</p>
            </div>

            {(managerAuthorized || isManager) && cart.some(i => i.descuento > DISCOUNT_LIMIT) && (
              <p className="text-xs text-emerald-700 bg-emerald-50 p-2 rounded mb-3 flex items-center gap-1">
                <Check size={13} /> Descuento superior a 10% autorizado
              </p>
            )}

            <div className="space-y-2">
              {(['efectivo', 'tarjeta', 'transferencia'] as const).map(metodo => (
                <button
                  key={metodo}
                  onClick={() => void handlePayment(metodo)}
                  disabled={status === 'loading'}
                  className="w-full py-3 border-2 border-slate-200 rounded-lg font-medium text-slate-700 hover:border-emerald-500 hover:bg-emerald-50 transition-colors capitalize disabled:opacity-60"
                >
                  {status === 'loading' ? <Loader2 size={16} className="inline animate-spin mr-2" /> : null}
                  {metodo === 'efectivo' ? '💵 Efectivo' : metodo === 'tarjeta' ? '💳 Tarjeta' : '🏦 Transferencia'}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Manager Auth Modal */}
      {showManagerAuth && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold flex items-center gap-2">
                <Lock size={18} /> Autorización de Gerente
              </h3>
              <button onClick={() => {
                setShowManagerAuth(false); setManagerError(''); setManagerPassword(''); setManagerEmail(''); setPendingDiscount(null);
              }} className="text-slate-400 hover:text-slate-600">
                <X size={20} />
              </button>
            </div>
            <p className="text-sm text-slate-500 mb-4">
              El descuento supera el límite de {DISCOUNT_LIMIT}%. Ingrese credenciales de gerente;
              el servidor validará el rol según el token.
            </p>
            {!canGrantDiscount && (
              <form onSubmit={handleManagerAuth} className="space-y-3">
                <input
                  type="email" value={managerEmail} onChange={e => setManagerEmail(e.target.value)} required
                  placeholder="Email del gerente"
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                />
                <input
                  type="password" value={managerPassword} onChange={e => setManagerPassword(e.target.value)} required
                  placeholder="Contraseña"
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                />
                {managerError && <p className="text-red-600 text-xs">{managerError}</p>}
                <button type="submit" className="w-full py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700">
                  Autorizar
                </button>
              </form>
            )}
            {canGrantDiscount && (
              <p className="text-sm text-emerald-700">Está autenticado como gerente; descuentos &gt;10% permitidos.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
