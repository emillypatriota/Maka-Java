import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserRole } from '../models';
import { clearToken, readToken, saveToken } from '../core/auth.interceptor';

export interface AuthUser {
  uid: string;
  email: string;
  role: UserRole;
  username: string;
}

interface LoginResponse {
  token: string;
  user: AuthUser;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _user = signal<AuthUser | null>(null);
  readonly user = this._user.asReadonly();

  // Promise que resolve UMA vez só: com o usuário (se há token válido) ou null.
  // Sem token salvo resolve null na hora, sem chamar a API.
  private _readyResolve!: (u: AuthUser | null) => void;
  readonly ready = new Promise<AuthUser | null>(res => { this._readyResolve = res; });

  constructor(private http: HttpClient) {
    this.restoreSession();
  }

  get isAuthenticated(): boolean { return this._user() !== null; }
  get role(): UserRole           { return this._user()?.role ?? null; }
  getUser()                      { return this._user(); }

  private async restoreSession(): Promise<void> {
    if (!readToken()) {
      this._readyResolve(null);
      return;
    }
    try {
      const user = await firstValueFrom(this.http.get<AuthUser>(`${environment.apiUrl}/auth/me`));
      this._user.set(user);
      this._readyResolve(user);
    } catch (err: any) {
      // Token vencido/inválido: limpa. Se for só o backend fora do ar, mantém o token.
      if (err?.status === 401 || err?.status === 403) clearToken();
      this._readyResolve(null);
    }
  }

  async loginWithEmail(email: string, password: string): Promise<AuthUser> {
    const res = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, password })
    );
    saveToken(res.token);
    this._user.set(res.user);
    return res.user;
  }

  async logout(): Promise<void> {
    clearToken();
    this._user.set(null);
  }
}
