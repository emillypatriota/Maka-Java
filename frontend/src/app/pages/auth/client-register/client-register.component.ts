import { Component } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { redirectSeguro } from '../client-login/client-login.component';

@Component({
  selector: 'app-client-register',
  standalone: true,
  imports: [FormsModule, NgIf, RouterLink],
  template: `
    <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;padding:2rem;background:#0a0a0c">
      <div style="width:100%;max-width:400px">
        <a routerLink="/" style="display:block;text-decoration:none;margin-bottom:2rem">
          <span style="font-family:var(--font-display);font-size:1.5rem;letter-spacing:.12em;color:#fff">MAKA</span>
        </a>

        <h1 style="font-family:var(--font-display);font-size:2rem;color:var(--text-primary);margin-bottom:.375rem">Criar conta</h1>
        <p style="color:var(--text-tertiary);font-size:.875rem;margin-bottom:1.75rem">Leva menos de um minuto.</p>

        <form (ngSubmit)="cadastrar()" style="display:flex;flex-direction:column;gap:1.125rem">
          <div class="form-group">
            <label class="form-label">Nome</label>
            <input class="form-input" type="text" [(ngModel)]="username" name="username" required maxlength="100" autocomplete="name" placeholder="Seu nome">
          </div>
          <div class="form-group">
            <label class="form-label">E-mail</label>
            <input class="form-input" type="email" [(ngModel)]="email" name="email" required autocomplete="email" placeholder="voce@email.com">
          </div>
          <div class="form-group">
            <label class="form-label">Senha</label>
            <input class="form-input" type="password" [(ngModel)]="password" name="password" required minlength="6" autocomplete="new-password" placeholder="Mínimo de 6 caracteres">
          </div>
          <div class="form-group">
            <label class="form-label">Confirmar senha</label>
            <input class="form-input" type="password" [(ngModel)]="confirm" name="confirm" required autocomplete="new-password" placeholder="Repita a senha">
          </div>

          <div *ngIf="error"
               style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:.75rem 1rem;font-size:.84rem;color:var(--danger-text)">
            {{ error }}
          </div>

          <button type="submit" [disabled]="loading"
                  style="height:3rem;border-radius:var(--r-lg);background:var(--brand-600);color:#fff;font-size:.9375rem;font-weight:600;border:none;cursor:pointer"
                  [style.opacity]="loading ? '.7' : '1'">
            {{ loading ? 'Criando conta...' : 'Criar conta' }}
          </button>
        </form>

        <p style="margin-top:1.5rem;text-align:center;font-size:.84rem;color:var(--text-tertiary)">
          Já tem conta?
          <a routerLink="/entrar" [queryParams]="{ redirect: redirect }" style="color:var(--brand-400);font-weight:600">Entrar</a>
        </p>
      </div>
    </div>
  `,
})
export class ClientRegisterComponent {
  username = '';
  email = '';
  password = '';
  confirm = '';
  error = '';
  loading = false;
  redirect: string;

  constructor(
    private auth: AuthService,
    private toast: ToastService,
    private router: Router,
    route: ActivatedRoute,
  ) {
    this.redirect = redirectSeguro(route.snapshot.queryParamMap.get('redirect'));
  }

  async cadastrar() {
    if (!this.username.trim() || !this.email || !this.password) {
      this.error = 'Preencha todos os campos.';
      return;
    }
    if (this.password.length < 6) {
      this.error = 'A senha deve ter pelo menos 6 caracteres.';
      return;
    }
    if (this.password !== this.confirm) {
      this.error = 'As senhas não conferem.';
      return;
    }
    this.error = '';
    this.loading = true;
    try {
      const user = await this.auth.register(this.username.trim(), this.email.trim(), this.password);
      this.toast.show('Conta criada! Bem-vindo, ' + user.username + '.');
      await this.router.navigateByUrl(this.redirect);
    } catch (err: any) {
      if (err?.status === 0) {
        this.error = 'Servidor indisponível. Tente novamente em instantes.';
      } else if (err?.status === 409) {
        this.error = 'Já existe uma conta com este e-mail.';
      } else if (err?.status === 400 && err?.error?.campos) {
        this.error = Object.values(err.error.campos).join(' ');
      } else {
        this.error = err?.error?.message ?? 'Não foi possível criar a conta.';
      }
    } finally {
      this.loading = false;
    }
  }
}
