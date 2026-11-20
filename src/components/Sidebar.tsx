import { NavLink, useNavigate } from 'react-router-dom';
import { useStore } from '../stores/useStore';
import { ROLE_LABELS } from '../mockData';
import { ROUTE_ROLES } from '../lib/roles';
import {
  ShoppingCart, Package, Truck, Users, BarChart3,
  Receipt, LogOut, X, ClipboardList,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

interface NavItem {
  to: string;
  icon: LucideIcon;
  label: string;
}

const navItems: NavItem[] = [
  { to: '/pos', icon: ShoppingCart, label: 'Punto de Venta' },
  { to: '/caja', icon: ClipboardList, label: 'Control de Caja' },
  { to: '/ventas', icon: Receipt, label: 'Ventas / DTE' },
  { to: '/inventario', icon: Package, label: 'Inventario' },
  { to: '/compras', icon: Truck, label: 'Compras' },
  { to: '/clientes', icon: Users, label: 'Clientes' },
  { to: '/reportes', icon: BarChart3, label: 'Reportes' },
];

export default function Sidebar({ open, onClose }: SidebarProps) {
  const { currentUser, logout } = useStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const filteredNav = navItems.filter(item =>
    currentUser && ROUTE_ROLES[item.to]?.includes(currentUser.role)
  );

  return (
    <>
      {open && <div className="fixed inset-0 bg-black/50 z-40 lg:hidden" onClick={onClose} />}

      <aside className={`fixed lg:static inset-y-0 left-0 z-50 w-64 bg-slate-900 text-white flex flex-col transform transition-transform duration-200 ${open ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
        <div className="flex items-center justify-between px-4 py-4 border-b border-slate-700">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-emerald-500 rounded-lg flex items-center justify-center font-bold text-sm">DA</div>
            <div>
              <h1 className="font-bold text-sm leading-tight">Daash ERP</h1>
              <p className="text-xs text-slate-400">Adventure Works SV</p>
            </div>
          </div>
          <button onClick={onClose} className="lg:hidden text-slate-400 hover:text-white">
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {filteredNav.map(item => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${isActive ? 'bg-emerald-600 text-white' : 'text-slate-300 hover:bg-slate-800 hover:text-white'}`
              }
            >
              <item.icon size={18} />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="px-3 py-4 border-t border-slate-700">
          <div className="px-3 mb-3">
            <p className="text-sm font-medium text-white">{currentUser?.nombre}</p>
            <p className="text-xs text-slate-400">{currentUser && ROLE_LABELS[currentUser.role]}</p>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-slate-300 hover:bg-red-600/20 hover:text-red-400 transition-colors"
          >
            <LogOut size={18} />
            Cerrar Sesión
          </button>
        </div>
      </aside>
    </>
  );
}
