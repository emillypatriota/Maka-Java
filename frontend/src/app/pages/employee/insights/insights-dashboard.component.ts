import {
  AfterViewInit, Component, ElementRef, OnDestroy, OnInit, NgZone, ViewChild
} from '@angular/core';
import { CurrencyPipe, DecimalPipe, NgFor, NgIf } from '@angular/common';
import { Subscription } from 'rxjs';
import { forkJoin } from 'rxjs';
import { Chart, registerables } from 'chart.js';
import { DashboardResumo, InsightsService, TopPrato } from '../../../services/insights.service';

Chart.register(...registerables);

const AMBER = 'rgba(245,158,11,1)';
const AMBER_BACKGROUND = 'rgba(245,158,11,.15)';
const CHART_GRID = 'rgba(255,255,255,.06)';
const CHART_TEXT = 'rgba(255,255,255,.5)';

@Component({
  selector: 'app-insights-dashboard',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, NgFor, NgIf],
  template: `
    <div style="max-width:1280px;margin:0 auto;padding:0 1.5rem 2rem">
      <div class="insights-heading animate-fade-up">
        <div>
          <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">
            Insights & Analytics
          </h1>
          <p style="color:var(--text-tertiary);font-size:var(--text-base)">
            Desempenho de vendas e operação
          </p>
        </div>
        <label class="period-filter">
          Período
          <select [value]="periodoDias" (change)="alterarPeriodo($event)">
            <option value="7">Últimos 7 dias</option>
            <option value="30">Últimos 30 dias</option>
            <option value="90">Últimos 90 dias</option>
          </select>
        </label>
      </div>

      <div *ngIf="errorMessage" class="card" role="alert"
           style="padding:1.25rem;margin-bottom:1.5rem;color:var(--danger-text);border-color:var(--danger-border)">
        {{ errorMessage }}
      </div>

      <ng-container *ngIf="resumo as dados">
        <div class="insights-kpis stagger" style="margin-bottom:2rem">
          <div class="kpi-card">
            <div class="kpi-label">Faturamento de Hoje</div>
            <div class="kpi-value" style="font-size:1.55rem">
              {{ dados.faturamentoHoje | currency:'BRL':'symbol':'1.2-2' }}
            </div>
            <div class="kpi-accent" style="background:var(--brand-600)"></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Pedidos de Hoje</div>
            <div class="kpi-value">{{ dados.pedidosHoje }}</div>
            <div class="kpi-accent" style="background:#6366f1"></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Ticket Médio de Hoje</div>
            <div class="kpi-value" style="font-size:1.55rem">
              {{ dados.ticketMedioHoje | currency:'BRL':'symbol':'1.2-2' }}
            </div>
            <div class="kpi-accent" style="background:#8b5cf6"></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Food Cost Médio</div>
            <div class="kpi-value">{{ dados.foodCostMedio | number:'1.1-2' }}%</div>
            <div class="kpi-accent" style="background:#10b981"></div>
          </div>
        </div>

        <div class="insights-grid">
          <section class="card">
            <div class="card-header">
              <div>
                <h2 style="font-family:var(--font-display);font-size:1.125rem;color:var(--text-primary)">
                  Vendas por Período
                </h2>
                <p style="font-size:var(--text-xs);color:var(--text-tertiary);margin-top:.125rem">
                  Faturamento diário no intervalo selecionado
                </p>
              </div>
            </div>
            <div class="card-body" style="padding-top:0">
              <div class="chart-container">
                <canvas #revenueChart aria-label="Gráfico de faturamento por dia"></canvas>
                <div *ngIf="!loading && !dados.vendasPorDia.length" class="chart-message">
                  Sem faturamento registrado no período.
                </div>
              </div>
            </div>
          </section>

          <section class="card">
            <div class="card-header">
              <div>
                <h2 style="font-family:var(--font-display);font-size:1.125rem;color:var(--text-primary)">
                  Pratos Mais Vendidos
                </h2>
                <p style="font-size:var(--text-xs);color:var(--text-tertiary);margin-top:.125rem">
                  Top 5 no período selecionado
                </p>
              </div>
            </div>
            <div class="card-body" style="padding-top:0">
              <div class="chart-container">
                <canvas #dishesChart aria-label="Gráfico dos pratos mais vendidos"></canvas>
                <div *ngIf="!loading && !topPratos.length" class="chart-message">
                  Ainda não há pratos vendidos para exibir.
                </div>
              </div>
            </div>
          </section>
        </div>

        <section class="card stock-alerts" *ngIf="dados.estoqueCritico.length">
          <div class="card-header">
            <div>
              <h2 style="font-family:var(--font-display);font-size:1.125rem;color:var(--text-primary)">
                Alertas de Estoque ({{ dados.alertasEstoque }})
              </h2>
              <p style="font-size:var(--text-xs);color:var(--text-tertiary);margin-top:.125rem">
                Ingredientes no mínimo ou abaixo do saldo mínimo
              </p>
            </div>
          </div>
          <ul class="stock-alert-list">
            <li *ngFor="let item of dados.estoqueCritico">
              <span>{{ item.nome }}</span>
              <strong>{{ item.saldo | number:'1.0-3' }} / {{ item.estoqueMinimo | number:'1.0-3' }} {{ item.unidadeMedida }}</strong>
            </li>
          </ul>
        </section>
        <div *ngIf="!dados.alertasEstoque" class="stock-ok" role="status">
          Estoque sem alertas no momento.
        </div>
      </ng-container>

      <div *ngIf="loading" class="card" role="status" style="padding:1.25rem;color:var(--text-secondary)">
        Carregando insights...
      </div>
    </div>
  `,
  styles: [`
    .insights-heading { display:flex;justify-content:space-between;align-items:flex-end;gap:1rem;margin-bottom:2rem; }
    .period-filter { display:flex;align-items:center;gap:.625rem;color:var(--text-secondary);font-size:var(--text-sm);white-space:nowrap; }
    .period-filter select { padding:.55rem .75rem;border-radius:.5rem;border:1px solid var(--border);background:var(--surface-1);color:var(--text-primary); }
    .insights-kpis { display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:1rem; }
    .insights-grid { display:grid;grid-template-columns:1.4fr 1fr;gap:1.5rem; }
    .chart-container { position:relative;height:270px; }
    .stock-alerts { margin-top:1.5rem; }
    .stock-alert-list { display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.5rem 1.5rem;list-style:none;padding:0 1.25rem 1.25rem;margin:0; }
    .stock-alert-list li { display:flex;justify-content:space-between;gap:1rem;color:var(--text-secondary);font-size:var(--text-sm); }
    .stock-alert-list strong { color:var(--danger-text);white-space:nowrap; }
    .stock-ok { margin-top:1.5rem;color:var(--text-tertiary);font-size:var(--text-sm); }
    .chart-message {
      position:absolute;inset:0;display:flex;align-items:center;justify-content:center;
      color:var(--text-tertiary);font-size:var(--text-sm);pointer-events:none;
    }
    @media (max-width: 900px) {
      .insights-grid { grid-template-columns:1fr; }
      .insights-kpis { grid-template-columns:repeat(2,minmax(0,1fr)); }
    }
    @media (max-width: 560px) {
      .insights-heading { align-items:flex-start;flex-direction:column; }
      .insights-kpis { grid-template-columns:1fr; }
      .stock-alert-list { grid-template-columns:1fr; }
    }
  `]
})
export class InsightsDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('revenueChart') revenueCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('dishesChart') dishesCanvas!: ElementRef<HTMLCanvasElement>;

  resumo: DashboardResumo | null = null;
  topPratos: TopPrato[] = [];
  loading = true;
  errorMessage = '';
  periodoDias = 7;

  private subscription?: Subscription;
  private charts: Chart[] = [];
  private viewReady = false;

  constructor(private insightsService: InsightsService, private ngZone: NgZone) {}

  ngOnInit(): void {
    this.carregarDados();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.ngZone.runOutsideAngular(() => {
      this.createCharts();
      this.updateCharts();
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.ngZone.runOutsideAngular(() => this.charts.forEach(chart => chart.destroy()));
  }

  alterarPeriodo(event: Event): void {
    const dias = Number((event.target as HTMLSelectElement).value);
    if (![7, 30, 90].includes(dias)) return;
    this.periodoDias = dias;
    this.carregarDados();
  }

  private carregarDados(): void {
    this.subscription?.unsubscribe();
    this.loading = true;
    this.errorMessage = '';

    const fim = new Date();
    const inicio = new Date(fim);
    inicio.setDate(inicio.getDate() - this.periodoDias + 1);
    const dataInicio = this.formatApiDate(inicio);
    const dataFim = this.formatApiDate(fim);

    this.subscription = forkJoin({
      resumo: this.insightsService.obterDashboardResumo(dataInicio, dataFim),
      topPratos: this.insightsService.obterTopPratos(dataInicio, dataFim)
    }).subscribe({
      next: ({ resumo, topPratos }) => {
        this.resumo = resumo;
        this.topPratos = topPratos;
        this.loading = false;
        if (this.viewReady) this.ngZone.runOutsideAngular(() => this.updateCharts());
      },
      error: error => {
        console.error('[Insights] Falha ao carregar dados do dashboard:', error);
        this.loading = false;
        this.errorMessage = 'Não foi possível carregar os insights. Verifique a conexão com a API e tente novamente.';
      }
    });
  }

  private createCharts(): void {
    this.charts = [
      new Chart<'line', number[], string>(this.revenueCanvas.nativeElement, {
        type: 'line',
        data: {
          labels: [],
          datasets: [{
            data: [],
            borderColor: AMBER,
            backgroundColor: AMBER_BACKGROUND,
            borderWidth: 2,
            fill: true,
            tension: .35,
            pointRadius: 3
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: {
            x: { grid: { color: CHART_GRID }, ticks: { color: CHART_TEXT } },
            y: {
              beginAtZero: true,
              grid: { color: CHART_GRID },
              ticks: { color: CHART_TEXT, callback: value => `R$ ${value}` }
            }
          }
        }
      }),
      new Chart<'bar', number[], string>(this.dishesCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels: [],
          datasets: [{
            data: [],
            backgroundColor: AMBER_BACKGROUND,
            borderColor: AMBER,
            borderWidth: 1.5,
            borderRadius: 6
          }]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: {
            x: { beginAtZero: true, grid: { color: CHART_GRID }, ticks: { color: CHART_TEXT, precision: 0 } },
            y: { grid: { display: false }, ticks: { color: CHART_TEXT } }
          }
        }
      })
    ];
  }

  private updateCharts(): void {
    if (!this.resumo || this.charts.length !== 2) return;

    const [revenueChart, dishesChart] = this.charts;
    const dailyRevenue = this.resumo.vendasPorDia;
    revenueChart.data.labels = dailyRevenue.map(item => this.formatDate(item.data));
    revenueChart.data.datasets[0].data = dailyRevenue.map(item => item.faturamento);
    revenueChart.update('none');

    dishesChart.data.labels = this.topPratos.map(item => item.nomeProduto);
    dishesChart.data.datasets[0].data = this.topPratos.map(item => item.quantidadeTotal);
    dishesChart.update('none');
  }

  private formatApiDate(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return year && month && day ? `${day}/${month}` : value;
  }
}
