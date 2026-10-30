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

@Injectable({ providedIn: 'root' })
export class InsightsService {
  constructor(private http: HttpClient) {}

  obterResumo(): Observable<InsightsResumo> {
    return this.http.get<InsightsResumo>(`${environment.apiUrl}/insights/resumo`);
  }
}
