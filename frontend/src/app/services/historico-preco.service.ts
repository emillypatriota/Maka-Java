import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PontoPreco {
  fornecedorId: number;
  fornecedor: string;
  preco: number;
  registradoEm: string;
}

@Injectable({ providedIn: 'root' })
export class HistoricoPrecoService {
  private readonly endpoint = `${environment.apiUrl}/admin/ingredientes`;

  constructor(private http: HttpClient) {}

  listar(ingredienteId: number, fornecedorId?: number): Observable<PontoPreco[]> {
    let params = new HttpParams();
    if (fornecedorId != null) {
      params = params.set('fornecedorId', fornecedorId);
    }
    return this.http.get<PontoPreco[]>(`${this.endpoint}/${ingredienteId}/historico-preco`, { params });
  }
}
