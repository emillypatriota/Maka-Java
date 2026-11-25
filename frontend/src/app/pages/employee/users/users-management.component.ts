import { Component, OnInit } from '@angular/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  InternalRole, InternalUser, ROLE_LABEL, UsuarioService
} from '../../../services/usuario.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { apiErrorMessage } from '../../../core/http-error';

interface UserForm {
  username: string;
  email: string;
  password: string;
  role: InternalRole;
}

// RF-041: só o ADMIN entra aqui (rota com roleGuard('admin') e API com hasRole('ADMIN'))
@Component({
  selector: 'app-users-management',
  standalone: true,
  imports: [DatePipe, FormsModule, NgFor, NgIf],
  template: `
    <div style="max-width:1100px;margin:0 auto;padding:0 1.5rem 2rem">
      <header style="display:flex;align-items:flex-start;justify-content:space-between;gap:1rem;flex-wrap:wrap;margin-bottom:2rem"
              class="animate-fade-up">
        <div>
          <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">
            Usuários
          </h1>
          <p style="color:var(--text-tertiary);font-size:var(--text-base)">
            Crie os logins de gerentes, cozinheiros e entregadores. Clientes se cadastram sozinhos no site.
          </p>
        </div>
        <button class="btn btn-primary" type="button" (click)="abrirNovo()">Novo usuário</button>
      </header>

      <div *ngIf="erro" class="card" role="alert"
           style="padding:1rem;margin-bottom:1rem;color:var(--danger-text);border-color:var(--danger-border)">
        {{ erro }}
        <button *ngIf="!carregando" class="btn btn-ghost btn-sm" type="button" (click)="carregar(pagina)">Tentar novamente</button>
      </div>

      <section class="card">
        <div *ngIf="carregando" role="status" style="padding:1.5rem;color:var(--text-secondary)">
          Carregando usuários...
        </div>

        <div *ngIf="!carregando && usuarios.length" style="overflow-x:auto">
          <table style="width:100%;border-collapse:collapse;text-align:left">
            <thead>
              <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                <th style="padding:1rem">Nome</th>
                <th style="padding:1rem">E-mail</th>
                <th style="padding:1rem">Perfil</th>
                <th style="padding:1rem">Status</th>
                <th style="padding:1rem">Criado em</th>
                <th style="padding:1rem;text-align:right">Ações</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let u of usuarios" style="border-top:1px solid var(--border);color:var(--text-primary)"
                  [style.opacity]="u.status === 'INATIVO' ? .55 : 1">
                <td style="padding:1rem;font-weight:600">
                  {{ u.username }}
                  <span *ngIf="u.email === emailLogado" style="color:var(--text-tertiary);font-weight:400"> (você)</span>
                </td>
                <td style="padding:1rem;color:var(--text-secondary)">{{ u.email }}</td>
                <td style="padding:1rem;color:var(--text-secondary)">{{ roleLabel(u.role) }}</td>
                <td style="padding:1rem">
                  <span class="badge" [class.badge-green]="u.status === 'ATIVO'" [class.badge-red]="u.status === 'INATIVO'">
                    {{ u.status === 'ATIVO' ? 'Ativo' : 'Inativo' }}
                  </span>
                </td>
                <td style="padding:1rem;color:var(--text-secondary)">{{ u.createdAt ? (u.createdAt | date:'dd/MM/yyyy') : '—' }}</td>
                <td style="padding:1rem;text-align:right;white-space:nowrap">
                  <ng-container *ngIf="u.role !== 'admin'; else contaAdmin">
                    <button class="btn btn-ghost btn-sm" type="button" (click)="abrirEdicao(u)">Editar</button>
                    <button *ngIf="u.status === 'ATIVO' && u.email !== emailLogado" class="btn btn-ghost btn-sm" type="button"
                            [disabled]="alterandoId === u.id" (click)="alterarStatus(u, 'INATIVO')">
                      {{ alterandoId === u.id ? 'Desativando...' : 'Desativar' }}
                    </button>
                    <button *ngIf="u.status === 'INATIVO'" class="btn btn-ghost btn-sm" type="button"
                            [disabled]="alterandoId === u.id" (click)="alterarStatus(u, 'ATIVO')">
                      {{ alterandoId === u.id ? 'Reativando...' : 'Reativar' }}
                    </button>
                  </ng-container>
                  <ng-template #contaAdmin>
                    <span style="color:var(--text-tertiary);font-size:var(--text-sm)">Conta de administrador</span>
                  </ng-template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div *ngIf="!carregando && !usuarios.length && !erro" class="empty-state" style="padding:2.5rem 1rem">
          <h2>Nenhum usuário interno</h2>
          <p>Crie o primeiro login da equipe.</p>
        </div>

        <footer *ngIf="!carregando && totalElements > 0"
                style="display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap;padding:1rem;border-top:1px solid var(--border)">
          <span style="font-size:var(--text-sm);color:var(--text-tertiary)">
            {{ totalElements }} usuário(s) · Página {{ pagina + 1 }} de {{ totalPages }}
          </span>
          <div style="display:flex;gap:.5rem">
            <button class="btn btn-ghost btn-sm" type="button" [disabled]="pagina === 0" (click)="carregar(pagina - 1)">Anterior</button>
            <button class="btn btn-ghost btn-sm" type="button" [disabled]="pagina + 1 >= totalPages" (click)="carregar(pagina + 1)">Próxima</button>
          </div>
        </footer>
      </section>
    </div>

    <div *ngIf="modalAberto" class="modal-overlay" (click)="fecharModal()">
      <section class="modal" style="max-width:480px" role="dialog" aria-modal="true"
               aria-labelledby="user-modal-title" (click)="$event.stopPropagation()">
        <header class="modal-header">
          <h2 id="user-modal-title">{{ editandoId === null ? 'Novo usuário' : 'Editar usuário' }}</h2>
          <button class="btn btn-ghost btn-icon" type="button" aria-label="Fechar" (click)="fecharModal()">×</button>
        </header>
        <div class="modal-body" style="display:flex;flex-direction:column;gap:1rem">
          <label class="form-group">
            <span class="form-label">Nome *</span>
            <input class="form-input" [(ngModel)]="form.username" maxlength="100" required>
          </label>
          <label class="form-group">
            <span class="form-label">E-mail (login) *</span>
            <input class="form-input" type="email" [(ngModel)]="form.email" maxlength="120"
                   [disabled]="editandoId !== null" required>
            <small *ngIf="editandoId !== null" style="color:var(--text-tertiary)">O e-mail é o login e não muda.</small>
          </label>
          <label class="form-group">
            <span class="form-label">Perfil *</span>
            <select class="form-input" [(ngModel)]="form.role">
              <option value="manager">Gerente</option>
              <option value="kitchen">Cozinheiro</option>
              <option value="delivery">Entregador</option>
            </select>
          </label>
          <label class="form-group">
            <span class="form-label">{{ editandoId === null ? 'Senha *' : 'Nova senha' }}</span>
            <input class="form-input" type="password" [(ngModel)]="form.password" maxlength="72" autocomplete="new-password"
                   [placeholder]="editandoId === null ? 'Mínimo de 6 caracteres' : 'Deixe em branco para manter a atual'">
          </label>
          <p *ngIf="erroFormulario" role="alert" style="color:var(--danger-text)">{{ erroFormulario }}</p>
        </div>
        <footer class="modal-footer">
          <button class="btn btn-ghost" type="button" (click)="fecharModal()">Cancelar</button>
          <button class="btn btn-primary" type="button" [disabled]="salvando" (click)="salvar()">
            {{ salvando ? 'Salvando...' : 'Salvar' }}
          </button>
        </footer>
      </section>
    </div>
  `
})
export class UsersManagementComponent implements OnInit {
  usuarios: InternalUser[] = [];
  pagina = 0;
  totalPages = 0;
  totalElements = 0;
  carregando = false;
  salvando = false;
  modalAberto = false;
  erro = '';
  erroFormulario = '';
  editandoId: string | null = null;
  alterandoId: string | null = null;
  emailLogado = '';
  form: UserForm = this.novoFormulario();

  constructor(
    private service: UsuarioService,
    private auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.emailLogado = this.auth.getUser()?.email ?? '';
    this.carregar(0);
  }

  roleLabel(role: string): string {
    return ROLE_LABEL[role] ?? role;
  }

  carregar(pagina: number): void {
    this.carregando = true;
    this.erro = '';
    this.service.listar(pagina).subscribe({
      next: resultado => {
        this.usuarios = resultado.content;
        this.pagina = resultado.number;
        this.totalPages = resultado.totalPages;
        this.totalElements = resultado.totalElements;
        this.carregando = false;
      },
      error: error => {
        this.erro = apiErrorMessage(error, 'Não foi possível carregar os usuários.');
        this.carregando = false;
      }
    });
  }

  abrirNovo(): void {
    this.editandoId = null;
    this.form = this.novoFormulario();
    this.erroFormulario = '';
    this.modalAberto = true;
  }

  abrirEdicao(u: InternalUser): void {
    if (u.role === 'admin') return;
    this.editandoId = u.id;
    this.form = { username: u.username, email: u.email, password: '', role: u.role };
    this.erroFormulario = '';
    this.modalAberto = true;
  }

  fecharModal(): void {
    if (this.salvando) return;
    this.modalAberto = false;
    this.erroFormulario = '';
  }

  salvar(): void {
    const username = this.form.username.trim();
    const email = this.form.email.trim().toLowerCase();
    const password = this.form.password;
    const novo = this.editandoId === null;

    if (!username) { this.erroFormulario = 'Informe o nome.'; return; }
    if (novo && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { this.erroFormulario = 'Informe um e-mail válido.'; return; }
    if ((novo || password) && password.length < 6) {
      this.erroFormulario = 'A senha deve ter pelo menos 6 caracteres.';
      return;
    }

    this.salvando = true;
    this.erroFormulario = '';
    const request = novo
      ? this.service.criar({ username, email, password, role: this.form.role })
      : this.service.atualizar(this.editandoId!, { username, role: this.form.role, password: password || undefined });

    request.subscribe({
      next: () => {
        this.salvando = false;
        this.modalAberto = false;
        this.toast.show(novo ? 'Usuário criado.' : 'Usuário atualizado.');
        this.carregar(novo ? 0 : this.pagina);
      },
      error: error => {
        this.salvando = false;
        this.erroFormulario = apiErrorMessage(error, 'Não foi possível salvar o usuário.');
      }
    });
  }

  alterarStatus(u: InternalUser, status: 'ATIVO' | 'INATIVO'): void {
    if (status === 'INATIVO' && !window.confirm(`Desativar "${u.username}"? Ele não vai conseguir mais entrar no sistema.`)) return;
    this.alterandoId = u.id;
    this.service.alterarStatus(u.id, status).subscribe({
      next: () => {
        this.alterandoId = null;
        this.toast.show(status === 'INATIVO' ? 'Usuário desativado.' : 'Usuário reativado.');
        this.carregar(this.pagina);
      },
      error: error => {
        this.alterandoId = null;
        this.toast.show(apiErrorMessage(error, 'Não foi possível alterar o status.'), 'error');
      }
    });
  }

  private novoFormulario(): UserForm {
    return { username: '', email: '', password: '', role: 'kitchen' };
  }
}
