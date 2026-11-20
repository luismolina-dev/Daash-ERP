import { useEffect, useState } from 'react';
import { useStore } from '../stores/useStore';
import { formatCurrency, formatDateShort } from '../utils/helpers';
import {
  TrendingUp, AlertTriangle, Download, BarChart3, DollarSign,
  ShoppingCart, Loader2, Package,
} from 'lucide-react';
import type { IvaReportType } from '../types';

export default function ReportesPage() {
  const { reportSummary, status, error, loadReportSummary, loadIvaReport, setMessage } = useStore();
  const [exportType, setExportType] = useState<IvaReportType>('ventas_consumidor');

  useEffect(() => { void loadReportSummary() }, [loadReportSummary]);

  const exportCSV = async () => {
    const csv = await loadIvaReport(exportType);
    if (!csv) return;
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `libro_${exportType}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  const ventasPorDia = reportSummary?.ventasPorDia ?? [];
  const maxDia = Math.max(1, ...ventasPorDia.map(d => d.total));
  const topProducts = reportSummary?.topProducts ?? [];

  return (
    <div className="space-y-6">
      {error && (
        <div className="flex items-center gap-2 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
          <AlertTriangle size={16} /> {error}
        </div>
      )}
      {status === 'loading' && !reportSummary && (
        <div className="flex items-center gap-2 text-slate-500 text-sm"><Loader2 size={16} className="animate-spin" /> Cargando reportes...</div>
      )}

      {/* KPIs desde el backend */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl p-5 border border-slate-200">
          <div className="flex items-center justify-between mb-3">
            <div className="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center"><DollarSign size={20} className="text-emerald-600" /></div>
            <TrendingUp size={16} className="text-emerald-500" />
          </div>
          <p className="text-xs text-slate-500">Ingresos Totales</p>
          <p className="text-2xl font-bold text-slate-800">{formatCurrency(reportSummary?.totalIngresos ?? 0)}</p>
        </div>

        <div className="bg-white rounded-xl p-5 border border-slate-200">
          <div className="flex items-center justify-between mb-3">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center"><ShoppingCart size={20} className="text-blue-600" /></div>
          </div>
          <p className="text-xs text-slate-500">Total Ventas</p>
          <p className="text-2xl font-bold text-slate-800">{reportSummary?.totalVentas ?? 0}</p>
        </div>

        <div className="bg-white rounded-xl p-5 border border-slate-200">
          <div className="flex items-center justify-between mb-3">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center"><BarChart3 size={20} className="text-purple-600" /></div>
          </div>
          <p className="text-xs text-slate-500">IVA Recaudado</p>
          <p className="text-2xl font-bold text-slate-800">{formatCurrency(reportSummary?.totalIVA ?? 0)}</p>
        </div>

        <div className="bg-white rounded-xl p-5 border border-slate-200">
          <div className="flex items-center justify-between mb-3">
            <div className="w-10 h-10 bg-amber-100 rounded-lg flex items-center justify-center"><AlertTriangle size={20} className="text-amber-600" /></div>
          </div>
          <p className="text-xs text-slate-500">Stock Bajo</p>
          <p className="text-2xl font-bold text-amber-600">{reportSummary?.lowStockProductCount ?? 0}</p>
        </div>
      </div>

      {/* Productos más vendidos */}
      <div className="bg-white rounded-xl border border-slate-200 p-5">
        <h3 className="text-sm font-semibold text-slate-800 flex items-center gap-2 mb-4">
          <Package size={16} className="text-blue-600" /> Productos más vendidos
        </h3>
        {topProducts.length === 0 ? (
          <p className="text-sm text-slate-500">Sin datos de ventas.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200">
                  <th className="text-left py-2 px-3 text-slate-500 font-medium">Producto</th>
                  <th className="text-right py-2 px-3 text-slate-500 font-medium">Unidades</th>
                  <th className="text-right py-2 px-3 text-slate-500 font-medium">Monto</th>
                </tr>
              </thead>
              <tbody>
                {topProducts.map((p, idx) => (
                  <tr key={`${p.productoId}-${idx}`} className="border-b border-slate-100">
                    <td className="py-2 px-3 font-medium">{p.nombre}</td>
                    <td className="py-2 px-3 text-right">{p.cantidad}</td>
                    <td className="py-2 px-3 text-right font-semibold">{formatCurrency(p.monto)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Ventas por día: barras CSS simples */}
      <div className="bg-white rounded-xl border border-slate-200 p-5">
        <h3 className="text-sm font-semibold text-slate-800 flex items-center gap-2 mb-4">
          <BarChart3 size={16} className="text-purple-600" /> Ventas por día
        </h3>
        {ventasPorDia.length === 0 ? (
          <p className="text-sm text-slate-500">Sin datos para el gráfico.</p>
        ) : (
          <div className="flex items-end gap-3 h-56 border-b border-slate-200 pb-2 overflow-x-auto">
            {ventasPorDia.map(dia => (
              <div key={dia.fecha} className="flex flex-col items-center justify-end min-w-[64px] flex-1 gap-1 group">
                <span className="text-[10px] text-slate-600 font-medium hidden group-hover:block">{formatCurrency(dia.total)}</span>
                <div
                  className="w-10 bg-emerald-500 rounded-t transition-all hover:bg-emerald-600"
                  style={{ height: `${Math.max(4, (dia.total / maxDia) * 180)}px` }}
                  title={`${formatDateShort(dia.fecha)}: ${formatCurrency(dia.total)}`}
                />
                <span className="text-[10px] text-slate-500 whitespace-nowrap">{formatDateShort(dia.fecha).slice(5)}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Libros de IVA */}
      <div className="bg-white rounded-xl border border-slate-200 p-5">
        <h3 className="text-sm font-semibold text-slate-800 flex items-center gap-2 mb-4">
          <Download size={16} className="text-emerald-600" /> Libros de IVA / Exportar
        </h3>

        <div className="flex flex-wrap gap-3 items-end mb-4">
          <div className="flex-1 min-w-[250px]">
            <label className="block text-xs font-medium text-slate-700 mb-1">Tipo de Libro</label>
            <select
              value={exportType}
              onChange={e => setExportType(e.target.value as IvaReportType)}
              className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
            >
              <option value="ventas_consumidor">Ventas a Consumidor Final</option>
              <option value="ventas_contribuyente">Ventas a Contribuyentes</option>
              <option value="compras">Compras (Crédito Fiscal)</option>
            </select>
          </div>
          <button
            onClick={() => void exportCSV()}
            disabled={status === 'loading'}
            className="flex items-center gap-2 px-5 py-2.5 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 text-sm disabled:opacity-60"
          >
            <Download size={16} /> Exportar CSV
          </button>
          <button
            onClick={() => { void loadReportSummary(); setMessage('Reporte actualizado') }}
            className="px-4 py-2.5 border border-slate-300 rounded-lg text-sm text-slate-600 hover:bg-slate-50"
          >
            Actualizar KPIs
          </button>
        </div>

        <div className="bg-slate-50 rounded-lg p-4">
          <p className="text-xs text-slate-500">
            Los datos se obtienen del servidor ({exportType === 'compras' ? 'compras reales' : 'ventas registradas'}).
            El archivo CSV es generado por el backend.
          </p>
        </div>
      </div>
    </div>
  );
}
