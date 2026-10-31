import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { errorMessage } from '../core/http-error';
import { Category, DishStatus, MenuItem, MenuSummary, Page, TechnicalSheet } from '../models';

export interface AdminDishFilters {
  page: number;
  size: number;
  category?: string;
  status?: DishStatus | '';
  search?: string;
}

export interface TechnicalSheetInput {
  ingredientId: number;
  quantity: number;
  correctionFactor: number;
}

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly adminDishes     = `${environment.apiUrl}/admin/pratos`;
  private readonly adminCategories = `${environment.apiUrl}/admin/categorias`;

  // Cardápio público: só pratos ativos de categorias ativas (RN09)
  private _items      = signal<MenuItem[]>([]);
  private _loaded     = signal(false);
  private _categories = signal<string[]>([]);

  readonly items$      = this._items.asReadonly();
  readonly loaded$     = this._loaded.asReadonly();
  readonly categories$ = this._categories.asReadonly();

  constructor(private http: HttpClient) {
    this.loadMenu();
    this.loadCategories();
  }

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

  private loadCategories(): void {
    this.http.get<Category[]>(`${environment.apiUrl}/cardapio/categorias`).subscribe({
      next: categories => this._categories.set(categories.map(c => c.name)),
      error: () => this._categories.set([]),
    });
  }

  getAll(): MenuItem[] { return this._items(); }
  getCategories(): string[] { return this._categories(); }
  getByCategory(cat: string): MenuItem[] { return this._items().filter(i => i.category === cat); }
  getSpecials(): MenuItem[] { return this._items().filter(i => i.isSpecial).slice(0, 3); }

  // ── Gestão de pratos (gerente) ──────────────────────────────

  listAdmin(filters: AdminDishFilters): Promise<Page<MenuItem>> {
    let params = new HttpParams().set('page', filters.page).set('size', filters.size);
    if (filters.category) params = params.set('categoria', filters.category);
    if (filters.status)   params = params.set('status', filters.status);
    if (filters.search?.trim()) params = params.set('busca', filters.search.trim());
    return this.read(this.http.get<Page<MenuItem>>(this.adminDishes, { params }));
  }

  summary(): Promise<MenuSummary> {
    return this.read(this.http.get<MenuSummary>(`${this.adminDishes}/resumo`));
  }

  // Prato novo nasce pausado: só ativa depois de ter ficha técnica (RN01)
  addItem(item: Omit<MenuItem, 'id'>): Promise<MenuItem> {
    return this.write(this.http.post<MenuItem>(this.adminDishes, item));
  }

  updateItem(id: string, data: Partial<MenuItem>): Promise<MenuItem> {
    return this.write(this.http.put<MenuItem>(`${this.adminDishes}/${id}`, data));
  }

  setStatus(id: string, status: DishStatus): Promise<MenuItem> {
    return this.write(this.http.patch<MenuItem>(`${this.adminDishes}/${id}/status`, { status }));
  }

  // Exclusão lógica: o prato fica inativo, mas continua no banco
  deleteItem(id: string): Promise<unknown> {
    return this.write(this.http.delete(`${this.adminDishes}/${id}`));
  }

  // ── Ficha técnica ───────────────────────────────────────────

  getTechnicalSheet(dishId: string): Promise<TechnicalSheet> {
    return this.read(this.http.get<TechnicalSheet>(`${this.adminDishes}/${dishId}/ficha-tecnica`));
  }

  saveTechnicalSheet(dishId: string, items: TechnicalSheetInput[]): Promise<TechnicalSheet> {
    return this.write(this.http.put<TechnicalSheet>(`${this.adminDishes}/${dishId}/ficha-tecnica`, { items }));
  }

  // ── Categorias ──────────────────────────────────────────────

  listAdminCategories(page = 0, size = 100): Promise<Page<Category>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.read(this.http.get<Page<Category>>(this.adminCategories, { params }));
  }

  addCategory(name: string): Promise<Category> {
    return this.write(this.http.post<Category>(this.adminCategories, { name }));
  }

  updateCategory(id: string, data: Pick<Category, 'name'> & Partial<Pick<Category, 'status'>>): Promise<Category> {
    return this.write(this.http.put<Category>(`${this.adminCategories}/${id}`, data));
  }

  // Exclusão lógica: os pratos da categoria saem do cardápio público
  deleteCategory(id: string): Promise<unknown> {
    return this.write(this.http.delete(`${this.adminCategories}/${id}`));
  }

  private async read<T>(request: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(request);
    } catch (e) {
      throw new Error(errorMessage(e, 'Erro ao carregar. Verifique a conexão.'));
    }
  }

  // Envia a escrita, atualiza o cardápio público e devolve a mensagem do backend em caso de erro
  private async write<T>(request: Observable<T>): Promise<T> {
    let result: T;
    try {
      result = await firstValueFrom(request);
    } catch (e) {
      throw new Error(errorMessage(e, 'Erro ao salvar. Tente novamente.'));
    }
    this.loadMenu();
    this.loadCategories();
    return result;
  }
}
