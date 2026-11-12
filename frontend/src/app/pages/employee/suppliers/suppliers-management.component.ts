import { Component, OnDestroy, OnInit } from '@angular/core';
import { CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime } from 'rxjs';
import {
  CatalogoItem, Fornecedor, FornecedorInput, FornecedorService, StatusFornecedor
} from '../../../services/fornecedor.service';
import { Ingrediente, IngredienteService } from '../../../services/ingrediente.service';
import { ToastService } from '../../../services/toast.service';
import { apiErrorMessage } from '../../../core/http-error';
import { formatCnpj, isValidCnpj } from '../../../core/cnpj';

interface CatalogoForm {
  ingredienteId: number | null;
  preco: number | null;
  unidadeVenda: string;
}

// Unidades de venda aceitas pelo backend (além da própria unidade do ingrediente)
const UNIDADES = ['g', 'kg', 'ml', 'L', 'un'];

// RF-021 (fornecedores com CNPJ válido, RN07) e RF-022 (catálogo com preço e unidade)
@Component({
  selector: 'app-suppliers-management',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, NgFor, NgIf],
  template: `
    <div style="max-width:1150px;margin:0 auto;padding:0 1.5rem 2rem">
      <header style="display:flex;align-items:flex-start;justify-content:space-between;gap:1rem;flex-wrap:wrap;margin-bottom:1.5rem"
              class="animate-fade-up">
        <div>
          <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">
            Fornecedores
          </h1>
          <p style="color:var(--text-tertiary);font-size:var(--text-base)">
            Cadastre fornecedores e o catálogo de preços de cada um. Os preços alimentam a cotação de compras.
          </p>
        </div>
        <button class="btn btn-primary" type="button" (click)="abrirNovo()">Novo fornecedor</button>
      </header>

      <section class="card" style="padding:1rem;margin-bottom:1rem;display:flex;gap:1rem;flex-wrap:wrap;align-items:end">
        <label class="form-group" style="flex:1;min-width:220px;margin:0">
          <span class="form-label">Buscar</span>
          <input class="form-input" [(ngModel)]="busca" (ngModelChange)="buscaMudou.next()" placeholder="Razão social ou CNPJ">
        </label>
        <label class="form-group" style="min-width:160px;margin:0">
          <span class="form-label">Status</span>
          <select class="form-input" [(ngModel)]="filtroStatus" (ngModelChange)="carregar(0)">
            <option value="">Todos</option>
            <option value="ATIVO">Ativos</option>
            <option value="INATIVO">Inativos</option>
          </select>
        </label>
      </section>

      <div *ngIf="erro" class="card" role="alert"
           style="padding:1rem;margin-bottom:1rem;color:var(--danger-text);border-color:var(--danger-border)">
        {{ erro }}
        <button *ngIf="!carregando" class="btn btn-ghost btn-sm" type="button" (click)="carregar(pagina)">Tentar novamente</button>
      </div>

      <section class="card">
        <div *ngIf="carregando" role="status" style="padding:1.5rem;color:var(--text-secondary)">Carregando fornecedores...</div>

        <div *ngIf="!carregando && fornecedores.length" style="overflow-x:auto">
          <table style="width:100%;border-collapse:collapse;text-align:left">
            <thead>
              <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                <th style="padding:1rem">Razão social</th>
                <th style="padding:1rem">CNPJ</th>
                <th style="padding:1rem">Contato</th>
                <th style="padding:1rem">Categorias</th>
                <th style="padding:1rem">Status</th>
                <th style="padding:1rem;text-align:right">Ações</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let f of fornecedores" style="border-top:1px solid var(--border);color:var(--text-primary)"
                  [style.opacity]="f.status === 'INATIVO' ? .55 : 1">
                <td style="padding:1rem;font-weight:600">{{ f.razaoSocial }}</td>
                <td style="padding:1rem;color:var(--text-secondary);white-space:nowrap">{{ f.cnpj }}</td>
                <td style="padding:1rem;color:var(--text-secondary)">
                  <div>{{ f.telefone || '—' }}</div>
                  <div style="font-size:var(--text-sm)">{{ f.email || '' }}</div>
                </td>
                <td style="padding:1rem;color:var(--text-secondary)">{{ f.categorias || '—' }}</td>
                <td style="padding:1rem">
                  <span class="badge" [class.badge-green]="f.status === 'ATIVO'" [class.badge-red]="f.status === 'INATIVO'">
                    {{ f.status === 'ATIVO' ? 'Ativo' : 'Inativo' }}
                  </span>
                </td>
                <td style="padding:1rem;text-align:right;white-space:nowrap">
                  <button class="btn btn-ghost btn-sm" type="button" (click)="abrirCatalogo(f)">Catálogo</button>
                  <button class="btn btn-ghost btn-sm" type="button" (click)="abrirEdicao(f)">Editar</button>
                  <button class="btn btn-ghost btn-sm" type="button" [disabled]="alterandoId === f.id"
                          (click)="alterarStatus(f, f.status === 'ATIVO' ? 'INATIVO' : 'ATIVO')">
                    {{ f.status === 'ATIVO' ? 'Desativar' : 'Reativar' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div *ngIf="!carregando && !fornecedores.length && !erro" class="empty-state" style="padding:2.5rem 1rem">
          <h2>Nenhum fornecedor encontrado</h2>
          <p>{{ busca || filtroStatus ? 'Tente outro filtro.' : 'Cadastre o primeiro fornecedor.' }}</p>
        </div>

        <footer *ngIf="!carregando && totalElements > 0"
                style="display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap;padding:1rem;border-top:1px solid var(--border)">
          <span style="font-size:var(--text-sm);color:var(--text-tertiary)">
            {{ totalElements }} fornecedor(es) · Página {{ pagina + 1 }} de {{ totalPages }}
          </span>
          <div style="display:flex;gap:.5rem">
            <button class="btn btn-ghost btn-sm" type="button" [disabled]="pagina === 0" (click)="carregar(pagina - 1)">Anterior</button>
            <button class="btn btn-ghost btn-sm" type="button" [disabled]="pagina + 1 >= totalPages" (click)="carregar(pagina + 1)">Próxima</button>
          </div>
        </footer>
      </section>
    </div>

    <!-- ── Cadastro / edição ─────────────────────────────────── -->
    <div *ngIf="modalAberto" class="modal-overlay" (click)="fecharModal()">
      <section class="modal" style="max-width:520px" role="dialog" aria-modal="true"
               aria-labelledby="supplier-modal-title" (click)="$event.stopPropagation()">
        <header class="modal-header">
          <h2 id="supplier-modal-title">{{ editandoId === null ? 'Novo fornecedor' : 'Editar fornecedor' }}</h2>
          <button class="btn btn-ghost btn-icon" type="button" aria-label="Fechar" (click)="fecharModal()">×</button>
        </header>
        <div class="modal-body" style="display:flex;flex-direction:column;gap:1rem">
          <label class="form-group">
            <span class="form-label">Razão social *</span>
            <input class="form-input" [(ngModel)]="form.razaoSocial" maxlength="150" required>
          </label>
          <label class="form-group">
            <span class="form-label">CNPJ *</span>
            <input #cnpjInput class="form-input" [value]="form.cnpj"
                   (input)="form.cnpj = mascaraCnpj(cnpjInput.value); cnpjInput.value = form.cnpj"
                   inputmode="numeric" maxlength="18" placeholder="00.000.000/0000-00" required
                   [style.border-color]="form.cnpj.length === 18 && !cnpjValido(form.cnpj) ? 'var(--danger-border)' : null">
            <small *ngIf="form.cnpj.length === 18 && !cnpjValido(form.cnpj)" style="color:var(--danger-text)">
              CNPJ inválido: confira os números.
            </small>
          </label>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:1rem">
            <label class="form-group">
              <span class="form-label">Telefone</span>
              <input class="form-input" [(ngModel)]="form.telefone" maxlength="20" placeholder="(11) 3000-0000">
            </label>
            <label class="form-group">
              <span class="form-label">E-mail</span>
              <input class="form-input" type="email" [(ngModel)]="form.email" maxlength="120">
            </label>
          </div>
          <label class="form-group">
            <span class="form-label">Categorias</span>
            <input class="form-input" [(ngModel)]="form.categorias" maxlength="255" placeholder="Ex.: Carnes, hortifruti">
          </label>
          <label class="form-group">
            <span class="form-label">Status</span>
            <select class="form-input" [(ngModel)]="form.status">
              <option value="ATIVO">Ativo</option>
              <option value="INATIVO">Inativo</option>
            </select>
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

    <!-- ── Catálogo (RF-022) ─────────────────────────────────── -->
    <div *ngIf="catalogoDe" class="modal-overlay" (click)="fecharCatalogo()">
      <section class="modal" style="max-width:860px" role="dialog" aria-modal="true"
               aria-labelledby="catalog-modal-title" (click)="$event.stopPropagation()">
        <header class="modal-header">
          <div>
            <h2 id="catalog-modal-title">Catálogo</h2>
            <span style="color:var(--text-tertiary);font-size:var(--text-sm)">{{ catalogoDe.razaoSocial }} · {{ catalogoDe.cnpj }}</span>
          </div>
          <button class="btn btn-ghost btn-icon" type="button" aria-label="Fechar" (click)="fecharCatalogo()">×</button>
        </header>
        <div class="modal-body" style="display:flex;flex-direction:column;gap:1.25rem">

          <div class="card" style="padding:1rem">
            <h3 style="margin:0 0 .75rem;font-size:var(--text-base);color:var(--text-primary)">
              {{ itemEditandoId === null ? 'Adicionar ingrediente' : 'Editar preço' }}
            </h3>
            <div style="display:grid;grid-template-columns:2fr 1fr 1fr auto;gap:.75rem;align-items:end">
              <label class="form-group" style="margin:0">
                <span class="form-label">Ingrediente</span>
                <select class="form-input" [(ngModel)]="itemForm.ingredienteId" (ngModelChange)="ingredienteEscolhido()"
                        [disabled]="itemEditandoId !== null">
                  <option [ngValue]="null">Selecione</option>
                  <option *ngFor="let i of ingredientesDisponiveis()" [ngValue]="i.id">{{ i.nome }} ({{ i.unidadeMedida }})</option>
                </select>
              </label>
              <label class="form-group" style="margin:0">
                <span class="form-label">Preço (R$)</span>
                <input class="form-input" type="number" [(ngModel)]="itemForm.preco" min="0.0001" step="0.01">
              </label>
              <label class="form-group" style="margin:0">
                <span class="form-label">Por</span>
                <select class="form-input" [(ngModel)]="itemForm.unidadeVenda">
                  <option *ngFor="let u of unidadesPara(itemForm.ingredienteId)" [value]="u">{{ u }}</option>
                </select>
              </label>
              <div style="display:flex;gap:.5rem">
                <button *ngIf="itemEditandoId !== null" class="btn btn-ghost" type="button" (click)="cancelarEdicaoItem()">Cancelar</button>
                <button class="btn btn-primary" type="button" [disabled]="salvandoItem" (click)="salvarItem()">
                  {{ salvandoItem ? 'Salvando...' : (itemEditandoId === null ? 'Adicionar' : 'Salvar') }}
                </button>
              </div>
            </div>
            <p *ngIf="erroItem" role="alert" style="color:var(--danger-text);margin:.75rem 0 0">{{ erroItem }}</p>
          </div>

          <div *ngIf="carregandoCatalogo" role="status" style="color:var(--text-secondary)">Carregando catálogo...</div>

          <div *ngIf="!carregandoCatalogo && catalogo.length" style="overflow-x:auto">
            <table style="width:100%;border-collapse:collapse;text-align:left">
              <thead>
                <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                  <th style="padding:.75rem">Ingrediente</th>
                  <th style="padding:.75rem">Preço de venda</th>
                  <th style="padding:.75rem">Na unidade do ingrediente</th>
                  <th style="padding:.75rem;text-align:right">Ações</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let item of catalogo" style="border-top:1px solid var(--border);color:var(--text-primary)">
                  <td style="padding:.75rem;font-weight:600">{{ item.ingredienteNome }}</td>
                  <td style="padding:.75rem;color:var(--text-secondary)">
                    {{ item.preco | currency:'BRL':'symbol':'1.2-4' }} / {{ item.unidadeVenda }}
                  </td>
                  <td style="padding:.75rem;color:var(--text-secondary)">
                    <ng-container *ngIf="item.precoPorUnidadeIngrediente !== null; else semConversao">
                      {{ item.precoPorUnidadeIngrediente | currency:'BRL':'symbol':'1.2-4' }} / {{ item.unidadeMedida }}
                    </ng-container>
                    <ng-template #semConversao>—</ng-template>
                  </td>
                  <td style="padding:.75rem;text-align:right;white-space:nowrap">
                    <button class="btn btn-ghost btn-sm" type="button" (click)="editarItem(item)">Editar</button>
                    <button class="btn btn-ghost btn-sm" type="button" [disabled]="removendoItemId === item.id"
                            (click)="removerItem(item)">Remover</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div *ngIf="!carregandoCatalogo && !catalogo.length" class="empty-state" style="padding:1.5rem 1rem">
            <h2>Catálogo vazio</h2>
            <p>Adicione os ingredientes que este fornecedor vende e o preço de cada um.</p>
          </div>
        </div>
      </section>
    </div>
  `
})
export class SuppliersManagementComponent implements OnInit, OnDestroy {
  fornecedores: Fornecedor[] = [];
  pagina = 0;
  totalPages = 0;
  totalElements = 0;
  busca = '';
  filtroStatus: StatusFornecedor | '' = '';
  buscaMudou = new Subject<void>();
  carregando = false;
  salvando = false;
  modalAberto = false;
  erro = '';
  erroFormulario = '';
  editandoId: number | null = null;
  alterandoId: number | null = null;
  form: FornecedorInput = this.novoFormulario();

  // Catálogo
  catalogoDe: Fornecedor | null = null;
  catalogo: CatalogoItem[] = [];
  ingredientes: Ingrediente[] = [];
  carregandoCatalogo = false;
  salvandoItem = false;
  erroItem = '';
  itemEditandoId: number | null = null;
  removendoItemId: number | null = null;
  itemForm: CatalogoForm = this.novoItem();

  private buscaSub?: Subscription;

  constructor(
    private service: FornecedorService,
    private ingredienteService: IngredienteService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.buscaSub = this.buscaMudou.pipe(debounceTime(350)).subscribe(() => this.carregar(0));
    this.carregar(0);
  }

  ngOnDestroy(): void {
    this.buscaSub?.unsubscribe();
  }

  mascaraCnpj(valor: string): string { return formatCnpj(valor); }
  cnpjValido(valor: string): boolean { return isValidCnpj(valor); }

  carregar(pagina: number): void {
    this.carregando = true;
    this.erro = '';
    this.service.listar(pagina, 10, this.busca, this.filtroStatus).subscribe({
      next: resultado => {
        this.fornecedores = resultado.content;
        this.pagina = resultado.number;
        this.totalPages = resultado.totalPages;
        this.totalElements = resultado.totalElements;
        this.carregando = false;
      },
      error: error => {
        this.erro = apiErrorMessage(error, 'Não foi possível carregar os fornecedores.');
        this.carregando = false;
      }
    });
  }

  // ── Cadastro ────────────────────────────────────────────────

  abrirNovo(): void {
    this.editandoId = null;
    this.form = this.novoFormulario();
    this.erroFormulario = '';
    this.modalAberto = true;
  }

  abrirEdicao(f: Fornecedor): void {
    this.editandoId = f.id;
    this.form = {
      razaoSocial: f.razaoSocial,
      cnpj: f.cnpj,
      telefone: f.telefone,
      email: f.email,
      categorias: f.categorias,
      status: f.status
    };
    this.erroFormulario = '';
    this.modalAberto = true;
  }

  fecharModal(): void {
    if (this.salvando) return;
    this.modalAberto = false;
  }

  salvar(): void {
    const payload: FornecedorInput = {
      razaoSocial: this.form.razaoSocial.trim(),
      cnpj: this.form.cnpj,
      telefone: this.form.telefone?.trim() || null,
      email: this.form.email?.trim() || null,
      categorias: this.form.categorias?.trim() || null,
      status: this.form.status
    };
    if (!payload.razaoSocial) { this.erroFormulario = 'Informe a razão social.'; return; }
    if (!isValidCnpj(payload.cnpj)) { this.erroFormulario = 'CNPJ inválido: confira os 14 números.'; return; }
    if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
      this.erroFormulario = 'E-mail inválido.';
      return;
    }

    this.salvando = true;
    this.erroFormulario = '';
    const novo = this.editandoId === null;
    const request = novo ? this.service.criar(payload) : this.service.atualizar(this.editandoId!, payload);
    request.subscribe({
      next: () => {
        this.salvando = false;
        this.modalAberto = false;
        this.toast.show(novo ? 'Fornecedor cadastrado.' : 'Fornecedor atualizado.');
        this.carregar(novo ? 0 : this.pagina);
      },
      error: error => {
        this.salvando = false;
        this.erroFormulario = apiErrorMessage(error, 'Não foi possível salvar o fornecedor.');
      }
    });
  }

  alterarStatus(f: Fornecedor, status: StatusFornecedor): void {
    if (status === 'INATIVO' && !window.confirm(`Desativar "${f.razaoSocial}"? Ele sai da cotação e não recebe pedidos novos.`)) return;
    this.alterandoId = f.id;
    this.service.alterarStatus(f.id, status).subscribe({
      next: () => {
        this.alterandoId = null;
        this.toast.show(status === 'INATIVO' ? 'Fornecedor desativado.' : 'Fornecedor reativado.');
        this.carregar(this.pagina);
      },
      error: error => {
        this.alterandoId = null;
        this.toast.show(apiErrorMessage(error, 'Não foi possível alterar o status.'), 'error');
      }
    });
  }

  // ── Catálogo ────────────────────────────────────────────────

  abrirCatalogo(f: Fornecedor): void {
    this.catalogoDe = f;
    this.catalogo = [];
    this.cancelarEdicaoItem();
    this.carregarCatalogo();
    if (!this.ingredientes.length) {
      this.ingredienteService.listar(0, 100).subscribe({
        next: pagina => this.ingredientes = pagina.content.filter(i => i.status === 'ATIVO'),
        error: () => this.erroItem = 'Não foi possível carregar os ingredientes.'
      });
    }
  }

  fecharCatalogo(): void {
    if (this.salvandoItem) return;
    this.catalogoDe = null;
  }

  carregarCatalogo(): void {
    if (!this.catalogoDe) return;
    this.carregandoCatalogo = true;
    this.service.catalogo(this.catalogoDe.id).subscribe({
      next: itens => { this.catalogo = itens; this.carregandoCatalogo = false; },
      error: error => {
        this.carregandoCatalogo = false;
        this.erroItem = apiErrorMessage(error, 'Não foi possível carregar o catálogo.');
      }
    });
  }

  // Na hora de adicionar, só aparecem ingredientes que ainda não estão no catálogo
  ingredientesDisponiveis(): Ingrediente[] {
    if (this.itemEditandoId !== null) return this.ingredientes;
    const usados = new Set(this.catalogo.map(c => c.ingredienteId));
    return this.ingredientes.filter(i => !usados.has(i.id));
  }

  unidadesPara(ingredienteId: number | null): string[] {
    const unidade = this.unidadeDoIngrediente(ingredienteId);
    if (!unidade) return UNIDADES;
    return UNIDADES.some(u => u.toLowerCase() === unidade.toLowerCase()) ? UNIDADES : [unidade, ...UNIDADES];
  }

  ingredienteEscolhido(): void {
    const unidade = this.unidadeDoIngrediente(this.itemForm.ingredienteId);
    if (unidade) {
      this.itemForm.unidadeVenda = UNIDADES.find(u => u.toLowerCase() === unidade.toLowerCase()) ?? unidade;
    }
  }

  editarItem(item: CatalogoItem): void {
    this.itemEditandoId = item.id;
    this.itemForm = { ingredienteId: item.ingredienteId, preco: item.preco, unidadeVenda: item.unidadeVenda };
    this.erroItem = '';
  }

  cancelarEdicaoItem(): void {
    this.itemEditandoId = null;
    this.itemForm = this.novoItem();
    this.erroItem = '';
  }

  salvarItem(): void {
    if (!this.catalogoDe) return;
    const preco = Number(this.itemForm.preco);
    if (this.itemForm.ingredienteId === null) { this.erroItem = 'Escolha o ingrediente.'; return; }
    if (!Number.isFinite(preco) || preco <= 0) { this.erroItem = 'Informe um preço maior que zero.'; return; }

    const payload = { ingredienteId: this.itemForm.ingredienteId, preco, unidadeVenda: this.itemForm.unidadeVenda };
    const fornecedorId = this.catalogoDe.id;
    const editando = this.itemEditandoId !== null;
    const request = editando
      ? this.service.atualizarItem(fornecedorId, this.itemEditandoId!, payload)
      : this.service.adicionarItem(fornecedorId, payload);

    this.salvandoItem = true;
    this.erroItem = '';
    request.subscribe({
      next: () => {
        this.salvandoItem = false;
        this.toast.show(editando ? 'Preço atualizado.' : 'Ingrediente adicionado ao catálogo.');
        this.cancelarEdicaoItem();
        this.carregarCatalogo();
      },
      error: error => {
        this.salvandoItem = false;
        this.erroItem = apiErrorMessage(error, 'Não foi possível salvar o item.');
      }
    });
  }

  removerItem(item: CatalogoItem): void {
    if (!this.catalogoDe || !window.confirm(`Tirar "${item.ingredienteNome}" do catálogo?`)) return;
    this.removendoItemId = item.id;
    this.service.removerItem(this.catalogoDe.id, item.id).subscribe({
      next: () => {
        this.removendoItemId = null;
        this.toast.show('Item removido do catálogo.');
        if (this.itemEditandoId === item.id) this.cancelarEdicaoItem();
        this.carregarCatalogo();
      },
      error: error => {
        this.removendoItemId = null;
        this.toast.show(apiErrorMessage(error, 'Não foi possível remover o item.'), 'error');
      }
    });
  }

  private unidadeDoIngrediente(id: number | null): string | null {
    if (id === null) return null;
    return this.ingredientes.find(i => i.id === id)?.unidadeMedida
      ?? this.catalogo.find(c => c.ingredienteId === id)?.unidadeMedida
      ?? null;
  }

  private novoFormulario(): FornecedorInput {
    return { razaoSocial: '', cnpj: '', telefone: null, email: null, categorias: null, status: 'ATIVO' };
  }

  private novoItem(): CatalogoForm {
    return { ingredienteId: null, preco: null, unidadeVenda: 'kg' };
  }
}
