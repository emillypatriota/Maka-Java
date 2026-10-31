import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { errorMessage } from '../core/http-error';
import { Page } from '../models';

export type StaffRole = 'manager' | 'kitchen' | 'delivery' | 'cashier' | 'waiter';
export type StaffStatus = 'active' | 'inactive' | 'on_leave';

export interface StaffMember {
  id: string;
  name: string;
  role: StaffRole;
  phone: string;
  email: string;
  status: StaffStatus;
  schedule: string;
  hireDate: string;
  notes?: string;
  createdAt?: any;
}

export interface StaffFilters {
  page: number;
  role: StaffRole | 'all';
  search: string;
}

export interface StaffSummary {
  total: number;
  active: number;
  byRole: Record<StaffRole, number>;
}

const PAGE_SIZE = 12;

@Injectable({ providedIn: 'root' })
export class StaffService {
  private readonly adminEndpoint = `${environment.apiUrl}/admin/funcionarios`;

  // staff$ guarda só a página atual; os filtros são aplicados no backend
  private _staff   = signal<StaffMember[]>([]);
  private _loaded  = signal(false);
  private _error   = signal('');
  private _page    = signal({ number: 0, totalPages: 0, totalElements: 0 });
  private _summary = signal<StaffSummary | null>(null);
  private filters: StaffFilters = { page: 0, role: 'all', search: '' };

  readonly staff$   = this._staff.asReadonly();
  readonly loaded$  = this._loaded.asReadonly();
  readonly error$   = this._error.asReadonly();
  readonly page$    = this._page.asReadonly();
  readonly summary$ = this._summary.asReadonly();

  constructor(private http: HttpClient) {}

  // Sem argumento, recarrega com os filtros atuais (usado depois de cada escrita)
  load(changes: Partial<StaffFilters> = {}): void {
    this.filters = { ...this.filters, ...changes };
    let params = new HttpParams().set('page', this.filters.page).set('size', PAGE_SIZE);
    if (this.filters.role !== 'all') params = params.set('cargo', this.filters.role);
    if (this.filters.search.trim()) params = params.set('busca', this.filters.search.trim());

    this._loaded.set(false);
    this.http.get<Page<StaffMember>>(this.adminEndpoint, { params }).subscribe({
      next: page => {
        // Página ficou vazia (ex.: último item removido): volta uma
        if (page.content.length === 0 && page.number > 0) {
          this.load({ page: page.number - 1 });
          return;
        }
        this._staff.set(page.content);
        this._page.set({ number: page.number, totalPages: page.totalPages, totalElements: page.totalElements });
        this._error.set('');
        this._loaded.set(true);
      },
      error: e => {
        this._staff.set([]);
        this._error.set(errorMessage(e, 'Erro ao carregar funcionários.'));
        this._loaded.set(true);
      },
    });
    this.http.get<StaffSummary>(`${this.adminEndpoint}/resumo`).subscribe({
      next: summary => this._summary.set(summary),
      error: () => this._summary.set(null),
    });
  }

  async addMember(member: Omit<StaffMember, 'id'>): Promise<void> {
    await this.write(this.http.post(this.adminEndpoint, toPayload(member)));
  }

  async updateMember(id: string, data: Omit<StaffMember, 'id'>): Promise<void> {
    await this.write(this.http.put(`${this.adminEndpoint}/${id}`, toPayload(data)));
  }

  async setStatus(id: string, status: StaffStatus): Promise<void> {
    await this.write(this.http.patch(`${this.adminEndpoint}/${id}/status`, { status }));
  }

  // Exclusão lógica: o funcionário fica inativo, mas continua no banco
  async removeMember(id: string): Promise<void> {
    await this.write(this.http.delete(`${this.adminEndpoint}/${id}`));
  }

  // Envia a escrita, recarrega a lista e devolve a mensagem do backend em caso de erro
  private async write(request: Observable<unknown>): Promise<void> {
    try {
      await firstValueFrom(request);
    } catch (e) {
      throw new Error(errorMessage(e, 'Erro ao salvar. Tente novamente.'));
    }
    this.load();
  }
}

// O input date devolve '' quando vazio; o backend espera null
function toPayload(member: Omit<StaffMember, 'id'>) {
  return { ...member, hireDate: member.hireDate || null };
}
