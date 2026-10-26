import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { switchMap, map, tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Order, OrderStatus } from '../models';

@Injectable({
  providedIn: 'root'
})
export class OrdersService {
  private _orders = signal<Order[]>([]);
  readonly orders$ = this._orders.asReadonly();

  // Polling a cada 5 segundos para atualizar os pedidos via HTTP GET
  readonly orders: Observable<Order[]> = timer(0, 5000).pipe(
    switchMap(() => this.http.get<Order[]>(`${environment.apiUrl}/pedidos`)),
    map(list => list.map(o => ({ ...o, createdAt: new Date(o.createdAt) }))),
    tap(list => this._orders.set(list)),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  constructor(private http: HttpClient) {
    // Subscreve para manter o fluxo de polling ativo na aplicação
    this.orders.subscribe();
  }

  getOrders(): Order[] {
    return this._orders();
  }

  async addOrder(orderData: any): Promise<Order> {
    const response = await this.http.post<Order>(`${environment.apiUrl}/pedidos`, orderData).toPromise();
    if (!response) {
      throw new Error('Erro ao criar pedido');
    }
    return response;
  }

  async updateStatus(orderId: string, status: OrderStatus): Promise<void> {
    await this.http.patch(`${environment.apiUrl}/pedidos/${orderId}/status`, { status }).toPromise();
  }

  async assignDelivery(orderId: string, deliveryPersonId: string): Promise<void> {
    await this.http.patch(`${environment.apiUrl}/pedidos/${orderId}/entregador`, { deliveryPersonId }).toPromise();
  }
}