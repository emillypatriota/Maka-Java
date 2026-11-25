import { Component, OnInit } from '@angular/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { EstoqueService, MotivoSaida, MovimentacaoEstoque, SaldoEstoque } from '../../../services/estoque.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-stock-management',
  standalone: true,
  imports: [DatePipe, FormsModule, NgFor, NgIf],
  template: `
    <div style="max-width:1200px;margin:0 auto;padding:0 1.5rem 2rem">
      <header style="margin-bottom:1.5rem">
        <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">
          Estoque
        </h1>
        <p style="color:var(--text-tertiary)">Consulte saldos e registre entradas e saídas manuais.</p>
      </header>

      <div *ngIf="erro" class="card" role="alert"
           style="padding:1rem;margin-bottom:1rem;color:var(--danger-text);border-color:var(--danger-border)">
        {{ erro }}
        <button class="btn btn-ghost btn-sm" type="button" (click)="carregar()">Tentar novamente</button>
      </div>

      <section class="card" style="padding:1.25rem;margin-bottom:1.5rem">
        <h2 style="margin:0 0 1rem;color:var(--text-primary)">Nova movimentação</h2>
        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:1rem;align-items:end">
          <label class="form-group">
            <span class="form-label">Tipo</span>
            <select class="form-input" [(ngModel)]="tipo">
              <option value="ENTRADA">Entrada manual</option>
              <option value="SAIDA">Saída manual</option>
            </select>
          </label>
          <label class="form-group">
            <span class="form-label">Ingrediente</span>
            <select class="form-input" [(ngModel)]="ingredienteId">
              <option [ngValue]="null">Selecione</option>
              <option *ngFor="let ingrediente of saldos" [ngValue]="ingrediente.ingredienteId">
                {{ ingrediente.nome }} ({{ ingrediente.unidadeMedida }})
              </option>
            </select>
          </label>
          <label class="form-group">
            <span class="form-label">Quantidade</span>
            <input class="form-input" type="number" [(ngModel)]="quantidade" min="0.001" step="0.001">
          </label>
          <label *ngIf="tipo === 'SAIDA'" class="form-group">
            <span class="form-label">Motivo da saída</span>
            <select class="form-input" [(ngModel)]="motivo">
              <option value="DESPERDICIO">Desperdício</option>
              <option value="VENCIMENTO">Vencimento</option>
              <option value="QUEBRA">Quebra</option>
              <option value="USO_INTERNO">Uso interno</option>
            </select>
          </label>
          <button class="btn btn-primary" type="button" [disabled]="salvando || carregando" (click)="registrar()">
            {{ salvando ? 'Registrando...' : 'Registrar movimentação' }}
          </button>
        </div>
      </section>

      <section class="card" style="margin-bottom:1.5rem">
        <header style="display:flex;justify-content:space-between;align-items:center;padding:1rem;gap:1rem">
          <h2 style="margin:0;color:var(--text-primary)">Saldos atuais</h2>
          <span style="color:var(--text-secondary);font-size:var(--text-sm)">
            {{ resumoAlertas }} ingrediente(s) no mínimo ou abaixo
          </span>
        </header>
        <div *ngIf="carregando" role="status" style="padding:1rem;color:var(--text-secondary)">Carregando estoque...</div>
        <div *ngIf="!carregando && saldos.length" style="overflow-x:auto">
          <table style="width:100%;border-collapse:collapse;text-align:left">
            <thead>
              <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                <th style="padding:1rem">Ingrediente</th>
                <th style="padding:1rem">SKU</th>
                <th style="padding:1rem">Saldo</th>
                <th style="padding:1rem">Mínimo</th>
                <th style="padding:1rem">Situação</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of saldos" style="border-top:1px solid var(--border);color:var(--text-primary)">
                <td style="padding:1rem;font-weight:600">{{ item.nome }}</td>
                <td style="padding:1rem;color:var(--text-secondary)">{{ item.sku || '—' }}</td>
                <td style="padding:1rem">{{ item.saldo }} {{ item.unidadeMedida }}</td>
                <td style="padding:1rem;color:var(--text-secondary)">{{ item.estoqueMinimo }} {{ item.unidadeMedida }}</td>
                <td style="padding:1rem">
                  <span [style.color]="item.abaixoDoMinimo ? 'var(--danger-text)' : 'var(--success-text)'">
                    {{ item.abaixoDoMinimo ? 'Estoque mínimo' : 'Normal' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p *ngIf="!carregando && !saldos.length" class="empty-state" style="padding:1.5rem">
          Nenhum ingrediente ativo cadastrado.
        </p>
      </section>

      <section class="card">
        <header style="padding:1rem">
          <h2 style="margin:0;color:var(--text-primary)">Movimentações recentes</h2>
        </header>
        <div *ngIf="!movimentacoes.length" style="padding:1rem;color:var(--text-secondary)">
          Nenhuma movimentação registrada.
        </div>
        <div *ngIf="movimentacoes.length" style="overflow-x:auto">
          <table style="width:100%;border-collapse:collapse;text-align:left">
            <thead>
              <tr style="color:var(--text-tertiary);font-size:var(--text-xs);text-transform:uppercase">
                <th style="padding:1rem">Data</th>
                <th style="padding:1rem">Ingrediente</th>
                <th style="padding:1rem">Tipo</th>
                <th style="padding:1rem">Motivo</th>
                <th style="padding:1rem">Quantidade</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let movimento of movimentacoes" style="border-top:1px solid var(--border);color:var(--text-primary)">
                <td style="padding:1rem;color:var(--text-secondary)">
                  {{ movimento.createdAt ? (movimento.createdAt | date:'dd/MM/yyyy HH:mm') : 'Agora' }}
                </td>
                <td style="padding:1rem;font-weight:600">{{ movimento.ingredienteNome }}</td>
                <td style="padding:1rem">{{ movimento.tipo === 'ENTRADA' ? 'Entrada' : 'Saída' }}</td>
                <td style="padding:1rem;color:var(--text-secondary)">{{ rotuloMotivo(movimento.motivo) }}</td>
                <td style="padding:1rem">{{ movimento.quantidade }} {{ movimento.unidadeMedida }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `
})
export class StockManagementComponent implements OnInit {
  saldos: SaldoEstoque[] = [];
  movimentacoes: MovimentacaoEstoque[] = [];
  resumoAlertas = 0;
  carregando = false;
  salvando = false;
  erro = '';
  tipo: 'ENTRADA' | 'SAIDA' = 'ENTRADA';
  ingredienteId: number | null = null;
  quantidade: number | null = null;
  motivo: MotivoSaida = 'DESPERDICIO';

  constructor(
    private estoqueService: EstoqueService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = '';
    this.estoqueService.carregarSaldos().subscribe({
      next: resumo => {
        this.saldos = resumo.itens;
        this.resumoAlertas = resumo.alertas;
        this.estoqueService.alertas.set(resumo.alertas);
        this.carregando = false;
      },
      error: error => {
        console.error('[Estoque] Falha ao carregar saldos:', error);
        this.erro = 'Não foi possível carregar os saldos de estoque.';
        this.carregando = false;
      }
    });
    this.estoqueService.listarMovimentacoes().subscribe({
      next: pagina => this.movimentacoes = pagina.content,
      error: error => {
        console.error('[Estoque] Falha ao carregar movimentações:', error);
        this.erro = 'Não foi possível carregar o histórico de movimentações.';
      }
    });
  }

  registrar(): void {
    if (this.ingredienteId === null || this.quantidade === null
        || !Number.isFinite(this.quantidade) || this.quantidade <= 0) {
      this.toast.show('Selecione um ingrediente e informe uma quantidade maior que zero.', 'error');
      return;
    }

    this.salvando = true;
    const request = this.tipo === 'ENTRADA'
      ? this.estoqueService.entrada(this.ingredienteId, this.quantidade)
      : this.estoqueService.saida(this.ingredienteId, this.quantidade, this.motivo);
    request.subscribe({
      next: () => {
        this.toast.show(this.tipo === 'ENTRADA' ? 'Entrada registrada.' : 'Saída registrada.');
        this.quantidade = null;
        this.salvando = false;
        this.carregar();
        this.estoqueService.atualizarAlertas();
      },
      error: error => {
        console.error('[Estoque] Falha ao registrar movimentação:', error);
        this.toast.show(this.mensagemErro(error), 'error');
        this.salvando = false;
      }
    });
  }

  rotuloMotivo(motivo: string): string {
    const rotulos: Record<string, string> = {
      AJUSTE: 'Ajuste manual',
      DESPERDICIO: 'Desperdício',
      VENCIMENTO: 'Vencimento',
      QUEBRA: 'Quebra',
      USO_INTERNO: 'Uso interno',
      COMPRA: 'Compra',
      VENDA: 'Venda',
      ESTORNO: 'Estorno'
    };
    return rotulos[motivo] ?? motivo;
  }

  private mensagemErro(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 422) {
      return error.error?.message ?? 'A movimentação viola uma regra de estoque.';
    }
    if (error instanceof HttpErrorResponse && error.status === 404) return 'Ingrediente não encontrado.';
    if (error instanceof HttpErrorResponse && error.status === 403) return 'Apenas gerentes podem movimentar estoque.';
    return 'Não foi possível registrar a movimentação.';
  }
}
