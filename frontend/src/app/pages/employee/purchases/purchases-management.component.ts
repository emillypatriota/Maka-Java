import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CompraService, Cotacao, Oferta, PedidoCompra, PedidoCompraInput, STATUS_COMPRA, StatusPedidoCompra
} from '../../../services/compra.service';
import { CatalogoItem, Fornecedor, FornecedorService } from '../../../services/fornecedor.service';
import { EstoqueService } from '../../../services/estoque.service';
import { ToastService } from '../../../services/toast.service';
import { apiErrorMessage } from '../../../core/http-error';

interface LinhaForm {
  ingredienteId: number | null;
  quantidade: number | null;
  precoUnitario: number | null;   // vazio = preço do catálogo
}

type Aba = 'pedidos' | 'cotacao';

// Bloco 7: pedido de compra (RF-024), recebimento com entrada no estoque (RF-025, RN05)
// e cotação comparativa entre fornecedores (RF-023)
@Component({
  selector: 'app-purchases-management',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, FormsModule, NgClass, NgFor, NgIf],
  template: `
    <div style="max-width:1200px;margin:0 auto;padding:0 1.5rem 2rem">
      <header style="display:flex;align-items:flex-start;justify-content:space-between;gap:1rem;flex-wrap:wrap;margin-bottom:1.5rem"
              class="animate-fade-up">
        <div>
          <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">
            Compras
          </h1>
          <p style="color:var(--text-tertiary);font-size:var(--text-base)">
            Compare preços, monte o pedido e, ao receber a mercadoria, o estoque e o custo dos ingredientes são atualizados.
          </p>
        </div>
        <button class="btn btn-primary" type="button" (click)="abrirNovo()">Novo pedido de compra</button>
      </header>

      <div style="display:flex;gap:.5rem;margin-bottom:1rem" role="tablist">
        <button class="btn btn-sm" type="button" role="tab" [attr.aria-selected]="aba === 'pedidos'"
                [ngClass]="aba === 'pedidos' ? 'btn-primary' : 'btn-ghost'" (click)="trocarAba('pedidos')">Pedidos de compra</button>
        <button class="btn btn-sm" type="button" role="tab" [attr.aria-selected]="aba === 'cotacao'"
                [ngClass]="aba === 'cotacao' ? 'btn-primary' : 'btn-ghost'" (click)="trocarAba('cotacao')">Cotação de preços</button>
      </div>

      <!-- ── Pedidos de compra (RF-024) ─────────────────────── -->
      <ng-container *ngIf="aba === 'pedidos'">
        <section class="card" style="padding:1rem;margin-bottom:1rem;display:flex;gap:.5rem;flex-wrap:wrap;align-items:center">
          <span style="color:var(--text-tertiary);font-size:var(--text-sm);margin-right:.25rem">Status:</span>
          <button *ngFor="let s of filtros" class="btn btn-sm" type="button"
                  [ngClass]="filtroStatus === s.valor ? 'btn-primary' : 'btn-ghost'" (click)="filtrar(s.valor)">
            {{ s.label }}
          </button>
        </section>

        <div *ngIf="erro" class="card" role="alert"
             style="padding:1rem;margin-bottom:1rem;color:var(--danger-text);border-color:var(--danger-border)">
          {{ erro }}
          <button *ngIf="!carregando" class="btn btn-ghost btn-sm" type="button" (click)="carregar(pagina)">Tentar novamente</button>
        </div>

        <section class="card">
          <div *ngIf="carregando" role="status" style="padding:1.5rem;color:var(--text-secondary)">Carregando pedidos de compra...</div>

          <div *ngIf="!carregando && pedidos.length" style="overflow-x:auto">
            <table style="width:100%;border-collapse:collapse;text-align:left">
              <thead>
                <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                  <th style="padding:1rem">Nº</th>
                  <th style="padding:1rem">Fornecedor</th>
                  <th style="padding:1rem">Itens</th>
                  <th style="padding:1rem">Total</th>
                  <th style="padding:1rem">Status</th>
                  <th style="padding:1rem">Criado em</th>
                  <th style="padding:1rem;text-align:right">Ações</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let p of pedidos" style="border-top:1px solid var(--border);color:var(--text-primary)">
                  <td style="padding:1rem;font-weight:600">#{{ p.id }}</td>
                  <td style="padding:1rem">{{ p.fornecedorNome }}</td>
                  <td style="padding:1rem;color:var(--text-secondary)">{{ p.itens.length }}</td>
                  <td style="padding:1rem;font-weight:600">{{ p.valorTotal | currency:'BRL' }}</td>
                  <td style="padding:1rem"><span class="badge" [ngClass]="statusInfo(p.status).badge">{{ statusInfo(p.status).label }}</span></td>
                  <td style="padding:1rem;color:var(--text-secondary)">{{ p.createdAt ? (p.createdAt | date:'dd/MM/yyyy HH:mm') : '—' }}</td>
                  <td style="padding:1rem;text-align:right;white-space:nowrap">
                    <button class="btn btn-ghost btn-sm" type="button" (click)="verDetalhe(p)">Ver</button>
                    <ng-container *ngIf="p.status === 'RASCUNHO'">
                      <button class="btn btn-ghost btn-sm" type="button" (click)="abrirEdicao(p)">Editar</button>
                      <button class="btn btn-ghost btn-sm" type="button" [disabled]="acaoId === p.id" (click)="enviar(p)">Enviar</button>
                    </ng-container>
                    <button *ngIf="p.status === 'ENVIADO'" class="btn btn-primary btn-sm" type="button" (click)="abrirRecebimento(p)">Receber</button>
                    <button *ngIf="p.status === 'RASCUNHO' || p.status === 'ENVIADO'" class="btn btn-ghost btn-sm" type="button"
                            [disabled]="acaoId === p.id" (click)="cancelar(p)">Cancelar</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div *ngIf="!carregando && !pedidos.length && !erro" class="empty-state" style="padding:2.5rem 1rem">
            <h2>Nenhum pedido de compra</h2>
            <p>{{ filtroStatus ? 'Nenhum pedido com este status.' : 'Crie um pedido ou comece pela cotação de preços.' }}</p>
          </div>

          <footer *ngIf="!carregando && totalElements > 0"
                  style="display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap;padding:1rem;border-top:1px solid var(--border)">
            <span style="font-size:var(--text-sm);color:var(--text-tertiary)">
              {{ totalElements }} pedido(s) · Página {{ pagina + 1 }} de {{ totalPages }}
            </span>
            <div style="display:flex;gap:.5rem">
              <button class="btn btn-ghost btn-sm" type="button" [disabled]="pagina === 0" (click)="carregar(pagina - 1)">Anterior</button>
              <button class="btn btn-ghost btn-sm" type="button" [disabled]="pagina + 1 >= totalPages" (click)="carregar(pagina + 1)">Próxima</button>
            </div>
          </footer>
        </section>
      </ng-container>

      <!-- ── Cotação (RF-023) ───────────────────────────────── -->
      <ng-container *ngIf="aba === 'cotacao'">
        <section class="card" style="padding:1rem;margin-bottom:1rem;display:flex;gap:1rem;align-items:end;flex-wrap:wrap">
          <label class="form-group" style="flex:1;min-width:220px;margin:0">
            <span class="form-label">Filtrar ingrediente</span>
            <input class="form-input" [(ngModel)]="filtroCotacao" placeholder="Ex.: salmão">
          </label>
          <button class="btn btn-ghost" type="button" [disabled]="carregandoCotacao" (click)="carregarCotacao()">Atualizar</button>
        </section>

        <div *ngIf="erroCotacao" class="card" role="alert"
             style="padding:1rem;margin-bottom:1rem;color:var(--danger-text);border-color:var(--danger-border)">{{ erroCotacao }}</div>
        <div *ngIf="carregandoCotacao" role="status" class="card" style="padding:1.5rem;color:var(--text-secondary)">Comparando preços...</div>

        <div *ngIf="!carregandoCotacao" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(340px,1fr));gap:1rem">
          <section *ngFor="let c of cotacoesFiltradas()" class="card" style="padding:1rem">
            <header style="display:flex;justify-content:space-between;gap:.5rem;align-items:baseline;margin-bottom:.75rem">
              <h3 style="margin:0;font-size:var(--text-base);color:var(--text-primary)">{{ c.ingredienteNome }}</h3>
              <span style="color:var(--text-tertiary);font-size:var(--text-xs);white-space:nowrap">
                custo atual {{ c.custoAtual | currency:'BRL':'symbol':'1.2-4' }}/{{ c.unidadeMedida }}
              </span>
            </header>
            <table style="width:100%;border-collapse:collapse;text-align:left;font-size:var(--text-sm)">
              <tbody>
                <tr *ngFor="let o of c.ofertas" style="border-top:1px solid var(--border)"
                    [style.background]="o.melhorPreco ? 'var(--success-bg)' : null">
                  <td style="padding:.5rem;color:var(--text-primary)">
                    {{ o.fornecedorNome }}
                    <span *ngIf="o.melhorPreco" class="badge badge-green" style="margin-left:.25rem">Melhor preço</span>
                  </td>
                  <td style="padding:.5rem;color:var(--text-secondary);white-space:nowrap">
                    {{ o.preco | currency:'BRL':'symbol':'1.2-4' }}/{{ o.unidadeVenda }}
                    <div *ngIf="o.precoPorUnidadeIngrediente !== null" style="font-size:var(--text-xs);color:var(--text-tertiary)">
                      {{ o.precoPorUnidadeIngrediente | currency:'BRL':'symbol':'1.2-4' }}/{{ c.unidadeMedida }}
                      <ng-container *ngIf="!o.melhorPreco && o.diferencaPercentual !== null">
                        · +{{ o.diferencaPercentual | number:'1.1-1' }}%
                      </ng-container>
                    </div>
                  </td>
                  <td style="padding:.5rem;text-align:right">
                    <button class="btn btn-ghost btn-sm" type="button" (click)="comprarDaOferta(c, o)">Comprar</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </section>
        </div>

        <div *ngIf="!carregandoCotacao && !cotacoesFiltradas().length && !erroCotacao" class="card empty-state" style="padding:2.5rem 1rem">
          <h2>Nada para comparar</h2>
          <p>{{ filtroCotacao ? 'Nenhum ingrediente com esse nome tem oferta.' : 'Cadastre o catálogo dos fornecedores para comparar preços.' }}</p>
        </div>
      </ng-container>
    </div>

    <!-- ── Novo / editar pedido ─────────────────────────────── -->
    <div *ngIf="modalAberto" class="modal-overlay" (click)="fecharModal()">
      <section class="modal" style="max-width:900px" role="dialog" aria-modal="true"
               aria-labelledby="purchase-modal-title" (click)="$event.stopPropagation()">
        <header class="modal-header">
          <h2 id="purchase-modal-title">{{ editandoId === null ? 'Novo pedido de compra' : 'Editar pedido #' + editandoId }}</h2>
          <button class="btn btn-ghost btn-icon" type="button" aria-label="Fechar" (click)="fecharModal()">×</button>
        </header>
        <div class="modal-body" style="display:flex;flex-direction:column;gap:1rem">
          <label class="form-group">
            <span class="form-label">Fornecedor *</span>
            <select class="form-input" [(ngModel)]="fornecedorId" (ngModelChange)="fornecedorMudou()">
              <option [ngValue]="null">Selecione</option>
              <option *ngFor="let f of fornecedoresAtivos" [ngValue]="f.id">{{ f.razaoSocial }} · {{ f.cnpj }}</option>
            </select>
          </label>

          <div *ngIf="fornecedorId !== null && carregandoCatalogo" role="status" style="color:var(--text-secondary)">Carregando catálogo...</div>
          <p *ngIf="fornecedorId !== null && !carregandoCatalogo && !catalogo.length" style="color:var(--warning-text)">
            Este fornecedor ainda não tem catálogo. Cadastre os preços em Fornecedores › Catálogo.
          </p>

          <ng-container *ngIf="fornecedorId !== null && catalogo.length">
            <div style="overflow-x:auto">
              <table style="width:100%;border-collapse:collapse;text-align:left">
                <thead>
                  <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                    <th style="padding:.5rem">Ingrediente</th>
                    <th style="padding:.5rem;width:140px">Quantidade</th>
                    <th style="padding:.5rem;width:170px">Preço por unidade</th>
                    <th style="padding:.5rem;text-align:right">Subtotal</th>
                    <th style="padding:.5rem"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let linha of linhas; let i = index" style="border-top:1px solid var(--border)">
                    <td style="padding:.5rem">
                      <select class="form-input" [(ngModel)]="linha.ingredienteId">
                        <option [ngValue]="null">Selecione</option>
                        <option *ngFor="let item of catalogo" [ngValue]="item.ingredienteId"
                                [disabled]="jaEscolhido(item.ingredienteId, i)">
                          {{ item.ingredienteNome }} ({{ item.preco | currency:'BRL':'symbol':'1.2-4' }}/{{ item.unidadeVenda }})
                        </option>
                      </select>
                    </td>
                    <td style="padding:.5rem">
                      <div style="display:flex;align-items:center;gap:.35rem">
                        <input class="form-input" type="number" [(ngModel)]="linha.quantidade" min="0.001" step="1">
                        <span style="color:var(--text-tertiary);font-size:var(--text-sm)">{{ unidadeDe(linha.ingredienteId) }}</span>
                      </div>
                    </td>
                    <td style="padding:.5rem">
                      <input class="form-input" type="number" [(ngModel)]="linha.precoUnitario" min="0.0001" step="0.0001"
                             [placeholder]="placeholderPreco(linha.ingredienteId)">
                    </td>
                    <td style="padding:.5rem;text-align:right;white-space:nowrap;color:var(--text-primary)">
                      {{ subtotal(linha) | currency:'BRL' }}
                    </td>
                    <td style="padding:.5rem;text-align:right">
                      <button class="btn btn-ghost btn-sm" type="button" aria-label="Remover item" (click)="removerLinha(i)">×</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;gap:1rem;flex-wrap:wrap">
              <button class="btn btn-ghost btn-sm" type="button" [disabled]="linhas.length >= catalogo.length" (click)="adicionarLinha()">
                + Adicionar item
              </button>
              <strong style="color:var(--text-primary)">Total estimado: {{ totalEstimado() | currency:'BRL' }}</strong>
            </div>
            <small style="color:var(--text-tertiary)">
              A quantidade é na unidade do ingrediente (a mesma do estoque). Deixe o preço vazio para usar o do catálogo.
              O total final é calculado pelo servidor.
            </small>
          </ng-container>

          <p *ngIf="erroFormulario" role="alert" style="color:var(--danger-text)">{{ erroFormulario }}</p>
        </div>
        <footer class="modal-footer">
          <button class="btn btn-ghost" type="button" (click)="fecharModal()">Cancelar</button>
          <button class="btn btn-primary" type="button" [disabled]="salvando || !catalogo.length" (click)="salvar()">
            {{ salvando ? 'Salvando...' : 'Salvar rascunho' }}
          </button>
        </footer>
      </section>
    </div>

    <!-- ── Detalhe ───────────────────────────────────────────── -->
    <div *ngIf="detalhe" class="modal-overlay" (click)="detalhe = null">
      <section class="modal" style="max-width:760px" role="dialog" aria-modal="true"
               aria-labelledby="purchase-detail-title" (click)="$event.stopPropagation()">
        <header class="modal-header">
          <div>
            <h2 id="purchase-detail-title">Pedido de compra #{{ detalhe.id }}</h2>
            <span style="color:var(--text-tertiary);font-size:var(--text-sm)">
              {{ detalhe.fornecedorNome }} · {{ detalhe.fornecedorCnpj }}
            </span>
          </div>
          <button class="btn btn-ghost btn-icon" type="button" aria-label="Fechar" (click)="detalhe = null">×</button>
        </header>
        <div class="modal-body" style="display:flex;flex-direction:column;gap:1rem">
          <div style="display:flex;gap:1.5rem;flex-wrap:wrap;color:var(--text-secondary);font-size:var(--text-sm)">
            <span>Status: <span class="badge" [ngClass]="statusInfo(detalhe.status).badge">{{ statusInfo(detalhe.status).label }}</span></span>
            <span>Criado por: {{ detalhe.criadoPor || '—' }}</span>
            <span>Criado em: {{ detalhe.createdAt ? (detalhe.createdAt | date:'dd/MM/yyyy HH:mm') : '—' }}</span>
            <span>Atualizado em: {{ detalhe.updatedAt ? (detalhe.updatedAt | date:'dd/MM/yyyy HH:mm') : '—' }}</span>
          </div>
          <table style="width:100%;border-collapse:collapse;text-align:left">
            <thead>
              <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                <th style="padding:.5rem">Ingrediente</th>
                <th style="padding:.5rem">Quantidade</th>
                <th style="padding:.5rem">Preço unitário</th>
                <th style="padding:.5rem;text-align:right">Subtotal</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of detalhe.itens" style="border-top:1px solid var(--border);color:var(--text-primary)">
                <td style="padding:.5rem">{{ item.ingredienteNome }}</td>
                <td style="padding:.5rem;color:var(--text-secondary)">{{ item.quantidade | number:'1.0-3' }} {{ item.unidadeMedida }}</td>
                <td style="padding:.5rem;color:var(--text-secondary)">{{ item.precoUnitario | currency:'BRL':'symbol':'1.2-4' }}/{{ item.unidadeMedida }}</td>
                <td style="padding:.5rem;text-align:right">{{ item.subtotal | currency:'BRL' }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr style="border-top:1px solid var(--border-strong)">
                <td colspan="3" style="padding:.5rem;font-weight:700;color:var(--text-primary)">Total</td>
                <td style="padding:.5rem;text-align:right;font-weight:700;color:var(--text-primary)">{{ detalhe.valorTotal | currency:'BRL' }}</td>
              </tr>
            </tfoot>
          </table>
          <p *ngIf="detalhe.status === 'RECEBIDO'" style="color:var(--success-text);font-size:var(--text-sm)">
            Mercadoria recebida: as entradas estão em Estoque › Movimentações e o custo dos ingredientes foi atualizado.
          </p>
        </div>
      </section>
    </div>

    <!-- ── Recebimento (RF-025) ──────────────────────────────── -->
    <div *ngIf="recebendo" class="modal-overlay" (click)="fecharRecebimento()">
      <section class="modal" style="max-width:480px" role="dialog" aria-modal="true"
               aria-labelledby="receive-modal-title" (click)="$event.stopPropagation()">
        <header class="modal-header">
          <h2 id="receive-modal-title">Receber pedido #{{ recebendo.id }}</h2>
          <button class="btn btn-ghost btn-icon" type="button" aria-label="Fechar" (click)="fecharRecebimento()">×</button>
        </header>
        <div class="modal-body" style="display:flex;flex-direction:column;gap:1rem">
          <p style="color:var(--text-secondary);margin:0">
            Confirme que chegou tudo de <strong>{{ recebendo.fornecedorNome }}</strong>
            ({{ recebendo.itens.length }} item(ns), {{ recebendo.valorTotal | currency:'BRL' }}).
            Cada item vira uma entrada no estoque e o custo do ingrediente passa a ser o preço desta compra.
          </p>
          <label class="form-group">
            <span class="form-label">Lote</span>
            <input class="form-input" [(ngModel)]="lote" maxlength="40" [placeholder]="'PC-' + recebendo.id">
          </label>
          <label class="form-group">
            <span class="form-label">Validade</span>
            <input class="form-input" type="date" [(ngModel)]="validade" [min]="hoje">
          </label>
          <p *ngIf="erroRecebimento" role="alert" style="color:var(--danger-text)">{{ erroRecebimento }}</p>
        </div>
        <footer class="modal-footer">
          <button class="btn btn-ghost" type="button" (click)="fecharRecebimento()">Voltar</button>
          <button class="btn btn-primary" type="button" [disabled]="confirmandoRecebimento" (click)="confirmarRecebimento()">
            {{ confirmandoRecebimento ? 'Recebendo...' : 'Confirmar recebimento' }}
          </button>
        </footer>
      </section>
    </div>
  `
})
export class PurchasesManagementComponent implements OnInit {
  readonly filtros: { valor: StatusPedidoCompra | ''; label: string }[] = [
    { valor: '', label: 'Todos' },
    { valor: 'RASCUNHO', label: 'Rascunho' },
    { valor: 'ENVIADO', label: 'Enviado' },
    { valor: 'RECEBIDO', label: 'Recebido' },
    { valor: 'CANCELADO', label: 'Cancelado' },
  ];

  aba: Aba = 'pedidos';

  // Lista
  pedidos: PedidoCompra[] = [];
  pagina = 0;
  totalPages = 0;
  totalElements = 0;
  filtroStatus: StatusPedidoCompra | '' = '';
  carregando = false;
  erro = '';
  acaoId: number | null = null;
  detalhe: PedidoCompra | null = null;

  // Formulário do pedido
  modalAberto = false;
  salvando = false;
  erroFormulario = '';
  editandoId: number | null = null;
  fornecedoresAtivos: Fornecedor[] = [];
  fornecedorId: number | null = null;
  catalogo: CatalogoItem[] = [];
  carregandoCatalogo = false;
  linhas: LinhaForm[] = [];

  // Recebimento
  recebendo: PedidoCompra | null = null;
  lote = '';
  validade = '';
  erroRecebimento = '';
  confirmandoRecebimento = false;
  readonly hoje = PurchasesManagementComponent.dataLocal(new Date());

  // Cotação
  cotacoes: Cotacao[] = [];
  filtroCotacao = '';
  carregandoCotacao = false;
  erroCotacao = '';
  private cotacaoCarregada = false;

  constructor(
    private compraService: CompraService,
    private fornecedorService: FornecedorService,
    private estoqueService: EstoqueService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.carregar(0);
  }

  statusInfo(status: StatusPedidoCompra) { return STATUS_COMPRA[status]; }

  trocarAba(aba: Aba): void {
    this.aba = aba;
    if (aba === 'cotacao' && !this.cotacaoCarregada) this.carregarCotacao();
  }

  // ── Lista ───────────────────────────────────────────────────

  filtrar(status: StatusPedidoCompra | ''): void {
    this.filtroStatus = status;
    this.carregar(0);
  }

  carregar(pagina: number): void {
    this.carregando = true;
    this.erro = '';
    this.compraService.listar(pagina, 10, this.filtroStatus).subscribe({
      next: resultado => {
        this.pedidos = resultado.content;
        this.pagina = resultado.number;
        this.totalPages = resultado.totalPages;
        this.totalElements = resultado.totalElements;
        this.carregando = false;
      },
      error: error => {
        this.erro = apiErrorMessage(error, 'Não foi possível carregar os pedidos de compra.');
        this.carregando = false;
      }
    });
  }

  verDetalhe(p: PedidoCompra): void {
    this.detalhe = p;
  }

  enviar(p: PedidoCompra): void {
    if (!window.confirm(`Enviar o pedido #${p.id} para ${p.fornecedorNome}? Depois de enviado ele não pode mais ser editado.`)) return;
    this.executar(p, this.compraService.enviar(p.id), 'Pedido enviado ao fornecedor.');
  }

  cancelar(p: PedidoCompra): void {
    if (!window.confirm(`Cancelar o pedido #${p.id}?`)) return;
    this.executar(p, this.compraService.cancelar(p.id), 'Pedido cancelado.');
  }

  private executar(p: PedidoCompra, acao: ReturnType<CompraService['enviar']>, mensagem: string): void {
    this.acaoId = p.id;
    acao.subscribe({
      next: () => {
        this.acaoId = null;
        this.toast.show(mensagem);
        this.carregar(this.pagina);
      },
      error: error => {
        this.acaoId = null;
        this.toast.show(apiErrorMessage(error, 'Não foi possível concluir a ação.'), 'error');
        this.carregar(this.pagina);
      }
    });
  }

  // ── Novo / editar ───────────────────────────────────────────

  abrirNovo(fornecedorId: number | null = null, ingredienteId: number | null = null): void {
    this.editandoId = null;
    this.erroFormulario = '';
    this.fornecedorId = null;
    this.catalogo = [];
    this.linhas = [];
    this.modalAberto = true;
    this.carregarFornecedores(() => {
      if (fornecedorId !== null && this.fornecedoresAtivos.some(f => f.id === fornecedorId)) {
        this.fornecedorId = fornecedorId;
        this.carregarCatalogo(fornecedorId, [{ ingredienteId, quantidade: null, precoUnitario: null }]);
      }
    });
  }

  abrirEdicao(p: PedidoCompra): void {
    this.editandoId = p.id;
    this.erroFormulario = '';
    this.fornecedorId = p.fornecedorId;
    this.catalogo = [];
    this.linhas = [];
    this.modalAberto = true;
    this.carregarFornecedores();
    this.carregarCatalogo(p.fornecedorId, p.itens.map(i => ({
      ingredienteId: i.ingredienteId,
      quantidade: i.quantidade,
      precoUnitario: i.precoUnitario,
    })));
  }

  fecharModal(): void {
    if (this.salvando) return;
    this.modalAberto = false;
  }

  fornecedorMudou(): void {
    this.catalogo = [];
    this.linhas = [];
    if (this.fornecedorId !== null) this.carregarCatalogo(this.fornecedorId);
  }

  adicionarLinha(): void {
    this.linhas.push({ ingredienteId: null, quantidade: null, precoUnitario: null });
  }

  removerLinha(i: number): void {
    this.linhas.splice(i, 1);
    if (!this.linhas.length) this.adicionarLinha();
  }

  jaEscolhido(ingredienteId: number, indice: number): boolean {
    return this.linhas.some((l, i) => i !== indice && l.ingredienteId === ingredienteId);
  }

  unidadeDe(ingredienteId: number | null): string {
    return this.itemDoCatalogo(ingredienteId)?.unidadeMedida ?? '';
  }

  placeholderPreco(ingredienteId: number | null): string {
    const item = this.itemDoCatalogo(ingredienteId);
    if (!item || item.precoPorUnidadeIngrediente === null) return 'Catálogo';
    return item.precoPorUnidadeIngrediente.toFixed(4).replace('.', ',');
  }

  subtotal(linha: LinhaForm): number {
    const qtd = Number(linha.quantidade);
    const preco = this.precoDaLinha(linha);
    return Number.isFinite(qtd) && qtd > 0 && preco !== null ? Math.round(qtd * preco * 100) / 100 : 0;
  }

  totalEstimado(): number {
    return this.linhas.reduce((soma, l) => soma + this.subtotal(l), 0);
  }

  salvar(): void {
    if (this.fornecedorId === null) { this.erroFormulario = 'Escolha o fornecedor.'; return; }
    const preenchidas = this.linhas.filter(l => l.ingredienteId !== null);
    if (!preenchidas.length) { this.erroFormulario = 'Adicione pelo menos um item.'; return; }
    for (const l of preenchidas) {
      const qtd = Number(l.quantidade);
      if (!Number.isFinite(qtd) || qtd <= 0) {
        this.erroFormulario = 'Informe a quantidade de todos os itens.';
        return;
      }
      if (l.precoUnitario !== null && String(l.precoUnitario) !== '' && !(Number(l.precoUnitario) > 0)) {
        this.erroFormulario = 'O preço informado deve ser maior que zero.';
        return;
      }
    }

    const payload: PedidoCompraInput = {
      fornecedorId: this.fornecedorId,
      itens: preenchidas.map(l => ({
        ingredienteId: l.ingredienteId!,
        quantidade: Number(l.quantidade),
        precoUnitario: l.precoUnitario !== null && String(l.precoUnitario) !== '' ? Number(l.precoUnitario) : null,
      })),
    };

    this.salvando = true;
    this.erroFormulario = '';
    const novo = this.editandoId === null;
    const request = novo ? this.compraService.criar(payload) : this.compraService.atualizar(this.editandoId!, payload);
    request.subscribe({
      next: pedido => {
        this.salvando = false;
        this.modalAberto = false;
        this.toast.show(novo ? `Rascunho #${pedido.id} criado. Revise e envie ao fornecedor.` : 'Rascunho atualizado.');
        this.aba = 'pedidos';
        this.carregar(novo ? 0 : this.pagina);
      },
      error: error => {
        this.salvando = false;
        this.erroFormulario = apiErrorMessage(error, 'Não foi possível salvar o pedido.');
      }
    });
  }

  private carregarFornecedores(depois?: () => void): void {
    this.fornecedorService.ativos().subscribe({
      next: lista => { this.fornecedoresAtivos = lista; depois?.(); },
      error: error => this.erroFormulario = apiErrorMessage(error, 'Não foi possível carregar os fornecedores.')
    });
  }

  private carregarCatalogo(fornecedorId: number, linhasIniciais?: LinhaForm[]): void {
    this.carregandoCatalogo = true;
    this.fornecedorService.catalogo(fornecedorId).subscribe({
      next: itens => {
        // Ignora a resposta se o usuário já trocou de fornecedor
        if (this.fornecedorId !== fornecedorId) return;
        this.catalogo = itens;
        this.carregandoCatalogo = false;
        this.linhas = linhasIniciais?.length ? linhasIniciais : [];
        if (!this.linhas.length && itens.length) this.adicionarLinha();
      },
      error: error => {
        this.carregandoCatalogo = false;
        this.erroFormulario = apiErrorMessage(error, 'Não foi possível carregar o catálogo do fornecedor.');
      }
    });
  }

  private itemDoCatalogo(ingredienteId: number | null): CatalogoItem | undefined {
    return ingredienteId === null ? undefined : this.catalogo.find(c => c.ingredienteId === ingredienteId);
  }

  private precoDaLinha(linha: LinhaForm): number | null {
    if (linha.precoUnitario !== null && String(linha.precoUnitario) !== '' && Number(linha.precoUnitario) > 0) {
      return Number(linha.precoUnitario);
    }
    return this.itemDoCatalogo(linha.ingredienteId)?.precoPorUnidadeIngrediente ?? null;
  }

  // ── Recebimento ─────────────────────────────────────────────

  abrirRecebimento(p: PedidoCompra): void {
    this.recebendo = p;
    this.lote = '';
    this.validade = '';
    this.erroRecebimento = '';
  }

  fecharRecebimento(): void {
    if (this.confirmandoRecebimento) return;
    this.recebendo = null;
  }

  confirmarRecebimento(): void {
    if (!this.recebendo) return;
    if (this.validade && this.validade < this.hoje) {
      this.erroRecebimento = 'A validade não pode ser anterior a hoje.';
      return;
    }
    this.confirmandoRecebimento = true;
    this.erroRecebimento = '';
    this.compraService.receber(this.recebendo.id, this.lote.trim() || null, this.validade || null).subscribe({
      next: pedido => {
        this.confirmandoRecebimento = false;
        this.recebendo = null;
        this.toast.show(`Pedido #${pedido.id} recebido: estoque e custos atualizados.`);
        this.estoqueService.atualizarAlertas();   // o badge de estoque baixo do menu muda na hora
        this.cotacaoCarregada = false;            // o custo atual mostrado na cotação mudou
        this.carregar(this.pagina);
      },
      error: error => {
        this.confirmandoRecebimento = false;
        this.erroRecebimento = apiErrorMessage(error, 'Não foi possível receber o pedido.');
      }
    });
  }

  // ── Cotação ─────────────────────────────────────────────────

  carregarCotacao(): void {
    this.carregandoCotacao = true;
    this.erroCotacao = '';
    this.compraService.cotacao().subscribe({
      next: lista => {
        this.cotacoes = lista;
        this.carregandoCotacao = false;
        this.cotacaoCarregada = true;
      },
      error: error => {
        this.carregandoCotacao = false;
        this.erroCotacao = apiErrorMessage(error, 'Não foi possível carregar a cotação.');
      }
    });
  }

  cotacoesFiltradas(): Cotacao[] {
    const termo = this.normalizar(this.filtroCotacao);
    return termo ? this.cotacoes.filter(c => this.normalizar(c.ingredienteNome).includes(termo)) : this.cotacoes;
  }

  comprarDaOferta(c: Cotacao, o: Oferta): void {
    this.abrirNovo(o.fornecedorId, c.ingredienteId);
  }

  // yyyy-MM-dd no fuso do navegador (toISOString usaria UTC e viraria o dia às 21h)
  private static dataLocal(d: Date): string {
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mm}-${dd}`;
  }

  private normalizar(texto: string): string {
    return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
  }
}
