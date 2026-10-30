import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Ingrediente {
  id: number;
  nome: string;
  unidadeMedida: string;
  custoUnitario: number;
}

export interface IngredientePage {
  content: Ingrediente[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type IngredienteInput = Omit<Ingrediente, 'id'>;

@Injectable({ providedIn: 'root' })
export class IngredienteService {
  private readonly endpoint = `${environment.apiUrl}/admin/ingredientes`;

  constructor(private http: HttpClient) {}

  listar(page: number, size = 10): Observable<IngredientePage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<IngredientePage>(this.endpoint, { params });
  }

  criar(ingrediente: IngredienteInput): Observable<Ingrediente> {
    return this.http.post<Ingrediente>(this.endpoint, ingrediente);
  }

  atualizar(id: number, ingrediente: IngredienteInput): Observable<Ingrediente> {
    return this.http.put<Ingrediente>(`${this.endpoint}/${id}`, ingrediente);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }
}
