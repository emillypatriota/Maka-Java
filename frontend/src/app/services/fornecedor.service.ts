import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Page } from '../models';

export type StatusFornecedor = 'ATIVO' | 'INATIVO';

// RF-021
export interface Fornecedor {
  id: number;
  razaoSocial: string;
  cnpj: string;              // sempre formatado: 11.222.333/0001-81
  telefone: string | null;
  email: string | null;
  categorias: string | null;
  status: StatusFornecedor;
  createdAt: string | null;
}

export interface FornecedorInput {
  razaoSocial: string;
  cnpj: string;
  telefone: string | null;
  email: string | null;
  categorias: string | null;
  status: StatusFornecedor;
}

// RF-022: preço por unidade de venda + o mesmo preço na unidade do ingrediente
export interface CatalogoItem {
  id: number;
  fornecedorId: number;
  ingredienteId: number;
  ingredienteNome: string;
  unidadeMedida: string;
  preco: number;
  unidadeVenda: string;
  precoPorUnidadeIngrediente: number | null;
}

export interface CatalogoItemInput {
  ingredienteId: number;
  preco: number;
  unidadeVenda: string;
}

@Injectable({ providedIn: 'root' })
export class FornecedorService {
  private readonly endpoint = `${environment.apiUrl}/admin/fornecedores`;

  constructor(private http: HttpClient) {}

  listar(page: number, size = 10, busca = '', status: StatusFornecedor | '' = ''): Observable<Page<Fornecedor>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (busca.trim()) params = params.set('busca', busca.trim());
    if (status) params = params.set('status', status);
    return this.http.get<Page<Fornecedor>>(this.endpoint, { params });
  }

  ativos(): Observable<Fornecedor[]> {
    return this.http.get<Fornecedor[]>(`${this.endpoint}/ativos`);
  }

  criar(data: FornecedorInput): Observable<Fornecedor> {
    return this.http.post<Fornecedor>(this.endpoint, data);
  }

  atualizar(id: number, data: FornecedorInput): Observable<Fornecedor> {
    return this.http.put<Fornecedor>(`${this.endpoint}/${id}`, data);
  }

  alterarStatus(id: number, status: StatusFornecedor): Observable<Fornecedor> {
    return this.http.patch<Fornecedor>(`${this.endpoint}/${id}/status`, { status });
  }

  catalogo(fornecedorId: number): Observable<CatalogoItem[]> {
    return this.http.get<CatalogoItem[]>(`${this.endpoint}/${fornecedorId}/catalogo`);
  }

  adicionarItem(fornecedorId: number, data: CatalogoItemInput): Observable<CatalogoItem> {
    return this.http.post<CatalogoItem>(`${this.endpoint}/${fornecedorId}/catalogo`, data);
  }

  atualizarItem(fornecedorId: number, itemId: number, data: CatalogoItemInput): Observable<CatalogoItem> {
    return this.http.put<CatalogoItem>(`${this.endpoint}/${fornecedorId}/catalogo/${itemId}`, data);
  }

  removerItem(fornecedorId: number, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${fornecedorId}/catalogo/${itemId}`);
  }
}
