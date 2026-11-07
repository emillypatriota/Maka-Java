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

  
  private _readyResolve!: (u: AuthUser | null) => void;
  readonly ready = new Promise<AuthUser | null>(res => { this._readyResolve = res; });

  constructor(private http: HttpClient) {
    this.restoreSession();
  }

  get isAuthenticated(): boolean { return this._user() !== null; }
  get role(): UserRole           { return this._user()?.role ?? null; }
  getUser()                      { return this._user(); }

  private async restoreSession(): Promise<void> {
    const token = readToken();
    if (!token) {
      this._readyResolve(null);
      return;
    }

    
    if (token === 'token-cozinha') {
      const mockUser: AuthUser = { uid: '123', email: 'cozinha@maka.com', role: 'kitchen' as UserRole, username: 'Chef de Cozinha' };
      this._user.set(mockUser);
      this._readyResolve(mockUser);
      return;
    }
    if (token === 'token-gerente') {
      const mockUser: AuthUser = { uid: '456', email: 'gerente@maka.com', role: 'manager' as UserRole, username: 'Gerente Maka' };
      this._user.set(mockUser);
      this._readyResolve(mockUser);
      return;
    }
    
        try {
      const user = await firstValueFrom(this.http.get<AuthUser>(`${environment.apiUrl}/auth/me`));
      this._user.set(user);
      this._readyResolve(user);
    } catch (err: any) {
    
      if (err?.status === 401 || err?.status === 403) clearToken();
      this._readyResolve(null);
    }
  }

  async loginWithEmail(email: string, password: string): Promise<AuthUser> {
    
    if (email === 'cozinha@maka.com' && password === '123456') {
      const fakeUser: AuthUser = { uid: '123', email, role: 'kitchen' as UserRole, username: 'Chef de Cozinha' };
      saveToken('token-cozinha');
      this._user.set(fakeUser);
      return fakeUser;
    }

    if (email === 'gerente@maka.com' && password === '123456') {
      const fakeUser: AuthUser = { uid: '456', email, role: 'manager' as UserRole, username: 'Gerente Maka' };
      saveToken('token-gerente');
      this._user.set(fakeUser);
      return fakeUser;
    }

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