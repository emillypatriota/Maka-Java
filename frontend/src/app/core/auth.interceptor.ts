import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Subject, catchError, throwError } from 'rxjs';
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

// Avisa o AuthService quando a API responde 401 para um token que a gente mandou
// (token vencido ou inválido). O AuthService limpa a sessão e leva para o login.
// É um Subject (e não inject(AuthService)) para o interceptor não depender do AuthService,
// que por sua vez depende do HttpClient: evita dependência circular.
export const sessionExpired$ = new Subject<void>();

// Nestas rotas o 401 é "senha errada", não "sessão vencida"
const ROTAS_DE_LOGIN = ['/auth/login', '/auth/register'];

// Anexa "Authorization: Bearer <token>" em toda chamada para a nossa API
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = readToken();
  if (!token || !req.url.startsWith(environment.apiUrl)) return next(req);

  const autenticada = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  return next(autenticada).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && !ROTAS_DE_LOGIN.some(r => req.url.endsWith(r))) {
        clearToken();
        sessionExpired$.next();
      }
      return throwError(() => err);
    })
  );
};
