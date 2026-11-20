import { useState } from 'react';
import { useStore } from '../stores/useStore';
import { Lock, User, AlertCircle, Loader2 } from 'lucide-react';
import { DEMO_LOGIN_EMAILS } from '../mockData';
import { landingRoute } from '../lib/roles';
import { useNavigate } from 'react-router-dom';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login, status } = useStore();
  const navigate = useNavigate();
  const loading = status === 'loading';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (await login(email, password)) {
      const user = useStore.getState().currentUser;
      navigate(landingRoute(user?.role), { replace: true });
    } else {
      setError(useStore.getState().error || 'Credenciales inválidas');
    }
  };

  const quickFillEmail = (userEmail: string) => {
    setEmail(userEmail);
    setPassword('');
    setError('');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-emerald-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-emerald-500 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <span className="text-white font-bold text-2xl">DA</span>
          </div>
          <h1 className="text-3xl font-bold text-white">Daash ERP</h1>
          <p className="text-slate-400 mt-1">Adventure Works El Salvador</p>
          <p className="text-slate-500 text-sm mt-1">Facturación Electrónica & Punto de Venta</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          <h2 className="text-xl font-semibold text-slate-800 mb-6">Iniciar Sesión</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Correo Electrónico</label>
              <div className="relative">
                <User size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="email" value={email} onChange={e => setEmail(e.target.value)} required
                  className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none text-sm"
                  placeholder="correo@adventureworks.com"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Contraseña</label>
              <div className="relative">
                <Lock size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="password" value={password} onChange={e => setPassword(e.target.value)} required
                  className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none text-sm"
                  placeholder="••••"
                />
              </div>
            </div>

            {error && (
              <div className="flex items-center gap-2 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
                <AlertCircle size={16} />
                {error}
              </div>
            )}

            <button type="submit" disabled={loading} className="w-full py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 transition-colors disabled:opacity-60 flex items-center justify-center gap-2">
              {loading && <Loader2 size={16} className="animate-spin" />}
              Ingresar
            </button>
          </form>

          <div className="mt-6 pt-4 border-t border-slate-200">
            <p className="text-xs text-slate-500 text-center mb-3">Acceso rápido: rellena el correo; ingresa tu contraseña</p>
            <div className="grid grid-cols-2 gap-2">
              {DEMO_LOGIN_EMAILS.map(({ email: demoEmail, role, label }) => (
                <button
                  key={demoEmail}
                  onClick={() => quickFillEmail(demoEmail)}
                  className="px-3 py-2 bg-slate-100 hover:bg-slate-200 rounded-lg text-xs text-slate-700 transition-colors text-left"
                >
                  <span className="font-medium block">{label}</span>
                  <span className="text-slate-500">{demoEmail.split('@')[0]}</span>
                  <span className="sr-only">Rol {role}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
