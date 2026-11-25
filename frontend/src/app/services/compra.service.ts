import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Page } from '../models';

// RF-024: RASCUNHO -> ENVIADO -> RECEBIDO (ou CANCELADO antes de receber)
export type StatusPedidoCompra = 'RASCUNHO' | 'ENVIADO' | 'RECEBIDO' | 'CANCELADO';

export const STATUS_COMPRA: Record<StatusPedidoCompra, { label: string; badge: string }> = {
  RASCUNHO:  { label: 'Rascunho',  badge: 'badge-neutral' },
  ENVIADO:   { label: 'Enviado',   badge: 'badge-blue' },
  RECEBIDO:  { label: 'Recebido',  badge: 'badge-green' },
  CANCELADO: { label: 'Cancelado', badge: 'badge-red' },
};

export interface PedidoCompraItem {
  id: number;
  ingredienteId: number;
  ingredienteNome: string;
  unidadeMedida: string;
  quantidade: number;        // na unidade do ingrediente (a mesma do estoque)
  precoUnitario: number;     // R$ por unidade do ingrediente
  subtotal: number;
}

export interface PedidoCompra {
  id: number;
  fornecedorId: number;
  fornecedorNome: string;
  fornecedorCnpj: string;
  status: StatusPedidoCompra;
  valorTotal: number;
  criadoPor: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  itens: PedidoCompraItem[];
}

export interface PedidoCompraInput {
  fornecedorId: number;
  itens: { ingredienteId: number; quantidade: number; precoUnitario?: number | null }[];
}

// RF-023
export interface Oferta {
  catalogoItemId: number;
  fornecedorId: number;
  fornecedorNome: string;
  preco: number;
  unidadeVenda: string;
  precoPorUnidadeIngrediente: number | null;
  melhorPreco: boolean;
  diferencaPercentual: number | null;
}

export interface Cotacao {
  ingredienteId: number;
  ingredienteNome: string;
  unidadeMedida: string;
  custoAtual: number;
  melhorFornecedorId: number | null;
  ofertas: Oferta[];
}

@Injectable({ providedIn: 'root' })
export class CompraService {
  private readonly endpoint = `${environment.apiUrl}/admin/compras`;

  constructor(private http: HttpClient) {}

  listar(page: number, size = 10, status: StatusPedidoCompra | '' = ''): Observable<Page<PedidoCompra>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<Page<PedidoCompra>>(this.endpoint, { params });
  }

  buscar(id: number): Observable<PedidoCompra> {
    return this.http.get<PedidoCompra>(`${this.endpoint}/${id}`);
  }

  criar(data: PedidoCompraInput): Observable<PedidoCompra> {
    return this.http.post<PedidoCompra>(this.endpoint, data);
  }

  atualizar(id: number, data: PedidoCompraInput): Observable<PedidoCompra> {
    return this.http.put<PedidoCompra>(`${this.endpoint}/${id}`, data);
  }

  enviar(id: number): Observable<PedidoCompra> {
    return this.http.patch<PedidoCompra>(`${this.endpoint}/${id}/enviar`, {});
  }

  receber(id: number, lote: string | null, validade: string | null): Observable<PedidoCompra> {
    return this.http.patch<PedidoCompra>(`${this.endpoint}/${id}/receber`, { lote, validade });
  }

  cancelar(id: number): Observable<PedidoCompra> {
    return this.http.patch<PedidoCompra>(`${this.endpoint}/${id}/cancelar`, {});
  }

  // Sem ids: compara todos os ingredientes que têm oferta
  cotacao(ingredienteIds: number[] = []): Observable<Cotacao[]> {
    let params = new HttpParams();
    if (ingredienteIds.length) params = params.set('ingredienteIds', ingredienteIds.join(','));
    return this.http.get<Cotacao[]>(`${this.endpoint}/cotacao`, { params });
  }
}
