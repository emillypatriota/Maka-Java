import { Component } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

// Só aceita voltar para caminho interno ("/checkout"), nunca para outro site
export function redirectSeguro(valor: string | null | undefined): string {
  return valor && valor.startsWith('/') && !valor.startsWith('//') ? valor : '/';
}

@Component({
  selector: 'app-client-login',
  standalone: true,
  imports: [FormsModule, NgIf, RouterLink],
  template: `
    <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;padding:2rem;background:#0a0a0c">
      <div style="width:100%;max-width:400px">
        <a routerLink="/" style="display:block;text-decoration:none;margin-bottom:2rem">
          <span style="font-family:var(--font-display);font-size:1.5rem;letter-spacing:.12em;color:#fff">MAKA</span>
        </a>

        <h1 style="font-family:var(--font-display);font-size:2rem;color:var(--text-primary);margin-bottom:.375rem">Entrar</h1>
        <p style="color:var(--text-tertiary);font-size:.875rem;margin-bottom:1.75rem">Entre para fazer e acompanhar seus pedidos.</p>

        <div *ngIf="expirada"
             style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:.75rem 1rem;font-size:.84rem;color:var(--danger-text);margin-bottom:1rem">
          Sua sessão expirou. Entre novamente.
        </div>

        <form (ngSubmit)="entrar()" style="display:flex;flex-direction:column;gap:1.125rem">
          <div class="form-group">
            <label class="form-label">E-mail</label>
            <input class="form-input" type="email" [(ngModel)]="email" name="email" required autocomplete="email" placeholder="voce@email.com">
          </div>
          <div class="form-group">
            <label class="form-label">Senha</label>
            <input class="form-input" type="password" [(ngModel)]="password" name="password" required autocomplete="current-password" placeholder="••••••••">
          </div>

          <div *ngIf="error"
               style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:.75rem 1rem;font-size:.84rem;color:var(--danger-text)">
            {{ error }}
          </div>

          <button type="submit" [disabled]="loading"
                  style="height:3rem;border-radius:var(--r-lg);background:var(--brand-600);color:#fff;font-size:.9375rem;font-weight:600;border:none;cursor:pointer"
                  [style.opacity]="loading ? '.7' : '1'">
            {{ loading ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>

        <p style="margin-top:1.5rem;text-align:center;font-size:.84rem;color:var(--text-tertiary)">
          Ainda não tem conta?
          <a routerLink="/cadastro" [queryParams]="{ redirect: redirect }" style="color:var(--brand-400);font-weight:600">Cadastre-se</a>
        </p>
        <p style="margin-top:.75rem;text-align:center;font-size:.8rem">
          <a routerLink="/portais" style="color:var(--text-tertiary)">Sou funcionário</a>
        </p>
      </div>
    </div>
  `,
})
export class ClientLoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;
  expirada: boolean;
  redirect: string;

  constructor(
    private auth: AuthService,
    private toast: ToastService,
    private router: Router,
    route: ActivatedRoute,
  ) {
    this.expirada = route.snapshot.queryParamMap.get('expirada') === '1';
    this.redirect = redirectSeguro(route.snapshot.queryParamMap.get('redirect'));
  }

  async entrar() {
    if (!this.email || !this.password) {
      this.error = 'Preencha todos os campos.';
      return;
    }
    this.error = '';
    this.loading = true;
    try {
      const user = await this.auth.loginWithEmail(this.email.trim(), this.password);
      if (user.role !== 'cliente') {
        await this.auth.logout();
        throw new Error('Esta é uma conta de funcionário. Use o portal de funcionários.');
      }
      this.toast.show('Bem-vindo, ' + user.username + '!');
      await this.router.navigateByUrl(this.redirect);
    } catch (err: any) {
      if (err?.status === 0) {
        this.error = 'Servidor indisponível. Tente novamente em instantes.';
      } else if (err?.status === 401) {
        this.error = 'E-mail ou senha incorretos.';
      } else {
        this.error = err?.error?.message ?? err?.message ?? 'Não foi possível entrar.';
      }
    } finally {
      this.loading = false;
    }
  }
}
