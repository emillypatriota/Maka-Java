import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface InsightsResumo {
  faturamentoTotal: number;
  ticketMedio: number;
  totalPedidos: number;
  faturamentoPorDia: { data: string; faturamento: number }[];
  pratosMaisVendidos: { nomeProduto: string; quantidadeTotal: number }[];
}

export interface DashboardResumo {
  faturamentoHoje: number;
  pedidosHoje: number;
  ticketMedioHoje: number;
  foodCostMedio: number;
  alertasEstoque: number;
  estoqueCritico: {
    ingredienteId: number;
    nome: string;
    unidadeMedida: string;
    estoqueMinimo: number;
    saldo: number;
    abaixoDoMinimo: boolean;
  }[];
  vendasPorDia: { data: string; faturamento: number }[];
}

export interface TopPrato {
  nomeProduto: string;
  quantidadeTotal: number;
}

@Injectable({ providedIn: 'root' })
export class InsightsService {
  constructor(private http: HttpClient) {}

  obterResumo(): Observable<InsightsResumo> {
    return this.http.get<InsightsResumo>(`${environment.apiUrl}/insights/resumo`);
  }

  obterDashboardResumo(dataInicio: string, dataFim: string): Observable<DashboardResumo> {
    const params = { dataInicio, dataFim };
    return this.http.get<DashboardResumo>(`${environment.apiUrl}/admin/dashboard/resumo`, { params });
  }

  obterTopPratos(dataInicio: string, dataFim: string): Observable<TopPrato[]> {
    const params = { dataInicio, dataFim };
    return this.http.get<TopPrato[]>(`${environment.apiUrl}/admin/dashboard/top-pratos`, { params });
  }
}
