import { useEffect } from 'react';
import { useStore } from '../stores/useStore';
import { Menu, AlertTriangle, Wifi, WifiOff, X, CheckCircle } from 'lucide-react';

interface HeaderProps {
  onMenuToggle: () => void;
  title: string;
}

export default function Header({ onMenuToggle, title }: HeaderProps) {
  const {
    simulatedDowntime, toggleDowntime, currentUser, cashRegister,
    message, error, setMessage, clearError,
  } = useStore();
  const canToggleDowntime = currentUser?.role === 'gerente' || currentUser?.role === 'auditor';

  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => setMessage(null), 4000);
    return () => clearTimeout(timer);
  }, [message, setMessage]);

  return (
    <>
      <header className="bg-white border-b border-slate-200 px-4 py-3 flex items-center justify-between sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <button onClick={onMenuToggle} className="lg:hidden text-slate-600 hover:text-slate-900">
            <Menu size={22} />
          </button>
          <h2 className="text-lg font-semibold text-slate-800">{title}</h2>
        </div>

        <div className="flex items-center gap-3">
          {cashRegister?.abierta && (
            <span className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 bg-emerald-100 text-emerald-700 rounded-full text-xs font-medium">
              <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
              Caja Abierta
            </span>
          )}
          {!cashRegister?.abierta && currentUser?.role === 'cajero' && (
            <span className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 bg-red-100 text-red-700 rounded-full text-xs font-medium">
              <AlertTriangle size={12} />
              Caja Cerrada
            </span>
          )}

          {canToggleDowntime && (
            <button
              onClick={() => void toggleDowntime()}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                simulatedDowntime
                  ? 'bg-red-100 text-red-700 hover:bg-red-200'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
              title="Simular caída del sistema de Hacienda"
            >
              {simulatedDowntime ? <WifiOff size={14} /> : <Wifi size={14} />}
              {simulatedDowntime ? 'Hacienda Caída' : 'Hacienda OK'}
            </button>
          )}
        </div>
      </header>

      {(message || error) && (
        <div className={`fixed top-16 right-4 z-40 max-w-sm px-4 py-3 rounded-lg shadow-lg flex items-start gap-2 text-sm ${
          error ? 'bg-red-600 text-white' : 'bg-emerald-600 text-white'
        }`}>
          {error ? <AlertTriangle size={16} className="shrink-0 mt-0.5" /> : <CheckCircle size={16} className="shrink-0 mt-0.5" />}
          <span className="flex-1">{error || message}</span>
          <button onClick={() => { setMessage(null); clearError() }} aria-label="Cerrar aviso">
            <X size={14} />
          </button>
        </div>
      )}
    </>
  );
}
