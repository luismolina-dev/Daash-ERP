import type { UserRole } from '../types';

/** Matriz de acceso por ruta. Debe coincidir con Sidebar y con docs/API_CONTRACT.md. */
export const ROUTE_ROLES: Record<string, UserRole[]> = {
  '/pos': ['cajero', 'gerente'],
  '/caja': ['cajero', 'gerente', 'auditor'],
  '/ventas': ['cajero', 'gerente', 'auditor'],
  '/inventario': ['admin_bodega', 'gerente', 'auditor'],
  '/compras': ['admin_bodega', 'gerente'],
  '/clientes': ['cajero', 'gerente'],
  '/reportes': ['gerente', 'auditor'],
};

export const DEFAULT_ROLE_ROUTE: Record<UserRole, string> = {
  cajero: '/pos',
  admin_bodega: '/inventario',
  gerente: '/pos',
  auditor: '/reportes',
};

export const canAccess = (path: string, role: UserRole | undefined): boolean => {
  if (!role) return false;
  const allowed = ROUTE_ROLES[path];
  return allowed ? allowed.includes(role) : false;
};

export const landingRoute = (role: UserRole | undefined): string =>
  role ? DEFAULT_ROLE_ROUTE[role] : '/login';
