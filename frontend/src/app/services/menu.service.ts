import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { MenuItem } from '../models';

export const CATEGORIES = ['Carnes Nobres', 'Entradas', 'Peixes & Frutos do Mar', 'Acompanhamentos', 'Sobremesas', 'Bebidas'];

// Mensagem mostrada no toast enquanto a escrita não existe no backend
const NOT_AVAILABLE = 'Disponível na próxima versão';

@Injectable({ providedIn: 'root' })
export class MenuService {
  private _items  = signal<MenuItem[]>([]);
  private _loaded = signal(false);

  readonly items$  = this._items.asReadonly();
  readonly loaded$ = this._loaded.asReadonly();

  constructor(private http: HttpClient) { this.loadMenu(); }

  private loadMenu(): void {
    this.http.get<MenuItem[]>(`${environment.apiUrl}/cardapio`).subscribe({
      next: items => {
        this._items.set(items);
        this._loaded.set(true);
      },
      error: () => {
        this._items.set([]);
        this._loaded.set(true); // para o skeleton mesmo com erro
      },
    });
  }

  getAll(): MenuItem[] { return this._items(); }
  getCategories(): string[] { return CATEGORIES; }
  getByCategory(cat: string): MenuItem[] { return this._items().filter(i => i.category === cat); }
  getSpecials(): MenuItem[] { return this._items().filter(i => i.isSpecial).slice(0, 3); }

  // TODO (Sprint 2 – Gestão): POST /api/cardapio e recarregar a lista
  async addItem(item: Omit<MenuItem, 'id'>): Promise<void> {
    throw new Error(NOT_AVAILABLE);
  }
  // TODO (Sprint 2 – Gestão): PUT /api/cardapio/{id} e recarregar a lista
  async updateItem(id: string, data: Partial<MenuItem>): Promise<void> {
    throw new Error(NOT_AVAILABLE);
  }
  // TODO (Sprint 2 – Gestão): DELETE /api/cardapio/{id} e recarregar a lista
  async deleteItem(id: string): Promise<void> {
    throw new Error(NOT_AVAILABLE);
  }
  // TODO (Sprint 2 – Gestão): PATCH /api/cardapio/{id}/ativo e recarregar a lista
  async toggleItem(id: string, active: boolean): Promise<void> {
    throw new Error(NOT_AVAILABLE);
  }
}
