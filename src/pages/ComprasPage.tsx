import { useEffect, useState } from 'react';
import { useStore } from '../stores/useStore';
import {
  Truck, Plus, X, Package, Building2, Loader2,
  Eye, Download, Mail, RefreshCw, FileText,
} from 'lucide-react';
import type { Purchase, PurchaseEmailEstado } from '../types';

interface PurchaseFormItem {
  productId: string;
  cantidad: string;
  precioUnitario: string;
}

const emailStyles: Record<PurchaseEmailEstado, string> = {
  PENDIENTE: 'bg-amber-100 text-amber-700',
  ENVIADO: 'bg-emerald-100 text-emerald-700',
  SIMULADO: 'bg-slate-100 text-slate-600',
  FALLIDO: 'bg-red-100 text-red-700',
};

const money = (value: number): string =>
  value.toLocaleString('es-SV', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function ComprasPage() {
  const {
    suppliers, products, purchases, status, currentUser,
    loadSuppliers, loadProducts, loadPurchases,
    addSupplier, addPurchase, downloadPurchasePdf, resendPurchaseEmail,
  } = useStore();
  const [tab, setTab] = useState<'compras' | 'proveedores'>('compras');
  const [showForm, setShowForm] = useState(false);
  const [showSupplierForm, setShowSupplierForm] = useState(false);
  const [form, setForm] = useState<{
    supplierId: string;
    customerEmail: string;
    items: PurchaseFormItem[];
  }>({
    supplierId: '',
    customerEmail: '',
    items: [{ productId: '', cantidad: '', precioUnitario: '' }],
  });

  useEffect(() => {
    void loadSuppliers();
    void loadProducts();
    void loadPurchases();
  }, [loadSuppliers, loadProducts, loadPurchases]);

  const handleAddItem = () => setForm({
    ...form,
    items: [...form.items, { productId: '', cantidad: '', precioUnitario: '' }],
  });
  const handleRemoveItem = (index: number) => setForm({
    ...form,
    items: form.items.filter((_, i) => i !== index),
  });
  const handleItemChange = (index: number, field: keyof PurchaseFormItem, value: string) => {
    setForm({
      ...form,
      items: form.items.map((item, i) => i === index ? { ...item, [field]: value } : item),
    });
  };

  const handleSubmitPurchase = async (e: React.FormEvent) => {
    e.preventDefault();
    const items = form.items
      .filter(item => item.productId && item.cantidad && item.precioUnitario)
      .map(item => ({
        productId: item.productId,
        cantidad: Number(item.cantidad),
        precioUnitario: Number(item.precioUnitario),
      }));
    if (!form.supplierId || !form.customerEmail || items.length === 0) return;
    const purchase = await addPurchase({
      supplierId: form.supplierId,
      customerEmail: form.customerEmail,
      items,
    });
    if (purchase) {
      setShowForm(false);
      setForm({
        supplierId: '',
        customerEmail: '',
        items: [{ productId: '', cantidad: '', precioUnitario: '' }],
      });
    }
  };

  const handleSubmitSupplier = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const ok = await addSupplier({
      nombre: String(fd.get('nombre') ?? ''),
      nit: String(fd.get('nit') ?? ''),
      nrc: String(fd.get('nrc') ?? ''),
      telefono: String(fd.get('telefono') ?? ''),
      email: String(fd.get('email') ?? ''),
      direccion: String(fd.get('direccion') ?? ''),
      contacto: String(fd.get('contacto') ?? ''),
    });
    if (ok) setShowSupplierForm(false);
  };

  const canResend = currentUser?.role === 'admin_bodega' || currentUser?.role === 'gerente';

  const renderPurchaseActions = (purchase: Purchase) => (
    <div className="flex items-center gap-1">
      <button
        title="Ver factura PDF"
        onClick={() => void downloadPurchasePdf(purchase.id, true)}
        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
      >
        <Eye size={16} />
      </button>
      <button
        title="Descargar factura PDF"
        onClick={() => void downloadPurchasePdf(purchase.id)}
        className="p-2 text-slate-600 hover:bg-slate-100 rounded-lg"
      >
        <Download size={16} />
      </button>
      {canResend && (
        <button
          title="Reenviar factura por correo"
          onClick={() => void resendPurchaseEmail(purchase.id)}
          className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-lg disabled:opacity-50"
          disabled={status === 'loading'}
        >
          <RefreshCw size={16} />
        </button>
      )}
    </div>
  );

  return (
    <div className="space-y-6">
      {/* Tabs */}
      <div className="flex gap-1 bg-slate-100 rounded-lg p-1 w-fit">
        <button
          onClick={() => setTab('compras')}
          className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${tab === 'compras' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
        >
          Compras
        </button>
        <button
          onClick={() => setTab('proveedores')}
          className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${tab === 'proveedores' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
        >
          Proveedores
        </button>
      </div>

      {status === 'loading' && suppliers.length === 0 && purchases.length === 0 && (
        <div className="flex items-center gap-2 text-slate-500 text-sm"><Loader2 size={16} className="animate-spin" /> Cargando datos...</div>
      )}

      {tab === 'compras' && (
        <>
          <div className="flex justify-between items-center">
            <div>
              <p className="text-sm text-slate-500">Registro de compras con Crédito Fiscal y factura PDF</p>
              <p className="text-xs text-slate-400 mt-0.5">{purchases.length} compras registradas</p>
            </div>
            <button
              onClick={() => setShowForm(true)}
              className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 text-sm"
            >
              <Plus size={16} /> Nueva Compra
            </button>
          </div>

          {purchases.length === 0 ? (
            <div className="bg-white rounded-xl border border-slate-200 p-6 text-center py-12">
              <Package size={48} className="mx-auto text-slate-300 mb-3" />
              <p className="text-slate-500">Las compras registradas impactan el inventario, generan movimientos Kardex y producen una factura en PDF.</p>
            </div>
          ) : (
            <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
                    <tr>
                      <th className="text-left px-4 py-3">Compra</th>
                      <th className="text-left px-4 py-3">Proveedor</th>
                      <th className="text-left px-4 py-3">Correo cliente</th>
                      <th className="text-right px-4 py-3">Total</th>
                      <th className="text-left px-4 py-3">Correo</th>
                      <th className="text-right px-4 py-3">PDF</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {purchases.map(purchase => (
                      <tr key={purchase.id} className="hover:bg-slate-50/60">
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2 font-medium text-slate-800">
                            <FileText size={15} className="text-slate-400" /> {purchase.numeroCompra}
                          </div>
                          <div className="text-xs text-slate-400">
                            {new Date(purchase.fecha).toLocaleString('es-SV')}
                          </div>
                        </td>
                        <td className="px-4 py-3 text-slate-700">{purchase.proveedorNombre}</td>
                        <td className="px-4 py-3 text-slate-600 text-xs">{purchase.customerEmail}</td>
                        <td className="px-4 py-3 text-right font-semibold text-slate-800">${money(purchase.total)}</td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${emailStyles[purchase.emailEstado] ?? emailStyles.PENDIENTE}`}>
                            <Mail size={12} /> {purchase.emailEstado}
                          </span>
                          {purchase.emailError && (
                            <div className="text-[11px] text-red-500 mt-1 max-w-[220px] truncate" title={purchase.emailError}>
                              {purchase.emailError}
                            </div>
                          )}
                        </td>
                        <td className="px-4 py-3 text-right">{renderPurchaseActions(purchase)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}

      {tab === 'proveedores' && (
        <>
          <div className="flex justify-between items-center">
            <p className="text-sm text-slate-500">{suppliers.length} proveedores registrados</p>
            <button
              onClick={() => setShowSupplierForm(true)}
              className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 text-sm"
            >
              <Plus size={16} /> Nuevo Proveedor
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {suppliers.map(supplier => (
              <div key={supplier.id} className="bg-white rounded-xl border border-slate-200 p-4">
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center shrink-0">
                    <Building2 size={20} className="text-blue-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-slate-800 text-sm">{supplier.nombre}</h3>
                    <p className="text-xs text-slate-500 mt-1">NIT: {supplier.nit}</p>
                    <p className="text-xs text-slate-500">NRC: {supplier.nrc}</p>
                    <p className="text-xs text-slate-500">{supplier.telefono}</p>
                    <p className="text-xs text-slate-500">{supplier.email}</p>
                    <p className="text-xs text-slate-400 mt-1">Contacto: {supplier.contacto}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Modal Nueva Compra */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold flex items-center gap-2">
                <Truck size={18} /> Registro de Compra (Crédito Fiscal)
              </h3>
              <button onClick={() => setShowForm(false)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>

            <form onSubmit={handleSubmitPurchase} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Proveedor *</label>
                <select
                  value={form.supplierId} onChange={e => setForm({ ...form, supplierId: e.target.value })} required
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                >
                  <option value="">Seleccionar proveedor...</option>
                  {suppliers.map(s => <option key={s.id} value={s.id}>{s.nombre} (NIT: {s.nit})</option>)}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Correo del cliente *</label>
                <input
                  type="email"
                  value={form.customerEmail}
                  onChange={e => setForm({ ...form, customerEmail: e.target.value })}
                  placeholder="cliente@empresa.com"
                  required
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                />
                <p className="text-xs text-slate-400 mt-1">Se adjuntará la factura PDF a este correo. Si SMTP no está configurado, queda disponible en la aplicación.</p>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-sm font-medium text-slate-700">Productos</label>
                  <button type="button" onClick={handleAddItem} className="text-emerald-600 text-xs flex items-center gap-1 hover:text-emerald-700">
                    <Plus size={12} /> Agregar
                  </button>
                </div>
                <div className="space-y-2">
                  {form.items.map((item, index) => (
                    <div key={index} className="flex gap-2 items-end">
                      <div className="flex-1">
                        <select
                          value={item.productId}
                          onChange={e => handleItemChange(index, 'productId', e.target.value)}
                          className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                        >
                          <option value="">Producto...</option>
                          {products.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                        </select>
                      </div>
                      <div className="w-20">
                        <input
                          type="number" min="1" value={item.cantidad}
                          onChange={e => handleItemChange(index, 'cantidad', e.target.value)}
                          placeholder="Cant."
                          className="w-full px-2 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                        />
                      </div>
                      <div className="w-24">
                        <input
                          type="number" step="0.01" min="0.01" value={item.precioUnitario}
                          onChange={e => handleItemChange(index, 'precioUnitario', e.target.value)}
                          placeholder="Precio"
                          className="w-full px-2 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                        />
                      </div>
                      {form.items.length > 1 && (
                        <button type="button" onClick={() => handleRemoveItem(index)} className="text-red-400 hover:text-red-600 p-2">
                          <X size={16} />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-slate-50 rounded-lg p-3 text-sm">
                <p className="text-slate-500">El stock se actualizará automáticamente al guardar la compra.</p>
                <p className="text-slate-500">Se registrará el IVA (13%) como crédito fiscal y se generará el PDF.</p>
              </div>

              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowForm(false)} className="flex-1 py-2.5 border border-slate-300 rounded-lg font-medium text-slate-700 hover:bg-slate-50">Cancelar</button>
                <button type="submit" disabled={status === 'loading'} className="flex-1 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 disabled:opacity-60">Registrar Compra</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Nuevo Proveedor */}
      {showSupplierForm && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Nuevo Proveedor</h3>
              <button onClick={() => setShowSupplierForm(false)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>
            <form onSubmit={handleSubmitSupplier} className="space-y-3">
              <input name="nombre" required placeholder="Nombre" className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none" />
              <div className="grid grid-cols-2 gap-3">
                <input name="nit" required placeholder="NIT" className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none" />
                <input name="nrc" required placeholder="NRC" className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <input name="telefono" required placeholder="Teléfono" className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none" />
                <input name="email" type="email" required placeholder="Email" className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none" />
              </div>
              <input name="direccion" required placeholder="Dirección" className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none" />
              <input name="contacto" required placeholder="Persona de contacto" className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none" />
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowSupplierForm(false)} className="flex-1 py-2.5 border border-slate-300 rounded-lg font-medium text-slate-700 hover:bg-slate-50">Cancelar</button>
                <button type="submit" disabled={status === 'loading'} className="flex-1 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 disabled:opacity-60">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
