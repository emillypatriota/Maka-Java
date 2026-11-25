import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Page } from '../models';

// RF-041: usuários internos que o ADMIN cria (gerente, cozinheiro, entregador)
export type InternalRole = 'manager' | 'kitchen' | 'delivery';
export type UserStatus = 'ATIVO' | 'INATIVO';

export interface InternalUser {
  id: string;
  username: string;
  email: string;
  role: 'admin' | InternalRole;
  status: UserStatus;
  createdAt: string | null;
}

export interface InternalUserCreate {
  username: string;
  email: string;
  password: string;
  role: InternalRole;
}

// password vazio mantém a senha atual
export interface InternalUserUpdate {
  username: string;
  role: InternalRole;
  password?: string;
}

export const ROLE_LABEL: Record<string, string> = {
  admin: 'Administrador',
  manager: 'Gerente',
  kitchen: 'Cozinheiro',
  delivery: 'Entregador',
};

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly endpoint = `${environment.apiUrl}/admin/usuarios`;

  constructor(private http: HttpClient) {}

  listar(page: number, size = 10): Observable<Page<InternalUser>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<InternalUser>>(this.endpoint, { params });
  }

  criar(data: InternalUserCreate): Observable<InternalUser> {
    return this.http.post<InternalUser>(this.endpoint, data);
  }

  atualizar(id: string, data: InternalUserUpdate): Observable<InternalUser> {
    return this.http.put<InternalUser>(`${this.endpoint}/${id}`, data);
  }

  alterarStatus(id: string, status: UserStatus): Observable<InternalUser> {
    return this.http.patch<InternalUser>(`${this.endpoint}/${id}/status`, { status });
  }
}
