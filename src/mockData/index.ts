import type { UserRole } from '../types';

/** Etiquetas de visualización para los roles del backend. */
export const ROLE_LABELS: Record<UserRole, string> = {
  cajero: 'Cajero',
  admin_bodega: 'Admin. Bodega',
  gerente: 'Gerente',
  auditor: 'Auditor',
};

/** Catálogo de demostración para sugerencias de categorías. Sin credenciales. */
export const DEMO_CATEGORIES = [
  'Bicicletas', 'Accesorios', 'Componentes', 'Herramientas',
  'Seguridad', 'Repuestos', 'Vestimenta', 'Otros',
] as const;

/** Correos de demostración: sólo se usa para rellenar el email en el login rápido. */
export const DEMO_LOGIN_EMAILS = [
  { email: 'maria@adventureworks.com', role: 'cajero' as UserRole, label: 'Cajero' },
  { email: 'carlos@adventureworks.com', role: 'admin_bodega' as UserRole, label: 'Admin. Bodega' },
  { email: 'ana@adventureworks.com', role: 'gerente' as UserRole, label: 'Gerente' },
  { email: 'roberto@adventureworks.com', role: 'auditor' as UserRole, label: 'Auditor' },
];
