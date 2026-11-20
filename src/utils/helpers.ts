export const formatCurrency = (amount: number): string => {
  return `$${amount.toFixed(2)}`;
};

export const formatDate = (dateStr: string): string => {
  const d = new Date(dateStr);
  return d.toLocaleDateString('sv-SE', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  });
};

export const formatDateShort = (dateStr: string): string => {
  const d = new Date(dateStr);
  return d.toLocaleDateString('sv-SE');
};

export const validateDUI = (dui: string): boolean => {
  return /^\d{8}-\d$/.test(dui);
};

export const validateNIT = (nit: string): boolean => {
  return /^\d{4}-\d{6}-\d{3}-\d$/.test(nit);
};

export const generateId = () => Math.random().toString(36).slice(2, 11);

export const IVA_RATE = 0.13;

export const calculateSubtotal = (precio: number, cantidad: number, descuento: number) => {
  return precio * cantidad * (1 - descuento / 100);
};

export const calculateIVA = (subtotal: number) => subtotal * IVA_RATE;
