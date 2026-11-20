import { useEffect, useMemo, useState } from 'react';
import { useStore } from '../stores/useStore';
import { formatCurrency, formatDate } from '../utils/helpers';
import { DEMO_CATEGORIES } from '../mockData';
import { AlertTriangle, Search, Settings, History, X, Plus, Loader2 } from 'lucide-react';
import type { TipoAjuste } from '../types';

export default function InventarioPage() {
  const {
    products, kardex, status, currentUser, addInventoryAdjustment, createProduct,
    loadProducts, loadKardex,
  } = useStore();
  const [search, setSearch] = useState('');
  const [filterAlert, setFilterAlert] = useState(false);
  const [showAjuste, setShowAjuste] = useState<string | null>(null);
  const [showKardex, setShowKardex] = useState<string | null>(null);
  const [showNuevo, setShowNuevo] = useState(false);
  const [ajusteForm, setAjusteForm] = useState({
    tipo: 'ajuste' as TipoAjuste | 'entrada' | 'salida',
    cantidad: '',
    justificacion: '',
  });
  const [nuevoForm, setNuevoForm] = useState({
    codigo: '', nombre: '', categoria: 'Accesorios', descripcion: '',
    precio: '', costo: '', stockMinimo: '',
  });

  useEffect(() => {
    void loadProducts();
    void loadKardex();
  }, [loadProducts, loadKardex]);

  const filtered = useMemo(() => products.filter(p => {
    const matchSearch = p.nombre.toLowerCase().includes(search.toLowerCase()) || p.codigo.toLowerCase().includes(search.toLowerCase());
    const matchAlert = !filterAlert || p.stock <= p.stockMinimo;
    return matchSearch && matchAlert;
  }), [products, search, filterAlert]);

  const lowStock = useMemo(() => products.filter(p => p.stock <= p.stockMinimo), [products]);
  const canManage = currentUser?.role === 'admin_bodega' || currentUser?.role === 'gerente';

  const handleAjuste = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!showAjuste) return;
    const cantidad = Number(ajusteForm.cantidad);
    if (!cantidad || Number.isNaN(cantidad)) return;
    const tipo = ajusteForm.tipo === 'entrada'
      ? 'entrada'
      : ajusteForm.tipo === 'salida'
        ? 'salida'
        : ajusteForm.tipo === 'dano' || ajusteForm.tipo === 'perdida'
          ? 'salida'
          : 'ajuste';
    const justificacion = ajusteForm.tipo === 'dano' || ajusteForm.tipo === 'perdida'
      ? `[${ajusteForm.tipo === 'dano' ? 'Daño' : 'Pérdida'}] ${ajusteForm.justificacion}`
      : ajusteForm.justificacion;
    const signo = ajusteForm.tipo === 'entrada' ? 1 : -1;
    const entry = await addInventoryAdjustment({
      productId: showAjuste,
      tipo,
      cantidad: Math.abs(cantidad) * signo,
      justificacion,
    });
    if (entry) {
      setShowAjuste(null);
      setAjusteForm({ tipo: 'ajuste', cantidad: '', justificacion: '' });
      await loadProducts();
    }
  };

  const handleNuevoProducto = async (e: React.FormEvent) => {
    e.preventDefault();
    const created = await createProduct({
      codigo: nuevoForm.codigo.trim(),
      nombre: nuevoForm.nombre.trim(),
      categoria: nuevoForm.categoria,
      descripcion: nuevoForm.descripcion.trim(),
      precio: Number(nuevoForm.precio),
      costo: Number(nuevoForm.costo),
      stockMinimo: Number(nuevoForm.stockMinimo),
    });
    if (created) {
      setShowNuevo(false);
      setNuevoForm({ codigo: '', nombre: '', categoria: 'Accesorios', descripcion: '', precio: '', costo: '', stockMinimo: '' });
    }
  };

  const openKardex = (productId: string) => {
    setShowKardex(productId);
    void loadKardex(productId);
  };

  const productKardex = showKardex ? kardex.filter(k => k.productoId === showKardex) : kardex;

  return (
    <div className="space-y-6">
      {status === 'loading' && products.length === 0 && (
        <div className="flex items-center gap-2 text-slate-500 text-sm"><Loader2 size={16} className="animate-spin" /> Cargando inventario...</div>
      )}
      {/* Filtros */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[250px]">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text" value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Buscar producto..."
            className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
          />
        </div>
        <button
          onClick={() => setFilterAlert(!filterAlert)}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${filterAlert ? 'bg-red-100 text-red-700 border border-red-300' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'}`}
        >
          <AlertTriangle size={16} /> Stock Bajo
        </button>
        {canManage && (
          <button
            onClick={() => setShowNuevo(true)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-medium bg-emerald-600 text-white hover:bg-emerald-700"
          >
            <Plus size={16} /> Nuevo Producto
          </button>
        )}
      </div>

      {/* Alertas de Stock Bajo */}
      {!filterAlert && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
          <h3 className="text-sm font-semibold text-amber-800 flex items-center gap-2 mb-2">
            <AlertTriangle size={16} /> Productos por debajo del stock mínimo
          </h3>
          <div className="flex flex-wrap gap-2">
            {lowStock.map(p => (
              <span key={p.id} className="px-2.5 py-1 bg-white border border-amber-300 rounded-full text-xs text-amber-700 font-medium">
                {p.nombre} ({p.stock}/{p.stockMinimo})
              </span>
            ))}
            {lowStock.length === 0 && (
              <span className="text-sm text-amber-600">Todos los productos tienen stock adecuado</span>
            )}
          </div>
        </div>
      )}

      {/* Tabla de Inventario */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Código</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Producto</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Categoría</th>
                <th className="text-right py-3 px-4 text-slate-500 font-medium">Costo</th>
                <th className="text-right py-3 px-4 text-slate-500 font-medium">Precio</th>
                <th className="text-center py-3 px-4 text-slate-500 font-medium">Stock</th>
                <th className="text-center py-3 px-4 text-slate-500 font-medium">Mínimo</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Estado</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(product => {
                const isLow = product.stock <= product.stockMinimo;
                const isZero = product.stock === 0;
                return (
                  <tr key={product.id} className={`border-b border-slate-100 hover:bg-slate-50 ${isZero ? 'bg-red-50' : isLow ? 'bg-amber-50/50' : ''}`}>
                    <td className="py-3 px-4 font-mono text-xs">{product.codigo}</td>
                    <td className="py-3 px-4">
                      <p className="font-medium">{product.nombre}</p>
                      <p className="text-xs text-slate-500 line-clamp-1">{product.descripcion}</p>
                    </td>
                    <td className="py-3 px-4 text-slate-600">{product.categoria}</td>
                    <td className="py-3 px-4 text-right">{formatCurrency(product.costo)}</td>
                    <td className="py-3 px-4 text-right font-medium">{formatCurrency(product.precio)}</td>
                    <td className="py-3 px-4 text-center">
                      <span className={`font-bold ${isZero ? 'text-red-600' : isLow ? 'text-amber-600' : 'text-slate-800'}`}>{product.stock}</span>
                    </td>
                    <td className="py-3 px-4 text-center text-slate-500">{product.stockMinimo}</td>
                    <td className="py-3 px-4">
                      {isZero ? (
                        <span className="px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-600">Sin Stock</span>
                      ) : isLow ? (
                        <span className="px-2 py-1 rounded-full text-xs font-medium bg-amber-100 text-amber-600 flex items-center gap-1 w-fit">
                          <AlertTriangle size={10} /> Bajo
                        </span>
                      ) : (
                        <span className="px-2 py-1 rounded-full text-xs font-medium bg-emerald-100 text-emerald-600">OK</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex gap-2">
                        {canManage && (
                          <button onClick={() => setShowAjuste(product.id)} className="text-slate-500 hover:text-amber-600" title="Ajuste Manual">
                            <Settings size={16} />
                          </button>
                        )}
                        <button onClick={() => openKardex(product.id)} className="text-slate-500 hover:text-blue-600" title="Ver Kardex">
                          <History size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Ajuste Manual */}
      {showAjuste && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold flex items-center gap-2">
                <Settings size={18} /> Ajuste Manual de Inventario
              </h3>
              <button onClick={() => setShowAjuste(null)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>
            {(() => {
              const product = products.find(p => p.id === showAjuste);
              if (!product) return null;
              return (
                <>
                  <div className="bg-slate-50 rounded-lg p-3 mb-4 text-sm">
                    <p><strong>Producto:</strong> {product.nombre}</p>
                    <p><strong>Stock actual:</strong> {product.stock}</p>
                  </div>
                  <form onSubmit={handleAjuste} className="space-y-3">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Tipo de Ajuste *</label>
                      <select
                        value={ajusteForm.tipo}
                        onChange={e => setAjusteForm({ ...ajusteForm, tipo: e.target.value as TipoAjuste | 'entrada' | 'salida' })}
                        className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                      >
                        <option value="entrada">Entrada</option>
                        <option value="salida">Salida</option>
                        <option value="ajuste">Ajuste</option>
                        <option value="dano">Daño</option>
                        <option value="perdida">Pérdida</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Cantidad *</label>
                      <input
                        type="number" step="1" min="1" value={ajusteForm.cantidad}
                        onChange={e => setAjusteForm({ ...ajusteForm, cantidad: e.target.value })} required
                        className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Justificación (Obligatoria) *</label>
                      <textarea
                        value={ajusteForm.justificacion}
                        onChange={e => setAjusteForm({ ...ajusteForm, justificacion: e.target.value })} required
                        rows={3}
                        className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none resize-none"
                        placeholder="Describa el motivo del ajuste..."
                      />
                    </div>
                    <div className="flex gap-3 pt-2">
                      <button type="button" onClick={() => setShowAjuste(null)} className="flex-1 py-2.5 border border-slate-300 rounded-lg font-medium text-slate-700 hover:bg-slate-50">
                        Cancelar
                      </button>
                      <button type="submit" disabled={status === 'loading'} className="flex-1 py-2.5 bg-amber-600 text-white rounded-lg font-medium hover:bg-amber-700 disabled:opacity-60">
                        Registrar Ajuste
                      </button>
                    </div>
                  </form>
                </>
              );
            })()}
          </div>
        </div>
      )}

      {/* Modal Nuevo Producto */}
      {showNuevo && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold flex items-center gap-2"><Plus size={18} /> Nuevo Producto</h3>
              <button onClick={() => setShowNuevo(false)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>
            <form onSubmit={handleNuevoProducto} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">SKU / Código *</label>
                  <input value={nuevoForm.codigo} onChange={e => setNuevoForm({ ...nuevoForm, codigo: e.target.value })} required
                    className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-emerald-500" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Categoría *</label>
                  <select value={nuevoForm.categoria} onChange={e => setNuevoForm({ ...nuevoForm, categoria: e.target.value })}
                    className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-emerald-500">
                    {DEMO_CATEGORIES.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Nombre *</label>
                <input value={nuevoForm.nombre} onChange={e => setNuevoForm({ ...nuevoForm, nombre: e.target.value })} required
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-emerald-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Descripción</label>
                <input value={nuevoForm.descripcion} onChange={e => setNuevoForm({ ...nuevoForm, descripcion: e.target.value })}
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-emerald-500" />
              </div>
              <div className="grid grid-cols-3 gap-3">
                {(['precio', 'costo', 'stockMinimo'] as const).map(field => (
                  <div key={field}>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                      {field === 'precio' ? 'Precio' : field === 'costo' ? 'Costo' : 'Stock mínimo'} *
                    </label>
                    <input type="number" step="0.01" min="0" value={nuevoForm[field]}
                      onChange={e => setNuevoForm({ ...nuevoForm, [field]: e.target.value })} required
                      className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-emerald-500" />
                  </div>
                ))}
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowNuevo(false)} className="flex-1 py-2.5 border border-slate-300 rounded-lg font-medium text-slate-700 hover:bg-slate-50">Cancelar</button>
                <button type="submit" disabled={status === 'loading'} className="flex-1 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 disabled:opacity-60">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Kardex */}
      {showKardex && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[80vh] flex flex-col">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold flex items-center gap-2">
                <History size={18} /> Kardex - {products.find(p => p.id === showKardex)?.nombre ?? ''}
              </h3>
              <button onClick={() => setShowKardex(null)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>
            <div className="overflow-y-auto flex-1">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200">
                    <th className="text-left py-2 px-3 text-slate-500 font-medium">Fecha</th>
                    <th className="text-left py-2 px-3 text-slate-500 font-medium">Tipo</th>
                    <th className="text-right py-2 px-3 text-slate-500 font-medium">Cantidad</th>
                    <th className="text-right py-2 px-3 text-slate-500 font-medium">Stock Ant.</th>
                    <th className="text-right py-2 px-3 text-slate-500 font-medium">Stock Post.</th>
                    <th className="text-left py-2 px-3 text-slate-500 font-medium">Justificación</th>
                  </tr>
                </thead>
                <tbody>
                  {productKardex.length === 0 && (
                    <tr><td colSpan={6} className="py-8 text-center text-slate-400">Sin movimientos registrados</td></tr>
                  )}
                  {productKardex.slice().sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime()).map(entry => (
                    <tr key={entry.id} className="border-b border-slate-100">
                      <td className="py-2 px-3">{formatDate(entry.fecha)}</td>
                      <td className="py-2 px-3">
                        <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                          entry.tipo === 'compra' || entry.tipo === 'entrada' || entry.tipo === 'anulacion' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'
                        }`}>{entry.tipo}</span>
                      </td>
                      <td className={`py-2 px-3 text-right font-medium ${entry.cantidad > 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                        {entry.cantidad > 0 ? '+' : ''}{entry.cantidad}
                      </td>
                      <td className="py-2 px-3 text-right">{entry.stockAnterior}</td>
                      <td className="py-2 px-3 text-right">{entry.stockPosterior}</td>
                      <td className="py-2 px-3 text-slate-600 text-xs">{entry.justificacion}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
