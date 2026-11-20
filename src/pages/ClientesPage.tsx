import { useEffect, useState } from 'react';
import { useStore } from '../stores/useStore';
import { validateDUI, validateNIT } from '../utils/helpers';
import { X, Search, AlertCircle, UserPlus, Loader2 } from 'lucide-react';

export default function ClientesPage() {
  const { customers, status, loadCustomers, addCustomer } = useStore();
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [form, setForm] = useState({
    nombre: '', email: '', telefono: '', dui: '', nit: '', nrc: '',
    direccion: '', tipoContribuyente: 'consumidor' as 'consumidor' | 'contribuyente',
  });

  useEffect(() => { void loadCustomers() }, [loadCustomers]);

  const filtered = customers.filter(c =>
    c.nombre.toLowerCase().includes(search.toLowerCase()) ||
    c.nit.includes(search) ||
    c.dui.includes(search)
  );

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    if (!form.nombre.trim()) newErrors.nombre = 'Nombre requerido';
    if (form.dui && !validateDUI(form.dui)) newErrors.dui = 'Formato DUI inválido (00000000-0)';
    if (form.nit && !validateNIT(form.nit)) newErrors.nit = 'Formato NIT inválido (0614-123456-100-0)';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const created = await addCustomer(form);
    if (created) {
      setForm({ nombre: '', email: '', telefono: '', dui: '', nit: '', nrc: '', direccion: '', tipoContribuyente: 'consumidor' });
      setShowForm(false);
      setErrors({});
    }
  };

  return (
    <div className="space-y-6">
      {status === 'loading' && customers.length === 0 && (
        <div className="flex items-center gap-2 text-slate-500 text-sm"><Loader2 size={16} className="animate-spin" /> Cargando clientes...</div>
      )}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[250px]">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text" value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Buscar por nombre, DUI o NIT..."
            className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
          />
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 text-sm"
        >
          <UserPlus size={16} /> Nuevo Cliente
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
        <div className="bg-white rounded-xl p-4 border border-slate-200">
          <p className="text-xs text-slate-500">Total Clientes</p>
          <p className="text-2xl font-bold text-slate-800">{customers.length}</p>
        </div>
        <div className="bg-white rounded-xl p-4 border border-slate-200">
          <p className="text-xs text-slate-500">Contribuyentes</p>
          <p className="text-2xl font-bold text-blue-600">{customers.filter(c => c.tipoContribuyente === 'contribuyente').length}</p>
        </div>
        <div className="bg-white rounded-xl p-4 border border-slate-200">
          <p className="text-xs text-slate-500">Consumidores Finales</p>
          <p className="text-2xl font-bold text-slate-600">{customers.filter(c => c.tipoContribuyente === 'consumidor').length}</p>
        </div>
      </div>

      {/* Tabla */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Nombre</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Email</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Teléfono</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">DUI</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">NIT</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">NRC</th>
                <th className="text-left py-3 px-4 text-slate-500 font-medium">Tipo</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(customer => (
                <tr key={customer.id} className="border-b border-slate-100 hover:bg-slate-50">
                  <td className="py-3 px-4 font-medium">{customer.nombre}</td>
                  <td className="py-3 px-4 text-slate-600">{customer.email}</td>
                  <td className="py-3 px-4 text-slate-600">{customer.telefono}</td>
                  <td className="py-3 px-4 font-mono text-xs">{customer.dui || '—'}</td>
                  <td className="py-3 px-4 font-mono text-xs">{customer.nit || '—'}</td>
                  <td className="py-3 px-4 text-xs">{customer.nrc || '—'}</td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${customer.tipoContribuyente === 'contribuyente' ? 'bg-blue-100 text-blue-700' : 'bg-slate-100 text-slate-600'}`}>
                      {customer.tipoContribuyente === 'contribuyente' ? 'Contribuyente' : 'Consumidor'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Nuevo Cliente */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold flex items-center gap-2">
                <UserPlus size={18} /> Nuevo Cliente
              </h3>
              <button onClick={() => { setShowForm(false); setErrors({}); }} className="text-slate-400 hover:text-slate-600">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Nombre / Razón Social *</label>
                <input
                  value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} required
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Email</label>
                  <input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}
                    className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Teléfono</label>
                  <input value={form.telefono} onChange={e => setForm({ ...form, telefono: e.target.value })}
                    className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">DUI (00000000-0)</label>
                <input
                  value={form.dui} onChange={e => setForm({ ...form, dui: e.target.value })}
                  placeholder="01234567-8"
                  className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none ${errors.dui ? 'border-red-400 bg-red-50' : 'border-slate-300'}`}
                />
                {errors.dui && <p className="text-xs text-red-600 mt-1 flex items-center gap-1"><AlertCircle size={12} /> {errors.dui}</p>}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">NIT (0614-123456-100-0)</label>
                  <input
                    value={form.nit} onChange={e => setForm({ ...form, nit: e.target.value })}
                    placeholder="0614-123456-100-0"
                    className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none ${errors.nit ? 'border-red-400 bg-red-50' : 'border-slate-300'}`}
                  />
                  {errors.nit && <p className="text-xs text-red-600 mt-1 flex items-center gap-1"><AlertCircle size={12} /> {errors.nit}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">NRC</label>
                  <input value={form.nrc} onChange={e => setForm({ ...form, nrc: e.target.value })}
                    className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Dirección</label>
                <input value={form.direccion} onChange={e => setForm({ ...form, direccion: e.target.value })}
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Tipo de Contribuyente *</label>
                <select
                  value={form.tipoContribuyente} onChange={e => setForm({ ...form, tipoContribuyente: e.target.value as 'consumidor' | 'contribuyente' })}
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                >
                  <option value="consumidor">Consumidor Final</option>
                  <option value="contribuyente">Contribuyente</option>
                </select>
              </div>

              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => { setShowForm(false); setErrors({}); }} className="flex-1 py-2.5 border border-slate-300 rounded-lg font-medium text-slate-700 hover:bg-slate-50">
                  Cancelar
                </button>
                <button type="submit" disabled={status === 'loading'} className="flex-1 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 disabled:opacity-60">
                  Guardar Cliente
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
