import {
  AfterViewInit, Component, ElementRef, OnDestroy, OnInit, NgZone, ViewChild
} from '@angular/core';
import { CurrencyPipe, NgIf } from '@angular/common';
import { Subscription } from 'rxjs';
import { Chart, registerables } from 'chart.js';
import { InsightsResumo, InsightsService } from '../../../services/insights.service';

Chart.register(...registerables);

const AMBER = 'rgba(245,158,11,1)';
const AMBER_BACKGROUND = 'rgba(245,158,11,.15)';
const CHART_GRID = 'rgba(255,255,255,.06)';
const CHART_TEXT = 'rgba(255,255,255,.5)';

@Component({
  selector: 'app-insights-dashboard',
  standalone: true,
  imports: [CurrencyPipe, NgIf],
  template: `
    <div style="max-width:1280px;margin:0 auto;padding:0 1.5rem 2rem">
      <div style="margin-bottom:2rem" class="animate-fade-up">
        <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">
          Insights & Analytics
        </h1>
        <p style="color:var(--text-tertiary);font-size:var(--text-base)">
          Resumo de desempenho fornecido pela API do Maka
        </p>
      </div>

      <div *ngIf="errorMessage" class="card" role="alert"
           style="padding:1.25rem;margin-bottom:1.5rem;color:var(--danger-text);border-color:var(--danger-border)">
        {{ errorMessage }}
      </div>

      <ng-container *ngIf="resumo as dados">
        <div class="grid-3 stagger" style="margin-bottom:2rem">
          <div class="kpi-card">
            <div class="kpi-label">Faturamento Total</div>
            <div class="kpi-value" style="font-size:1.55rem">
              {{ dados.faturamentoTotal | currency:'BRL':'symbol':'1.2-2' }}
            </div>
            <div class="kpi-accent" style="background:var(--brand-600)"></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Total de Pedidos</div>
            <div class="kpi-value">{{ dados.totalPedidos }}</div>
            <div class="kpi-accent" style="background:#6366f1"></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Ticket Médio</div>
            <div class="kpi-value" style="font-size:1.55rem">
              {{ dados.ticketMedio | currency:'BRL':'symbol':'1.2-2' }}
            </div>
            <div class="kpi-accent" style="background:#8b5cf6"></div>
          </div>
        </div>

        <div class="insights-grid">
          <section class="card">
            <div class="card-header">
              <div>
                <h2 style="font-family:var(--font-display);font-size:1.125rem;color:var(--text-primary)">
                  Faturamento por Dia
                </h2>
                <p style="font-size:var(--text-xs);color:var(--text-tertiary);margin-top:.125rem">
                  Valores consolidados pela API
                </p>
              </div>
            </div>
            <div class="card-body" style="padding-top:0">
              <div class="chart-container">
                <canvas #revenueChart aria-label="Gráfico de faturamento por dia"></canvas>
                <div *ngIf="!loading && !dados.faturamentoPorDia.length" class="chart-message">
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
                  Ranking por quantidade
                </p>
              </div>
            </div>
            <div class="card-body" style="padding-top:0">
              <div class="chart-container">
                <canvas #dishesChart aria-label="Gráfico dos pratos mais vendidos"></canvas>
                <div *ngIf="!loading && !dados.pratosMaisVendidos.length" class="chart-message">
                  Ainda não há pratos vendidos para exibir.
                </div>
              </div>
            </div>
          </section>
        </div>
      </ng-container>

      <div *ngIf="loading" class="card" role="status" style="padding:1.25rem;color:var(--text-secondary)">
        Carregando insights...
      </div>
    </div>
  `,
  styles: [`
    .insights-grid { display:grid;grid-template-columns:1.4fr 1fr;gap:1.5rem; }
    .chart-container { position:relative;height:270px; }
    .chart-message {
      position:absolute;inset:0;display:flex;align-items:center;justify-content:center;
      color:var(--text-tertiary);font-size:var(--text-sm);pointer-events:none;
    }
    @media (max-width: 900px) { .insights-grid { grid-template-columns:1fr; } }
  `]
})
export class InsightsDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('revenueChart') revenueCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('dishesChart') dishesCanvas!: ElementRef<HTMLCanvasElement>;

  resumo: InsightsResumo | null = null;
  loading = true;
  errorMessage = '';

  private subscription?: Subscription;
  private charts: Chart[] = [];
  private viewReady = false;

  constructor(private insightsService: InsightsService, private ngZone: NgZone) {}

  ngOnInit(): void {
    this.subscription = this.insightsService.obterResumo().subscribe({
      next: resumo => {
        this.resumo = resumo;
        this.loading = false;
        this.errorMessage = '';
        if (this.viewReady) this.ngZone.runOutsideAngular(() => this.updateCharts());
      },
      error: error => {
        console.error('[Insights] Falha ao carregar resumo da API:', error);
        this.loading = false;
        this.errorMessage = 'Não foi possível carregar os insights. Verifique a conexão com a API e tente novamente.';
      }
    });
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
    const dailyRevenue = this.resumo.faturamentoPorDia;
    revenueChart.data.labels = dailyRevenue.map(item => this.formatDate(item.data));
    revenueChart.data.datasets[0].data = dailyRevenue.map(item => item.faturamento);
    revenueChart.update('none');

    const bestSellers = this.resumo.pratosMaisVendidos;
    dishesChart.data.labels = bestSellers.map(item => item.nomeProduto);
    dishesChart.data.datasets[0].data = bestSellers.map(item => item.quantidadeTotal);
    dishesChart.update('none');
  }

  private formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return year && month && day ? `${day}/${month}` : value;
  }
}
