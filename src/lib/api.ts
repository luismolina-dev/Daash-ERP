import type { AuthResponse } from '../types';

export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

/** Access token en memoria únicamente: nunca se persiste en localStorage/sessionStorage. */
let accessToken: string | null = null;
let refreshPromise: Promise<boolean> | null = null;
let authFailureHandler: (() => void) | null = null;

/** Registra un callback para limpiar el estado local cuando el refresh falla. */
export const setAuthFailureHandler = (handler: () => void): void => { authFailureHandler = handler };

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly path?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export const getAccessToken = (): string | null => accessToken;
export const setAccessToken = (token: string | null): void => { accessToken = token };

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  /** Evita intentar refresh (p. ej. en /auth/login o /auth/refresh). */
  skipRefresh?: boolean;
  /** Devuelve la respuesta cruda como texto (para CSV). */
  rawText?: boolean;
  /** Devuelve la respuesta como Blob (para PDFs). */
  blob?: boolean;
}

async function refreshSession(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
          headers: { Accept: 'application/json' },
        });
        if (!response.ok) return false;
        const data = (await response.json()) as AuthResponse;
        setAccessToken(data.accessToken);
        return true;
      } catch {
        return false;
      } finally {
        // Se libera al terminar para permitir futuros refreshes.
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, skipRefresh = false, rawText = false, blob = false, headers, ...rest } = options;
  const url = `${API_BASE_URL}${path}`;

  const buildHeaders = (): HeadersInit => {
    const result = new Headers(headers);
    if (body !== undefined) result.set('Content-Type', 'application/json');
    result.set('Accept', blob ? 'application/pdf' : rawText ? 'text/csv' : 'application/json');
    if (accessToken) result.set('Authorization', `Bearer ${accessToken}`);
    return result;
  };

  const doRequest = () => fetch(url, {
    ...rest,
    credentials: 'include',
    headers: buildHeaders(),
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  let response = await doRequest();

  if (response.status === 401 && !skipRefresh) {
    if (await refreshSession()) {
      response = await doRequest();
    } else {
      setAccessToken(null);
      authFailureHandler?.();
      throw new ApiError(401, 'Sesión expirada. Inicie sesión nuevamente.', path);
    }
  }

  if (!response.ok) {
    let message = `Error ${response.status}`;
    try {
      const errorBody = (await response.json()) as { message?: string; error?: string };
      message = errorBody.message || errorBody.error || message;
    } catch {
      message = response.status === 401 ? 'Credenciales inválidas' : message;
    }
    throw new ApiError(response.status, message, path);
  }

  if (blob) return (await response.blob()) as T;
  if (rawText) return (await response.text()) as T;
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'POST', body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'PATCH', body }),
};
