import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SaldoEstoque {
  ingredienteId: number;
  sku: string | null;
  nome: string;
  unidadeMedida: string;
  estoqueMinimo: number;
  saldo: number;
  abaixoDoMinimo: boolean;
}

export interface SaldoEstoqueResumo {
  itens: SaldoEstoque[];
  alertas: number;
}

export type MotivoSaida = 'DESPERDICIO' | 'VENCIMENTO' | 'QUEBRA' | 'USO_INTERNO';
export type TipoMovimentacao = 'ENTRADA' | 'SAIDA' | 'ESTORNO';

export interface MovimentacaoEstoque {
  id: number;
  ingredienteId: number;
  ingredienteNome: string;
  unidadeMedida: string;
  tipo: TipoMovimentacao;
  motivo: string;
  quantidade: number;
  createdAt: string | null;
}

export interface MovimentacaoPage {
  content: MovimentacaoEstoque[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class EstoqueService {
  private readonly endpoint = `${environment.apiUrl}/admin/estoque`;
  readonly alertas = signal(0);

  constructor(private http: HttpClient) {}

  carregarSaldos(): Observable<SaldoEstoqueResumo> {
    return this.http.get<SaldoEstoqueResumo>(`${this.endpoint}/saldos`);
  }

  atualizarAlertas(): void {
    this.carregarSaldos().subscribe({
      next: resumo => this.alertas.set(resumo.alertas),
      error: error => console.error('[Estoque] Falha ao atualizar alertas:', error)
    });
  }

  listarMovimentacoes(page = 0, size = 20): Observable<MovimentacaoPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<MovimentacaoPage>(`${this.endpoint}/movimentacoes`, { params });
  }

  entrada(ingredienteId: number, quantidade: number): Observable<MovimentacaoEstoque> {
    return this.http.post<MovimentacaoEstoque>(`${this.endpoint}/entradas`, { ingredienteId, quantidade });
  }

  saida(ingredienteId: number, quantidade: number, motivo: MotivoSaida): Observable<MovimentacaoEstoque> {
    return this.http.post<MovimentacaoEstoque>(`${this.endpoint}/saidas`, { ingredienteId, quantidade, motivo });
  }
}
