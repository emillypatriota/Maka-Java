import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs'; 
import { switchMap, map, tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Order } from '../models'; 

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private http = inject(HttpClient);
  private _orders = signal<Order[]>([]);

  readonly orders: Observable<Order[]> = timer(0, 5000).pipe(
    switchMap(() => this.http.get<Order[]>(`${environment.apiUrl}/pedidos`)),
    map(list => list.map(o => ({ ...o, createdAt: new Date(o.createdAt) }))),
    tap(list => this._orders.set(list)),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  constructor() {
    this.orders.subscribe();
  }

  getOrders(): Order[] {
    return this._orders();
  }
  
  getAdminPedidos(): Observable<Order[]> {
    return this.http.get<Order[]>(`${environment.apiUrl}/admin/pedidos`);
  }
  
  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${environment.apiUrl}/pedidos/meus`);
  }
  
  async addOrder(orderData: any): Promise<Order | undefined> {
    
    return this.http.post<Order>(`${environment.apiUrl}/pedidos`, orderData).toPromise();
  }

  async updateStatus(id: string, status: string): Promise<any> {
    return this.http.patch(`${environment.apiUrl}/pedidos/${id}/status`, { status }).toPromise();
  }

  async assignDelivery(id: string): Promise<any> {
    return this.http.patch(`${environment.apiUrl}/pedidos/${id}/entregador`, {}).toPromise();
  }

  async cancelOrder(id: string, motivo: string): Promise<any> {
    return this.http.patch(`${environment.apiUrl}/admin/pedidos/${id}/cancelar`, { motivo }).toPromise();
  }
}