import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, ViewChild } from '@angular/core';
import { NgIf } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { HistoricoPrecoService, PontoPreco } from '../../../services/historico-preco.service';

Chart.register(...registerables);

const CORES = ['#c2410c', '#0f766e', '#4338ca', '#a16207', '#be185d'];

@Component({
  selector: 'app-price-history',
  standalone: true,
  imports: [NgIf, RouterLink],
  template: `
    <div style="max-width:1100px;margin:0 auto;padding:0 1.5rem 2rem">
      <header style="margin-bottom:1.5rem" class="animate-fade-up">
        <a routerLink="/gerente/ingredientes" class="btn btn-ghost btn-sm">← Ingredientes</a>
        <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin:.75rem 0 .25rem">
          Histórico de preços
        </h1>
        <p style="color:var(--text-tertiary);font-size:var(--text-base)">
          Evolução do preço de compra por fornecedor.
        </p>
      </header>

      <div *ngIf="erro" class="card" role="alert" style="padding:1rem;color:var(--danger-text);border-color:var(--danger-border)">
        {{ erro }}
      </div>
      <p *ngIf="carregando" style="color:var(--text-tertiary)">Carregando...</p>
      <p *ngIf="!carregando && !erro && vazio" style="color:var(--text-tertiary)">
        Ainda não há preços registrados para este ingrediente.
      </p>

      <div class="card" style="padding:1rem" [style.display]="vazio || erro ? 'none' : 'block'">
        <div style="position:relative;height:360px">
          <canvas #canvas aria-label="Gráfico de evolução de preços por fornecedor" role="img"></canvas>
        </div>
      </div>
    </div>
  `,
})
export class PriceHistoryComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas') canvas!: ElementRef<HTMLCanvasElement>;

  carregando = true;
  erro = '';
  vazio = false;
  private chart?: Chart;

  constructor(
    private route: ActivatedRoute,
    private service: HistoricoPrecoService,
    private zone: NgZone,
  ) {}

  ngAfterViewInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.service.listar(id).subscribe({
      next: pontos => {
        this.carregando = false;
        this.vazio = pontos.length === 0;
        if (!this.vazio) this.desenhar(pontos);
      },
      error: () => {
        this.carregando = false;
        this.erro = 'Não foi possível carregar o histórico de preços.';
      },
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private desenhar(pontos: PontoPreco[]): void {
    const porFornecedor = new Map<number, PontoPreco[]>();
    pontos.forEach(p => {
      const lista = porFornecedor.get(p.fornecedorId) ?? [];
      lista.push(p);
      porFornecedor.set(p.fornecedorId, lista);
    });

    const datasets = [...porFornecedor.values()].map((lista, i) => ({
      label: lista[0].fornecedor,
      data: lista.map(p => ({ x: new Date(p.registradoEm).getTime(), y: p.preco })),
      borderColor: CORES[i % CORES.length],
      backgroundColor: CORES[i % CORES.length],
      tension: 0.25,
    }));

    this.zone.runOutsideAngular(() => {
      this.chart = new Chart(this.canvas.nativeElement, {
        type: 'line',
        data: { datasets },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          parsing: false,
          scales: {
            x: {
              type: 'linear',
              ticks: { callback: v => new Date(Number(v)).toLocaleDateString('pt-BR') },
            },
            y: {
              title: { display: true, text: 'Preço (R$)' },
              ticks: { callback: v => 'R$ ' + Number(v).toFixed(2) },
            },
          },
          plugins: {
            tooltip: {
              callbacks: {
                title: items => new Date(items[0].parsed.x as number).toLocaleDateString('pt-BR'),
                label: item => `${item.dataset.label}: R$ ${Number(item.parsed.y).toFixed(2)}`,
              },
            },
          },
        },
      });
    });
  }
}
