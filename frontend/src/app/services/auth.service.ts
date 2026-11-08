import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserRole } from '../models';
import { clearToken, readToken, saveToken, sessionExpired$ } from '../core/auth.interceptor';
import { ToastService } from './toast.service';

export interface AuthUser {
  uid: string;
  email: string;
  role: UserRole;
  username: string;
}

export interface RegisterData {
  name: string;
  email: string;
  password: string;
}

interface LoginResponse {
  token: string;
  user: AuthUser;
}

// Para onde cada perfil vai depois de entrar
export const ROLE_HOME: Record<string, string> = {
  admin:    '/gerente/insights',
  manager:  '/gerente/insights',
  kitchen:  '/cozinha/painel',
  delivery: '/entregador/painel',
  client:   '/',
};

// Tela de login de cada perfil (cliente tem login próprio; o ADMIN entra pelo portal do gerente)
export const ROLE_LOGIN: Record<string, string> = {
  admin:    '/login/manager',
  manager:  '/login/manager',
  kitchen:  '/login/kitchen',
  delivery: '/login/delivery',
  client:   '/entrar',
};

// Tela de login certa para a área em que a pessoa estava (usado quando a sessão vence)
export function loginRouteForUrl(url: string): string {
  if (url.startsWith('/gerente'))    return ROLE_LOGIN['manager'];
  if (url.startsWith('/cozinha'))    return ROLE_LOGIN['kitchen'];
  if (url.startsWith('/entregador')) return ROLE_LOGIN['delivery'];
  return ROLE_LOGIN['client'];
}

// returnUrl vem da barra de endereço: só aceita caminho interno (evita open redirect)
export function safeReturnUrl(raw: string | null | undefined): string | null {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//')) return null;
  if (raw.startsWith('/entrar') || raw.startsWith('/cadastro') || raw.startsWith('/login')) return null;
  return raw;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _user = signal<AuthUser | null>(null);
  readonly user = this._user.asReadonly();

  // Resolve com o usuário atual (ou null). Começa como "restaurar a sessão do token salvo" e é
  // trocada a cada login/cadastro/logout, então guards e layouts que fazem `await auth.ready`
  // sempre enxergam o estado de agora (dá para navegar sem recarregar a página depois de logar).
  // Sem token salvo resolve null na hora, sem chamar a API.
  private _ready: Promise<AuthUser | null>;
  private booted = false;

  constructor(
    private http: HttpClient,
    private router: Router,
    private toast: ToastService
  ) {
    sessionExpired$.subscribe(() => this.onSessionExpired());
    this._ready = this.restoreSession();
  }

  get ready(): Promise<AuthUser | null> { return this._ready; }

  get isAuthenticated(): boolean { return this._user() !== null; }
  get role(): UserRole           { return this._user()?.role ?? null; }
  getUser()                      { return this._user(); }

  hasRole(...roles: UserRole[]): boolean {
    const role = this._user()?.role;
    return !!role && roles.includes(role);
  }

  homeRoute(role: UserRole = this.role): string {
    return ROLE_HOME[role ?? ''] ?? '/';
  }

  private async restoreSession(): Promise<AuthUser | null> {
    try {
      if (!readToken()) return null;
      try {
        const user = await firstValueFrom(this.http.get<AuthUser>(`${environment.apiUrl}/auth/me`));
        this._user.set(user);
        return user;
      } catch (err: any) {
        // Token vencido/inválido: limpa. Se for só o backend fora do ar, mantém o token.
        if (err?.status === 401 || err?.status === 403) clearToken();
        return null;
      }
    } finally {
      this.booted = true;
    }
  }

  async loginWithEmail(email: string, password: string): Promise<AuthUser> {
    const res = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, password })
    );
    return this.startSession(res);
  }

  // Cadastro de cliente: a API já devolve o token, então a pessoa fica logada
  async register(data: RegisterData): Promise<AuthUser> {
    const res = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiUrl}/auth/register`, data)
    );
    return this.startSession(res);
  }

  async logout(): Promise<void> {
    clearToken();
    this._user.set(null);
    this._ready = Promise.resolve(null);
  }

  private startSession(res: LoginResponse): AuthUser {
    saveToken(res.token);
    this._user.set(res.user);
    this._ready = Promise.resolve(res.user);
    return res.user;
  }

  // A API respondeu 401 a um token nosso: encerra a sessão e leva para o login da área atual
  private onSessionExpired(): void {
    if (!this.booted || !this._user()) return;   // ainda subindo, ou já saiu: nada a fazer
    const url = this.router.url;
    this._user.set(null);
    this._ready = Promise.resolve(null);
    this.toast.show('Sua sessão expirou. Entre novamente.', 'error');
    const login = loginRouteForUrl(url);
    this.router.navigate([login], login === ROLE_LOGIN['client'] ? { queryParams: { returnUrl: url } } : {});
  }
}
