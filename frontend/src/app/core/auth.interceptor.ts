import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
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

// Para qual login mandar quando a sessão expira, conforme a área em que a pessoa estava
function loginDaAreaAtual(): string {
  const path = window.location.pathname;
  if (path.startsWith('/gerente'))    return '/login/manager';
  if (path.startsWith('/cozinha'))    return '/login/kitchen';
  if (path.startsWith('/entregador')) return '/login/delivery';
  return `/entrar?expirada=1&redirect=${encodeURIComponent(path)}`;
}

// Anexa "Authorization: Bearer <token>" em toda chamada para a nossa API e,
// se a API responder 401 com token (sessão expirada), limpa o token e manda para o login.
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = readToken();
  if (!token || !req.url.startsWith(environment.apiUrl)) return next(req);

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
    catchError((err: HttpErrorResponse) => {
      // /auth/* fica de fora: o /auth/me do início já trata o 401 sozinho (restoreSession)
      const ehAuth = req.url.startsWith(`${environment.apiUrl}/auth/`);
      if (err.status === 401 && !ehAuth) {
        clearToken();
        window.location.assign(loginDaAreaAtual());
      }
      return throwError(() => err);
    })
  );
};
