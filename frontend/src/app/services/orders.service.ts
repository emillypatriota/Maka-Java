import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer, of } from 'rxjs';
import { switchMap, map, tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Order } from '../models'; 

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private http = inject(HttpClient);
  private _orders = signal<Order[]>([]);

 
  private mockPedidos: any[] = [
    { id: '1', status: 'RECEBIDO', createdAt: new Date().toISOString(), total: 45.0, canal: 'WEB', items: [{ name: 'X-Burger', quantity: 2 }] },
    { id: '2', status: 'EM_PREPARO', createdAt: new Date().toISOString(), total: 32.0, canal: 'APP', items: [{ name: 'Batata Frita', quantity: 1 }] },
    { id: '3', status: 'RECEBIDO', createdAt: new Date().toISOString(), total: 22.0, canal: 'BALCAO', items: [{ name: 'Salada', quantity: 1 }] }
  ];

  readonly orders: Observable<Order[]> = timer(0, 5000).pipe(
  
    switchMap(() => of(this.mockPedidos as Order[])), // <- MOCK
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
    return this.orders;
  }

  async addOrder(orderData: any): Promise<Order> {
    
    const novoPedido = { ...orderData, id: Math.random().toString(), status: 'RECEBIDO', createdAt: new Date().toISOString() };
    this.mockPedidos.push(novoPedido);
    return novoPedido;
  }

  async updateStatus(id: string, status: string): Promise<any> {
    
    
    const pedido = this.mockPedidos.find(p => String(p.id) === String(id));
    if (pedido) pedido.status = status;
    return { success: true };
  }

 
  atualizarStatus(id: string | number, novoStatus: string): Observable<any> {
    const pedido = this.mockPedidos.find(p => String(p.id) === String(id));
    if (pedido) pedido.status = novoStatus;
    return of({ success: true });
  }

  async assignDelivery(id: string): Promise<any> {
    return this.http.patch(`${environment.apiUrl}/pedidos/${id}/entregador`, {}).toPromise();
  }

  async cancelOrder(id: string, motivo: string): Promise<any> {
    return this.http.patch(`${environment.apiUrl}/admin/pedidos/${id}/cancelar`, { motivo }).toPromise();
  }
}