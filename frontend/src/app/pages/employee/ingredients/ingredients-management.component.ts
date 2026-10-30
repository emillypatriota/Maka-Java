import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Ingrediente, IngredienteInput, IngredienteService } from '../../../services/ingrediente.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-ingredients-management',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, NgFor, NgIf],
  template: `
    <div style="max-width:1100px;margin:0 auto;padding:0 1.5rem 2rem">
      <header style="display:flex;align-items:flex-start;justify-content:space-between;gap:1rem;flex-wrap:wrap;margin-bottom:2rem"
              class="animate-fade-up">
        <div>
          <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">
            Ingredientes
          </h1>
          <p style="color:var(--text-tertiary);font-size:var(--text-base)">
            Cadastre ingredientes e custos para as fichas técnicas.
          </p>
        </div>
        <button class="btn btn-primary" type="button" (click)="abrirNovo()">Novo ingrediente</button>
      </header>

      <div *ngIf="erro" class="card" role="alert"
           style="padding:1rem;margin-bottom:1rem;color:var(--danger-text);border-color:var(--danger-border)">
        {{ erro }}
        <button *ngIf="!carregando" class="btn btn-ghost btn-sm" type="button" (click)="carregar(pagina)">Tentar novamente</button>
      </div>

      <section class="card">
        <div *ngIf="carregando" role="status" style="padding:1.5rem;color:var(--text-secondary)">
          Carregando ingredientes...
        </div>

        <div *ngIf="!carregando && ingredientes.length" style="overflow-x:auto">
          <table style="width:100%;border-collapse:collapse;text-align:left">
            <thead>
              <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                <th style="padding:1rem">Nome</th>
                <th style="padding:1rem">Unidade</th>
                <th style="padding:1rem">Custo unitário</th>
                <th style="padding:1rem;text-align:right">Ações</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let ingrediente of ingredientes"
                  style="border-top:1px solid var(--border);color:var(--text-primary)">
                <td style="padding:1rem;font-weight:600">{{ ingrediente.nome }}</td>
                <td style="padding:1rem;color:var(--text-secondary)">{{ ingrediente.unidadeMedida }}</td>
                <td style="padding:1rem;color:var(--text-secondary)">
                  {{ ingrediente.custoUnitario | currency:'BRL':'symbol':'1.2-4' }} / {{ ingrediente.unidadeMedida }}
                </td>
                <td style="padding:1rem;text-align:right;white-space:nowrap">
                  <button class="btn btn-ghost btn-sm" type="button" (click)="abrirEdicao(ingrediente)">Editar</button>
                  <button class="btn btn-ghost btn-sm" type="button" [disabled]="excluindoId === ingrediente.id"
                          (click)="excluir(ingrediente)">
                    {{ excluindoId === ingrediente.id ? 'Removendo...' : 'Excluir' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div *ngIf="!carregando && !ingredientes.length && !erro"
             class="empty-state" style="padding:2.5rem 1rem">
          <h2>Nenhum ingrediente cadastrado</h2>
          <p>Adicione ingredientes para começar as fichas técnicas.</p>
        </div>

        <footer *ngIf="!carregando && totalElements > 0"
                style="display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap;padding:1rem;border-top:1px solid var(--border)">
          <span style="font-size:var(--text-sm);color:var(--text-tertiary)">
            {{ totalElements }} ingrediente(s) · Página {{ pagina + 1 }} de {{ totalPages }}
          </span>
          <div style="display:flex;gap:.5rem">
            <button class="btn btn-ghost btn-sm" type="button" [disabled]="pagina === 0" (click)="carregar(pagina - 1)">
              Anterior
            </button>
            <button class="btn btn-ghost btn-sm" type="button" [disabled]="pagina + 1 >= totalPages"
                    (click)="carregar(pagina + 1)">
              Próxima
            </button>
          </div>
        </footer>
      </section>
    </div>

    <div *ngIf="modalAberto" class="modal-overlay" (click)="fecharModal()">
      <section class="modal" style="max-width:500px" role="dialog" aria-modal="true"
               aria-labelledby="ingredient-modal-title" (click)="$event.stopPropagation()">
        <header class="modal-header">
          <h2 id="ingredient-modal-title">{{ editandoId === null ? 'Novo ingrediente' : 'Editar ingrediente' }}</h2>
          <button class="btn btn-ghost btn-icon" type="button" aria-label="Fechar" (click)="fecharModal()">×</button>
        </header>
        <div class="modal-body" style="display:flex;flex-direction:column;gap:1rem">
          <label class="form-group">
            <span class="form-label">Nome *</span>
            <input class="form-input" [(ngModel)]="form.nome" maxlength="100" required>
          </label>
          <label class="form-group">
            <span class="form-label">Unidade de medida *</span>
            <input class="form-input" [(ngModel)]="form.unidadeMedida" maxlength="20"
                   placeholder="Ex.: kg, g, L, unidade" required>
          </label>
          <label class="form-group">
            <span class="form-label">Custo por unidade (R$) *</span>
            <input class="form-input" type="number" [(ngModel)]="form.custoUnitario"
                   min="0" step="0.0001" required>
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
export class IngredientsManagementComponent implements OnInit {
  ingredientes: Ingrediente[] = [];
  pagina = 0;
  totalPages = 0;
  totalElements = 0;
  carregando = false;
  salvando = false;
  modalAberto = false;
  erro = '';
  erroFormulario = '';
  editandoId: number | null = null;
  excluindoId: number | null = null;
  form: IngredienteInput = this.novoFormulario();

  constructor(private service: IngredienteService, private toast: ToastService) {}

  ngOnInit(): void {
    this.carregar(0);
  }

  carregar(pagina: number): void {
    this.carregando = true;
    this.erro = '';
    this.service.listar(pagina).subscribe({
      next: resultado => {
        this.ingredientes = resultado.content;
        this.pagina = resultado.number;
        this.totalPages = resultado.totalPages;
        this.totalElements = resultado.totalElements;
        this.carregando = false;
      },
      error: error => {
        console.error('[Ingredientes] Falha ao carregar página:', error);
        this.erro = 'Não foi possível carregar os ingredientes. Verifique a conexão e tente novamente.';
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

  abrirEdicao(ingrediente: Ingrediente): void {
    this.editandoId = ingrediente.id;
    this.form = {
      nome: ingrediente.nome,
      unidadeMedida: ingrediente.unidadeMedida,
      custoUnitario: ingrediente.custoUnitario
    };
    this.erroFormulario = '';
    this.modalAberto = true;
  }

  fecharModal(): void {
    if (this.salvando) return;
    this.modalAberto = false;
    this.erroFormulario = '';
  }

  salvar(): void {
    const payload: IngredienteInput = {
      nome: this.form.nome.trim(),
      unidadeMedida: this.form.unidadeMedida.trim(),
      custoUnitario: Number(this.form.custoUnitario)
    };
    if (!payload.nome || !payload.unidadeMedida || !Number.isFinite(payload.custoUnitario) || payload.custoUnitario < 0) {
      this.erroFormulario = 'Informe nome, unidade de medida e um custo igual ou maior que zero.';
      return;
    }

    this.salvando = true;
    this.erroFormulario = '';
    const request = this.editandoId === null
      ? this.service.criar(payload)
      : this.service.atualizar(this.editandoId, payload);
    request.subscribe({
      next: () => {
        this.modalAberto = false;
        this.salvando = false;
        this.toast.show(this.editandoId === null ? 'Ingrediente cadastrado.' : 'Ingrediente atualizado.');
        this.carregar(this.pagina);
      },
      error: error => {
        console.error('[Ingredientes] Falha ao salvar ingrediente:', error);
        this.erroFormulario = this.mensagemErro(error, 'Não foi possível salvar o ingrediente.');
        this.salvando = false;
      }
    });
  }

  excluir(ingrediente: Ingrediente): void {
    if (!window.confirm(`Excluir o ingrediente "${ingrediente.nome}"?`)) return;
    this.excluindoId = ingrediente.id;
    this.service.excluir(ingrediente.id).subscribe({
      next: () => {
        this.toast.show('Ingrediente excluído.');
        const proximaPagina = this.ingredientes.length === 1 && this.pagina > 0 ? this.pagina - 1 : this.pagina;
        this.excluindoId = null;
        this.carregar(proximaPagina);
      },
      error: error => {
        console.error('[Ingredientes] Falha ao excluir ingrediente:', error);
        this.toast.show(this.mensagemErro(error, 'Não foi possível excluir o ingrediente.'), 'error');
        this.excluindoId = null;
      }
    });
  }

  private novoFormulario(): IngredienteInput {
    return { nome: '', unidadeMedida: '', custoUnitario: 0 };
  }

  private mensagemErro(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && error.status === 404) return 'Ingrediente não encontrado.';
    if (error instanceof HttpErrorResponse && error.status === 403) return 'Apenas gerentes podem alterar ingredientes.';
    if (error instanceof HttpErrorResponse && error.status === 400) return 'Confira os dados informados.';
    return fallback;
  }
}
