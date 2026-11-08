import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { AuthService, ROLE_HOME, safeReturnUrl } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';
import { ToastService } from '../../../services/toast.service';

// Cadastro do CLIENTE (rota /cadastro). Depois de cadastrar, a pessoa já fica logada.
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, NgIf, RouterLink],
  template: `
    <div style="padding:2.5rem 0 4rem;background:var(--stone-50);min-height:70vh">
      <div class="container" style="max-width:440px">

        <div style="margin-bottom:1.75rem;text-align:center">
          <h1 style="font-family:var(--font-serif);font-size:2.25rem;color:var(--stone-900);margin-bottom:.375rem">Criar conta</h1>
          <p style="color:var(--stone-500);font-size:.875rem">Leva menos de um minuto e você acompanha seus pedidos</p>
        </div>

        <div class="card">
          <div class="card-body">
            <form (ngSubmit)="cadastrar()" style="display:flex;flex-direction:column;gap:1.125rem">

              <div class="form-group">
                <label class="form-label" for="name">Nome</label>
                <input class="form-input" id="name" name="name" type="text" [(ngModel)]="name"
                       placeholder="Seu nome" autocomplete="name" maxlength="100" required>
              </div>

              <div class="form-group">
                <label class="form-label" for="email">E-mail</label>
                <input class="form-input" id="email" name="email" type="email" [(ngModel)]="email"
                       placeholder="voce@email.com" autocomplete="email" maxlength="120" required>
              </div>

              <div class="form-group">
                <label class="form-label" for="password">Senha</label>
                <div style="position:relative">
                  <input class="form-input" id="password" name="password" [type]="showPass ? 'text' : 'password'"
                         [(ngModel)]="password" placeholder="Mínimo 6 caracteres" autocomplete="new-password"
                         maxlength="72" style="padding-right:4.5rem" required>
                  <button type="button" (click)="showPass = !showPass"
                          style="position:absolute;right:1rem;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;font-size:.72rem;font-weight:600;padding:0;color:var(--brand-600)">
                    {{ showPass ? 'OCULTAR' : 'MOSTRAR' }}
                  </button>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label" for="confirm">Confirmar senha</label>
                <input class="form-input" id="confirm" name="confirm" [type]="showPass ? 'text' : 'password'"
                       [(ngModel)]="confirm" placeholder="Repita a senha" autocomplete="new-password"
                       maxlength="72" required>
              </div>

              <div *ngIf="error"
                   style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:.75rem 1rem;font-size:.84rem;color:var(--danger-text)">
                {{ error }}
                <a *ngIf="emailRepetido" routerLink="/entrar" [queryParams]="returnUrl ? { returnUrl: returnUrl } : {}"
                   style="font-weight:600;text-decoration:underline;margin-left:.25rem">Entrar</a>
              </div>

              <button type="submit" class="btn btn-primary btn-lg" [disabled]="loading"
                      style="width:100%;justify-content:center">
                {{ loading ? 'Criando conta...' : 'Criar conta' }}
              </button>
            </form>
          </div>
        </div>

        <p style="margin-top:1.5rem;text-align:center;font-size:.875rem;color:var(--stone-500)">
          Já tem conta?
          <a routerLink="/entrar" [queryParams]="returnUrl ? { returnUrl: returnUrl } : {}"
             style="color:var(--brand-600);font-weight:600">Entrar</a>
        </p>
      </div>
    </div>
  `
})
export class RegisterComponent implements OnInit {
  name = '';
  email = '';
  password = '';
  confirm = '';
  error = '';
  emailRepetido = false;
  loading = false;
  showPass = false;
  returnUrl: string | null;

  constructor(
    private auth: AuthService,
    private cart: CartService,
    private toast: ToastService,
    private router: Router,
    route: ActivatedRoute
  ) {
    this.returnUrl = safeReturnUrl(route.snapshot.queryParamMap.get('returnUrl'));
  }

  async ngOnInit() {
    await this.auth.ready;
    const user = this.auth.getUser();
    if (user) this.router.navigateByUrl(ROLE_HOME[user.role ?? ''] ?? '/');
  }

  private validar(): string {
    if (!this.name.trim())                                 return 'Informe o seu nome.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim())) return 'Informe um e-mail válido.';
    if (this.password.length < 6)                          return 'A senha deve ter pelo menos 6 caracteres.';
    if (this.password !== this.confirm)                    return 'As senhas não conferem.';
    return '';
  }

  async cadastrar() {
    this.emailRepetido = false;
    this.error = this.validar();
    if (this.error) return;

    this.loading = true;
    try {
      const user = await this.auth.register({
        name: this.name.trim(),
        email: this.email.trim(),
        password: this.password,
      });
      this.toast.show(`Conta criada! Bem-vindo, ${user.username.split(' ')[0]}.`);
      await this.router.navigateByUrl(this.returnUrl ?? (this.cart.getCount() > 0 ? '/checkout' : '/'));
    } catch (err: any) {
      if (err?.status === 0) {
        this.error = 'Servidor indisponível. Tente novamente em instantes.';
      } else if (err?.status === 409) {
        this.emailRepetido = true;
        this.error = 'Este e-mail já tem cadastro.';
      } else if (err?.status === 400 && err?.error?.campos) {
        // Primeira mensagem de campo que a API devolveu (nome, e-mail ou senha)
        this.error = (Object.values(err.error.campos)[0] as string) ?? 'Dados inválidos.';
      } else {
        this.error = err?.error?.message ?? 'Não foi possível criar a conta.';
      }
    } finally {
      this.loading = false;
    }
  }
}
