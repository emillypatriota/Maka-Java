import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { AuthService, AuthUser, ROLE_HOME, safeReturnUrl } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';
import { ToastService } from '../../../services/toast.service';

// Login do CLIENTE (rota /entrar). Funcionário também consegue entrar por aqui:
// quem não é cliente é levado para o painel do próprio perfil.
@Component({
  selector: 'app-client-login',
  standalone: true,
  imports: [FormsModule, NgIf, RouterLink],
  template: `
    <div style="padding:2.5rem 0 4rem;background:var(--stone-50);min-height:70vh">
      <div class="container" style="max-width:440px">

        <div style="margin-bottom:1.75rem;text-align:center">
          <h1 style="font-family:var(--font-serif);font-size:2.25rem;color:var(--stone-900);margin-bottom:.375rem">Entrar</h1>
          <p style="color:var(--stone-500);font-size:.875rem">
            {{ returnUrl ? 'Entre para continuar com o seu pedido' : 'Acesse a sua conta para pedir e acompanhar entregas' }}
          </p>
        </div>

        <div class="card">
          <div class="card-body">
            <form (ngSubmit)="login()" style="display:flex;flex-direction:column;gap:1.125rem">

              <div class="form-group">
                <label class="form-label" for="email">E-mail</label>
                <input class="form-input" id="email" name="email" type="email" [(ngModel)]="email"
                       placeholder="voce@email.com" autocomplete="email" required>
              </div>

              <div class="form-group">
                <label class="form-label" for="password">Senha</label>
                <div style="position:relative">
                  <input class="form-input" id="password" name="password" [type]="showPass ? 'text' : 'password'"
                         [(ngModel)]="password" placeholder="••••••••" autocomplete="current-password"
                         style="padding-right:4.5rem" required>
                  <button type="button" (click)="showPass = !showPass"
                          style="position:absolute;right:1rem;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;font-size:.72rem;font-weight:600;padding:0;color:var(--brand-600)">
                    {{ showPass ? 'OCULTAR' : 'MOSTRAR' }}
                  </button>
                </div>
              </div>

              <div *ngIf="error"
                   style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:.75rem 1rem;font-size:.84rem;color:var(--danger-text)">
                {{ error }}
              </div>

              <button type="submit" class="btn btn-primary btn-lg" [disabled]="loading"
                      style="width:100%;justify-content:center">
                {{ loading ? 'Entrando...' : 'Entrar' }}
              </button>
            </form>
          </div>
        </div>

        <p style="margin-top:1.5rem;text-align:center;font-size:.875rem;color:var(--stone-500)">
          Ainda não tem conta?
          <a routerLink="/cadastro" [queryParams]="returnUrl ? { returnUrl: returnUrl } : {}"
             style="color:var(--brand-600);font-weight:600">Cadastre-se</a>
        </p>
        <p style="margin-top:.5rem;text-align:center;font-size:.8rem">
          <a routerLink="/portais" style="color:var(--stone-400)">Sou funcionário</a>
        </p>
      </div>
    </div>
  `
})
export class ClientLoginComponent implements OnInit {
  email = '';
  password = '';
  error = '';
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
    // Já está logado: não precisa ver a tela de login
    await this.auth.ready;
    const user = this.auth.getUser();
    if (user) this.router.navigateByUrl(this.destino(user));
  }

  async login() {
    if (!this.email.trim() || !this.password) {
      this.error = 'Preencha o e-mail e a senha.';
      return;
    }
    this.error = '';
    this.loading = true;
    try {
      const user = await this.auth.loginWithEmail(this.email.trim(), this.password);
      this.toast.show(`Bem-vindo, ${user.username.split(' ')[0]}!`);
      await this.router.navigateByUrl(this.destino(user));
    } catch (err: any) {
      if (err?.status === 0) {
        this.error = 'Servidor indisponível. Tente novamente em instantes.';
      } else if (err?.status === 401) {
        this.error = 'E-mail ou senha incorretos.';
      } else {
        this.error = err?.error?.message ?? 'Não foi possível entrar.';
      }
    } finally {
      this.loading = false;
    }
  }

  // Cliente volta para onde estava (ex.: /checkout); sem returnUrl vai ao checkout se tiver
  // itens no carrinho. Funcionário vai para o painel dele.
  private destino(user: AuthUser): string {
    if (user.role !== 'client') return ROLE_HOME[user.role ?? ''] ?? '/';
    return this.returnUrl ?? (this.cart.getCount() > 0 ? '/checkout' : '/');
  }
}
