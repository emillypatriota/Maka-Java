import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { errorMessage } from '../core/http-error';
import { MenuItem } from '../models';

export const CATEGORIES = ['Carnes Nobres', 'Entradas', 'Peixes & Frutos do Mar', 'Acompanhamentos', 'Sobremesas', 'Bebidas'];

// Resposta paginada do Spring (Page<ProdutoResponse>)
interface MenuPage {
  content: MenuItem[];
  totalElements: number;
}

// Tamanho máximo aceito pelo backend: a tela de gestão ainda filtra tudo no front
const ADMIN_PAGE_SIZE = 100;

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly adminEndpoint = `${environment.apiUrl}/admin/pratos`;

  // Cardápio público: só pratos ativos
  private _items  = signal<MenuItem[]>([]);
  private _loaded = signal(false);

  // Gestão: ativos e inativos (exige login de gerente)
  private _adminItems  = signal<MenuItem[]>([]);
  private _adminLoaded = signal(false);

  readonly items$       = this._items.asReadonly();
  readonly loaded$      = this._loaded.asReadonly();
  readonly adminItems$  = this._adminItems.asReadonly();
  readonly adminLoaded$ = this._adminLoaded.asReadonly();

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

  loadAdmin(): void {
    const params = new HttpParams().set('page', 0).set('size', ADMIN_PAGE_SIZE);
    this.http.get<MenuPage>(this.adminEndpoint, { params }).subscribe({
      next: page => {
        this._adminItems.set(page.content);
        this._adminLoaded.set(true);
      },
      error: () => {
        this._adminItems.set([]);
        this._adminLoaded.set(true);
      },
    });
  }

  getAll(): MenuItem[] { return this._items(); }
  getCategories(): string[] { return CATEGORIES; }
  getByCategory(cat: string): MenuItem[] { return this._items().filter(i => i.category === cat); }
  getSpecials(): MenuItem[] { return this._items().filter(i => i.isSpecial).slice(0, 3); }

  async addItem(item: Omit<MenuItem, 'id'>): Promise<void> {
    await this.write(this.http.post(this.adminEndpoint, item));
  }

  async updateItem(id: string, data: Partial<MenuItem>): Promise<void> {
    await this.write(this.http.put(`${this.adminEndpoint}/${id}`, data));
  }

  // Exclusão lógica: o prato sai do cardápio público, mas continua no banco
  async deleteItem(id: string): Promise<void> {
    await this.write(this.http.delete(`${this.adminEndpoint}/${id}`));
  }

  async toggleItem(id: string, active: boolean): Promise<void> {
    await this.write(this.http.patch(`${this.adminEndpoint}/${id}/ativo`, { active }));
  }

  // Envia a escrita, recarrega as duas listas e devolve a mensagem do backend em caso de erro
  private async write(request: Observable<unknown>): Promise<void> {
    try {
      await firstValueFrom(request);
    } catch (e) {
      throw new Error(errorMessage(e, 'Erro ao salvar. Tente novamente.'));
    }
    this.loadMenu();
    this.loadAdmin();
  }
}
