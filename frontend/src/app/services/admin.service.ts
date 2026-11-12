import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface MenuItem {
  id?: string;
  name: string;
  description: string;
  price: number;
  category: string;
  available: boolean;
}

export interface InsightsDTO {
  totalRevenue: number;
  totalOrders: number;
  topSellingItems: { name: string; quantity: number }[];
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);

  getMenuItems(): Observable<MenuItem[]> {
    return this.http.get<MenuItem[]>(`${environment.apiUrl}/admin/menu`);
  }

  addMenuItem(item: MenuItem): Observable<MenuItem> {
    return this.http.post<MenuItem>(`${environment.apiUrl}/admin/menu`, item);
  }

  deleteMenuItem(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/menu/${id}`);
  }

  getInsights(): Observable<InsightsDTO> {
    return this.http.get<InsightsDTO>(`${environment.apiUrl}/admin/insights`);
  }
}