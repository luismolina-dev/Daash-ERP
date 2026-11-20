import { useEffect, useState } from 'react';
import { Outlet, Navigate, useLocation } from 'react-router-dom';
import { useStore } from '../stores/useStore';
import { canAccess, ROUTE_ROLES } from '../lib/roles';
import Sidebar from './Sidebar';
import Header from './Header';

const PAGE_TITLES: Record<string, string> = {
  '/pos': 'Punto de Venta',
  '/caja': 'Control de Caja',
  '/ventas': 'Ventas y Facturación DTE',
  '/inventario': 'Inventario y Kardex',
  '/compras': 'Compras y Proveedores',
  '/clientes': 'Clientes CRM',
  '/reportes': 'Dashboard y Reportes',
};

export default function Layout() {
  const { currentUser, setMessage } = useStore();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();
  const path = location.pathname;
  const denied = !!currentUser && !canAccess(path, currentUser.role);

  useEffect(() => {
    if (!denied) return;
    const allowed = ROUTE_ROLES[path]?.join(', ') ?? '';
    setMessage(`Su rol no tiene acceso a ${path}. Roles permitidos: ${allowed}.`);
  }, [denied, path, setMessage]);

  if (!currentUser) return <Navigate to="/login" replace />;

  // Redirección literal a /pos si el rol puede entrar; si no, a /login
  // (que a su vez envía al usuario a su página inicial permitida).
  if (denied) return <Navigate to={canAccess('/pos', currentUser.role) ? '/pos' : '/login'} replace />;

  const title = PAGE_TITLES[path] || 'Daash ERP';

  return (
    <div className="flex h-screen bg-slate-50">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="flex-1 flex flex-col overflow-hidden">
        <Header onMenuToggle={() => setSidebarOpen(!sidebarOpen)} title={title} />
        <main className="flex-1 overflow-y-auto p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
