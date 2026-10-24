import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

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

// Mensagem mostrada no toast enquanto a escrita não existe no backend
const NOT_AVAILABLE = 'Disponível na próxima versão';

@Injectable({ providedIn: 'root' })
export class StaffService {
  private _staff  = signal<StaffMember[]>([]);
  private _loaded = signal(false);

  readonly staff$  = this._staff.asReadonly();
  readonly loaded$ = this._loaded.asReadonly();

  constructor(private http: HttpClient) {}

  load(): void {
    this.http.get<StaffMember[]>(`${environment.apiUrl}/funcionarios`).subscribe({
      next: staff => {
        this._staff.set(staff);
        this._loaded.set(true);
      },
      error: () => {
        this._staff.set([]);
        this._loaded.set(true);
      },
    });
  }

  // TODO (Sprint 2 – Gestão): POST /api/funcionarios e recarregar a lista
  async addMember(member: Omit<StaffMember, 'id'>): Promise<void> {
    throw new Error(NOT_AVAILABLE);
  }
  // TODO (Sprint 2 – Gestão): PUT /api/funcionarios/{id} e recarregar a lista
  async updateMember(id: string, data: Omit<StaffMember, 'id'>): Promise<void> {
    throw new Error(NOT_AVAILABLE);
  }
  // TODO (Sprint 2 – Gestão): PATCH /api/funcionarios/{id}/status e recarregar a lista
  async setStatus(id: string, status: StaffStatus): Promise<void> {
    throw new Error(NOT_AVAILABLE);
  }
  // TODO (Sprint 2 – Gestão): DELETE /api/funcionarios/{id} e recarregar a lista
  async removeMember(id: string): Promise<void> {
    throw new Error(NOT_AVAILABLE);
  }
}
