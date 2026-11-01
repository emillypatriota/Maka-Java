import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { errorMessage } from '../core/http-error';

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

@Injectable({ providedIn: 'root' })
export class StaffService {
  private readonly adminEndpoint = `${environment.apiUrl}/admin/funcionarios`;

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
