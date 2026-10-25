import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../environments/environment';

export const AUTH_TOKEN_KEY = 'maka_token';

export function readToken(): string | null {
  try { return localStorage.getItem(AUTH_TOKEN_KEY); } catch { return null; }
}

export function saveToken(token: string): void {
  try { localStorage.setItem(AUTH_TOKEN_KEY, token); } catch { /* sem storage: sessão só até recarregar */ }
}

export function clearToken(): void {
  try { localStorage.removeItem(AUTH_TOKEN_KEY); } catch { /* ignora */ }
}

// Anexa "Authorization: Bearer <token>" em toda chamada para a nossa API
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = readToken();
  if (!token || !req.url.startsWith(environment.apiUrl)) return next(req);
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
