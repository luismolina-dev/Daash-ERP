import { useEffect, useMemo, useState } from 'react';
import { useStore } from '../stores/useStore';
import { formatCurrency, formatDate } from '../utils/helpers';
import {
  FileText, CheckCircle, AlertTriangle, XCircle, RotateCcw, X, Search, RefreshCw,
} from 'lucide-react';
import type { EstadoDTE } from '../types';

export default function VentasPage() {
  const {
    sales, currentUser, contingencyQueue, loadSales, loadContingencies, anularSale,
  } = useStore();
  const [showAnular, setShowAnular] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterEstado, setFilterEstado] = useState<string>('todos');
  const [showQueue, setShowQueue] = useState(false);

  useEffect(() => {
    void loadSales();
    if (currentUser?.role === 'gerente' || currentUser?.role === 'auditor') void loadContingencies();
  }, [loadSales, loadContingencies, currentUser?.role]);

  const canAnular = currentUser?.role === 'gerente';
  const canSeeQueue = currentUser?.role === 'gerente' || currentUser?.role === 'auditor';

  const filteredSales = useMemo(() => sales.filter(s => {
    const matchSearch = s.numeroFactura.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.clienteNombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (s.uuid ?? '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchEstado = filterEstado === 'todos' || s.estadoDTE === filterEstado;
    return matchSearch && matchEstado;
  }), [sales, searchTerm, filterEstado]);

  const estadoBadge = (estado: EstadoDTE, anulada: boolean) => {
    if (anulada) return <span className="px-2 py-1 rounded-full text-xs font-medium bg-slate-100 text-slate-500">Anulada</span>;
    switch (estado) {
      case 'aceptado':
        return <span className="px-2 py-1 rounded-full text-xs font-medium bg-emerald-100 text-emerald-700 flex items-center gap-1"><CheckCircle size={12} /> Aceptado</span>;
      case 'contingencia':
        return <span className="px-2 py-1 rounded-full text-xs font-medium bg-amber-100 text-amber-700 flex items-center gap-1"><AlertTriangle size={12} /> Contingencia</span>;
      case 'rechazado':
        return <span className="px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-700 flex items-center gap-1"><XCircle size={12} /> Rechazado</span>;
    }
  };

  const handleAnular = async (saleId: string) => {
    if (await anularSale(saleId)) setShowAnular(null);
  };

  const refreshAll = () => { void loadSales(); void loadContingencies() };

  return (
    <div className="space-y-6">
      {/* Filtros */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[250px]">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text" value={searchTerm} onChange={e => setSearchTerm(e.target.value)}
            placeholder="Buscar por factura, cliente o UUID..."
            className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
          />
        </div>
        <div className="flex gap-1">
          {['todos', 'aceptado', 'contingencia', 'rechazado'].map(estado => (
            <button
              key={estado}
              onClick={() => setFilterEstado(estado)}
              className={`px-3 py-2 rounded-lg text-xs font-medium capitalize transition-colors ${filterEstado === estado ? 'bg-emerald-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'}`}
            >
              {estado === 'todos' ? 'Todos' : estado}
            </button>
          ))}
        </div>
        <button onClick={refreshAll} className="px-3 py-2 rounded-lg text-xs font-medium bg-white text-slate-600 border border-slate-200 hover:bg-slate-50 flex items-center gap-1">
          <RefreshCw size={13} /> Actualizar
        </button>
        {canSeeQueue && (
          <button onClick={() => setShowQueue(!showQueue)} className="px-3 py-2 rounded-lg text-xs font-medium bg-amber-100 text-amber-700 hover:bg-amber-200 flex items-center gap-1">
            <AlertTriangle size={13} /> Cola de contingencia ({contingencyQueue.filter(q => q.estado !== 'enviado').length})
          </button>
        )}
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div className="bg-white rounded-xl p-4 border border-slate-200">
          <p className="text-xs text-slate-500">Total Facturas</p>
          <p className="text-2xl font-bold text-slate-800">{sales.length}</p>
        </div>
        <div className="bg-white rounded-xl p-4 border border-slate-200">
          <p className="text-xs text-slate-500">Aceptadas</p>
          <p className="text-2xl font-bold text-emerald-600">{sales.filter(s => s.estadoDTE === 'aceptado' && !s.anulada).length}</p>
        </div>
        <div className="bg-white rounded-xl p-4 border border-slate-200">
          <p className="text-xs text-slate-500">En Contingencia</p>
          <p className="text-2xl font-bold text-amber-600">{sales.filter(s => s.estadoDTE === 'contingencia').length}</p>
        </div>
        <div className="bg-white rounded-xl p-4 border border-slate-200">
          <p className="text-xs text-slate-500">Anuladas</p>
          <p className="text-2xl font-bold text-red-600">{sales.filter(s => s.anulada).length}</p>
        </div>
      </div>

      {/* Cola de contingencia */}
      {showQueue && canSeeQueue && (
        <div className="bg-white rounded-xl border border-amber-200 p-5">
          <h3 className="text-sm font-semibold text-amber-800 flex items-center gap-2 mb-3">
            <AlertTriangle size={16} /> Cola de reintento DTE
          </h3>
          {contingencyQueue.length === 0 ? (
            <p className="text-sm text-slate-500">No hay eventos en la cola.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200">
                    <th className="text-left py-2 px-3 text-slate-500 font-medium">Venta</th>
                    <th className="text-left py-2 px-3 text-slate-500 font-medium">Intentos</th>
                    <th className="text-left py-2 px-3 text-slate-500 font-medium">Estado</th>
                    <th className="text-left py-2 px-3 text-slate-500 font-medium">Próximo reintento</th>
                    <th className="text-left py-2 px-3 text-slate-500 font-medium">Último error</th>
                  </tr>
                </thead>
                <tbody>
                  {contingencyQueue.map(q => (
                    <tr key={q.id} className="border-b border-slate-100">
                      <td className="py-2 px-3 font-mono text-xs">{q.saleId}</td>
                      <td className="py-2 px-3">{q.intentos} / {q.maxIntentos}</td>
                      <td className="py-2 px-3">
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                          q.estado === 'enviado' ? 'bg-emerald-100 text-emerald-700' : q.estado === 'fallido' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'
                        }`}>{q.estado}</span>
                      </td>
                      <td className="py-2 px-3">{formatDate(q.proximoReintento)}</td>
                      <td className="py-2 px-3 text-xs text-slate-500">{q.ultimoError || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="text-xs text-slate-500 mt-3">El job del servidor reintenta automáticamente cada 5 minutos; use “Actualizar” para ver el estado más reciente.</p>
        </div>
      )}

      {/* Tabla de Ventas */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Factura</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Fecha</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Cliente</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Método</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Total</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Estado DTE</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">UUID / Nota crédito</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filteredSales.length === 0 && (
                <tr><td colSpan={8} className="py-8 text-center text-slate-400">No hay ventas registradas</td></tr>
              )}
              {filteredSales.map(sale => (
                <tr key={sale.id} className="border-b border-slate-100 hover:bg-slate-50">
                  <td className="py-3 px-4 font-medium flex items-center gap-1"><FileText size={13} className="text-slate-400" />{sale.numeroFactura}</td>
                  <td className="py-3 px-4 text-slate-600">{formatDate(sale.fecha)}</td>
                  <td className="py-3 px-4">{sale.clienteNombre}</td>
                  <td className="py-3 px-4 capitalize">{sale.metodoPago}</td>
                  <td className="py-3 px-4 font-semibold">{formatCurrency(sale.total)}</td>
                  <td className="py-3 px-4">{estadoBadge(sale.estadoDTE, sale.anulada)}</td>
                  <td className="py-3 px-4 text-xs text-slate-500 font-mono max-w-[160px] truncate">
                    {sale.notaCreditoId ? `NC: ${sale.notaCreditoId}` : (sale.uuid || '—')}
                  </td>
                  <td className="py-3 px-4">
                    {canAnular && !sale.anulada && (
                      <button
                        onClick={() => setShowAnular(sale.id)}
                        className="text-red-500 hover:text-red-700 text-xs flex items-center gap-1"
                      >
                        <RotateCcw size={12} /> Anular
                      </button>
                    )}
                    {!canAnular && !sale.anulada && <span className="text-xs text-slate-400">Solo gerente</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Anulación */}
      {showAnular && canAnular && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold flex items-center gap-2 text-red-600">
                <RotateCcw size={18} /> Anular Factura
              </h3>
              <button onClick={() => setShowAnular(null)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>
            {(() => {
              const sale = sales.find(s => s.id === showAnular);
              if (!sale) return null;
              return (
                <>
                  <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
                    <p className="text-sm text-red-700">
                      Se anulará la factura <strong>{sale.numeroFactura}</strong> y se emitirá una <strong>Nota de Crédito</strong>.
                      El servidor recalculará el inventario y el DTE.
                    </p>
                  </div>
                  <div className="bg-slate-50 rounded-lg p-3 mb-4 text-sm space-y-1">
                    <p><strong>UUID:</strong> {sale.uuid || 'Sin UUID (Contingencia)'}</p>
                    <p><strong>Estado DTE:</strong> {sale.estadoDTE}</p>
                    <p><strong>Cliente:</strong> {sale.clienteNombre}</p>
                    <p><strong>Total:</strong> {formatCurrency(sale.total)}</p>
                    <p className="text-xs text-slate-500 mt-2">Productos que se reingresarán:</p>
                    {sale.items.map((item, idx) => (
                      <p key={idx} className="text-xs text-slate-600">• {item.nombre} x{item.cantidad}</p>
                    ))}
                  </div>
                  <div className="flex gap-3">
                    <button onClick={() => setShowAnular(null)} className="flex-1 py-2.5 border border-slate-300 rounded-lg font-medium text-slate-700 hover:bg-slate-50">Cancelar</button>
                    <button onClick={() => void handleAnular(showAnular)} className="flex-1 py-2.5 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700">Confirmar Anulación</button>
                  </div>
                </>
              );
            })()}
          </div>
        </div>
      )}
    </div>
  );
}
