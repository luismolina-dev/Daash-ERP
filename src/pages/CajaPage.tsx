import { useEffect, useState } from 'react';
import { useStore } from '../stores/useStore';
import { formatCurrency, formatDate } from '../utils/helpers';
import {
  Lock, Unlock, DollarSign, AlertTriangle, CheckCircle, TrendingUp,
  CreditCard, Banknote, Loader2,
} from 'lucide-react';

export default function CajaPage() {
  const {
    currentUser, cashRegister, status, openCashRegister, closeCashRegister,
    loadCashRegister, error,
  } = useStore();
  const [fondoInicial, setFondoInicial] = useState('');
  const [showCorte, setShowCorte] = useState(false);
  const [conteo, setConteo] = useState({ efectivoReal: '', tarjetaReal: '', transferenciaReal: '' });

  useEffect(() => { void loadCashRegister() }, [loadCashRegister]);

  const handleOpen = async (e: React.FormEvent) => {
    e.preventDefault();
    const fondo = Number(fondoInicial);
    if (fondo > 0 && await openCashRegister(fondo)) setFondoInicial('');
  };

  const handleCorteZ = async () => {
    const ok = await closeCashRegister(
      Number(conteo.efectivoReal) || 0,
      Number(conteo.tarjetaReal) || 0,
      Number(conteo.transferenciaReal) || 0,
    );
    if (ok) {
      setShowCorte(false);
      setConteo({ efectivoReal: '', tarjetaReal: '', transferenciaReal: '' });
    }
  };

  const arqueo = cashRegister?.arqueo;
  const canManage = currentUser?.role === 'cajero' || currentUser?.role === 'gerente';

  return (
    <div className="space-y-6 max-w-4xl">
      {error && (
        <div className="flex items-center gap-2 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
          <AlertTriangle size={16} /> {error}
        </div>
      )}
      {status === 'loading' && !cashRegister && (
        <div className="flex items-center gap-2 text-slate-500 text-sm"><Loader2 size={16} className="animate-spin" /> Cargando caja...</div>
      )}

      {/* Estado actual */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className={`rounded-xl p-6 border-2 ${cashRegister?.abierta ? 'bg-emerald-50 border-emerald-200' : 'bg-red-50 border-red-200'}`}>
          <div className="flex items-center gap-3 mb-3">
            {cashRegister?.abierta ? (
              <div className="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center">
                <Unlock size={20} className="text-emerald-600" />
              </div>
            ) : (
              <div className="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
                <Lock size={20} className="text-red-600" />
              </div>
            )}
            <div>
              <h3 className="font-semibold text-slate-800">Estado de Caja</h3>
              <p className={`text-sm font-medium ${cashRegister?.abierta ? 'text-emerald-600' : 'text-red-600'}`}>
                {cashRegister?.abierta ? 'ABIERTA' : 'CERRADA'}
              </p>
            </div>
          </div>
          {cashRegister && (
            <p className="text-xs text-slate-500">Abierta: {formatDate(cashRegister.fechaApertura)}</p>
          )}
        </div>

        <div className="bg-white rounded-xl p-6 border border-slate-200">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <DollarSign size={20} className="text-blue-600" />
            </div>
            <div>
              <h3 className="font-semibold text-slate-800">Fondo Inicial</h3>
              <p className="text-lg font-bold text-slate-800">
                {cashRegister ? formatCurrency(cashRegister.fondoInicial) : '$0.00'}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl p-6 border border-slate-200">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center">
              <TrendingUp size={20} className="text-emerald-600" />
            </div>
            <div>
              <h3 className="font-semibold text-slate-800">Total Ventas Turno</h3>
              <p className="text-lg font-bold text-emerald-600">
                {cashRegister ? formatCurrency(cashRegister.totalVentas) : '$0.00'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Apertura de Turno */}
      {!cashRegister?.abierta && canManage && (
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
            <DollarSign size={20} /> Apertura de Turno
          </h3>
          <form onSubmit={handleOpen} className="flex gap-3 items-end">
            <div className="flex-1">
              <label className="block text-sm font-medium text-slate-700 mb-1">Fondo Inicial en Efectivo</label>
              <input
                type="number" step="0.01" min="0.01" value={fondoInicial}
                onChange={e => setFondoInicial(e.target.value)} required
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                placeholder="200.00"
              />
            </div>
            <button type="submit" disabled={status === 'loading'} className="px-6 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 transition-colors disabled:opacity-60">
              Abrir Caja
            </button>
          </form>
          <p className="text-xs text-slate-500 mt-2 flex items-center gap-1">
            <AlertTriangle size={12} className="text-amber-500" />
            Debe abrir caja para habilitar el Punto de Venta.
          </p>
        </div>
      )}

      {/* Último arqueo */}
      {arqueo && (
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <CheckCircle size={20} className="text-emerald-600" /> Último Corte Z
          </h3>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
            <div><span className="text-slate-500">Efectivo esperado:</span> <strong>{formatCurrency(arqueo.efectivoEsperado)}</strong></div>
            <div><span className="text-slate-500">Efectivo real:</span> <strong>{formatCurrency(arqueo.efectivoReal)}</strong></div>
            <div><span className="text-slate-500">Tarjeta:</span> <strong>{formatCurrency(arqueo.tarjeta)}</strong></div>
            <div><span className="text-slate-500">Transferencia:</span> <strong>{formatCurrency(arqueo.transferencia)}</strong></div>
            <div><span className="text-slate-500">Total ventas:</span> <strong>{formatCurrency(arqueo.totalVentas)}</strong></div>
            <div>
              <span className="text-slate-500">Diferencia:</span>{' '}
              <strong className={arqueo.diferencia === 0 ? 'text-emerald-600' : 'text-red-600'}>
                {formatCurrency(arqueo.diferencia)}
              </strong>
            </div>
          </div>
        </div>
      )}

      {/* Desglose por Método de Pago */}
      {cashRegister?.abierta && (
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">Desglose por Método de Pago</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-slate-50 rounded-lg p-4 flex items-center gap-3">
              <Banknote size={24} className="text-emerald-600" />
              <div>
                <p className="text-xs text-slate-500">Efectivo</p>
                <p className="text-xl font-bold text-slate-800">{formatCurrency(cashRegister.totalEfectivo)}</p>
              </div>
            </div>
            <div className="bg-slate-50 rounded-lg p-4 flex items-center gap-3">
              <CreditCard size={24} className="text-blue-600" />
              <div>
                <p className="text-xs text-slate-500">Tarjeta</p>
                <p className="text-xl font-bold text-slate-800">{formatCurrency(cashRegister.totalTarjeta)}</p>
              </div>
            </div>
            <div className="bg-slate-50 rounded-lg p-4 flex items-center gap-3">
              <TrendingUp size={24} className="text-purple-600" />
              <div>
                <p className="text-xs text-slate-500">Transferencia</p>
                <p className="text-xl font-bold text-slate-800">{formatCurrency(cashRegister.totalTransferencia)}</p>
              </div>
            </div>
          </div>

          {canManage && (
            <div className="mt-6 flex gap-3">
              <button
                onClick={() => setShowCorte(true)}
                className="px-6 py-2.5 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors flex items-center gap-2"
              >
                <Lock size={16} /> Corte de Caja Z
              </button>
            </div>
          )}
        </div>
      )}

      {/* Corte Z Modal */}
      {showCorte && cashRegister?.abierta && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg p-6">
            <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
              <CheckCircle size={20} /> Arqueo Final - Corte de Caja Z
            </h3>

            <div className="space-y-3 bg-slate-50 rounded-lg p-4">
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">Fondo Inicial</span>
                <span className="font-medium">{formatCurrency(cashRegister.fondoInicial)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">+ Total Efectivo</span>
                <span className="font-medium">{formatCurrency(cashRegister.totalEfectivo)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">= Efectivo Esperado</span>
                <span className="font-bold">{formatCurrency(cashRegister.fondoInicial + cashRegister.totalEfectivo)}</span>
              </div>
              <hr />
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs text-slate-500 mb-1">Efectivo real (conteo)</label>
                  <input type="number" step="0.01" min="0" value={conteo.efectivoReal}
                    onChange={e => setConteo({ ...conteo, efectivoReal: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm" />
                </div>
                <div>
                  <label className="block text-xs text-slate-500 mb-1">Tarjeta real</label>
                  <input type="number" step="0.01" min="0" value={conteo.tarjetaReal}
                    onChange={e => setConteo({ ...conteo, tarjetaReal: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm" />
                </div>
                <div>
                  <label className="block text-xs text-slate-500 mb-1">Transferencia real</label>
                  <input type="number" step="0.01" min="0" value={conteo.transferenciaReal}
                    onChange={e => setConteo({ ...conteo, transferenciaReal: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm" />
                </div>
              </div>
              <p className="text-xs text-slate-500">La diferencia será calculada por el backend.</p>
              <hr />
              <div className="flex justify-between text-base font-bold">
                <span>Total Ventas del Turno</span>
                <span className="text-emerald-600">{formatCurrency(cashRegister.totalVentas)}</span>
              </div>
            </div>

            <div className="flex gap-3 mt-4">
              <button onClick={() => setShowCorte(false)} className="flex-1 py-2.5 border border-slate-300 rounded-lg font-medium text-slate-700 hover:bg-slate-50">
                Cancelar
              </button>
              <button onClick={() => void handleCorteZ()} disabled={status === 'loading'} className="flex-1 py-2.5 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 disabled:opacity-60">
                Confirmar Cierre
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
