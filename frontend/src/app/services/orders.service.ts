import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { switchMap, map, tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Order } from '../../models'; 

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private http = inject(HttpClient);
  private _orders = signal<Order[]>([]);

 
  readonly orders$: Observable<Order[]> = timer(0, 5000).pipe(
    switchMap(() => this.http.get<Order[]>(`${environment.apiUrl}/pedidos`)),
    map(list => list.map(o => ({ ...o, createdAt: new Date(o.createdAt) }))),
    tap(list => this._orders.set(list)),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  constructor() {
    
    this.orders$.subscribe();
  }

  getOrders(): Order[] {
    return this._orders();
  }

  
  addOrder(orderData: any): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/pedidos`, orderData);
  }

 
  updateStatus(id: string, status: string): Observable<any> {
    return this.http.patch(`${environment.apiUrl}/pedidos/${id}/status`, { status });
  }

  
  assignDelivery(id: string): Observable<any> {
    return this.http.patch(`${environment.apiUrl}/pedidos/${id}/entregador`, {});
  }

  
  cancelOrder(id: string, motivoCancelamento: string): Observable<any> {
    return this.http.patch(`${environment.apiUrl}/pedidos/${id}/status`, { 
      status: 'cancelled',
      motivo_cancelamento: motivoCancelamento
    });
  }
}