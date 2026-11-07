import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer, of, BehaviorSubject } from 'rxjs'; 
import { switchMap, map, tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Order } from '../models'; 

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private http = inject(HttpClient);
  private _orders = signal<Order[]>([]);

    
  private _localMyOrders = new BehaviorSubject<any[]>([
    { 
      id: '101', 
      status: 'preparing', 
      createdAt: new Date().toISOString(), 
      total: 55.0, 
      items: [
        { name: 'Pizza Margherita', quantity: 1 },
        { name: 'Coca-Cola 2L', quantity: 1 }
      ]
    },
    { 
      id: '98', 
      status: 'delivered', 
      createdAt: new Date(Date.now() - 86400000).toISOString(), 
      total: 32.0, 
      items: [
        { name: 'Hamburguer Duplo', quantity: 1 }
      ]
    }
  ]);

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
    return of([
      { id: '1', status: 'pending', createdAt: new Date().toISOString(), total: 45.0, canal: 'WEB', items: [{ name: 'X-Burger', quantity: 2 }] },
      { id: '2', status: 'preparing', createdAt: new Date().toISOString(), total: 32.0, canal: 'APP', items: [{ name: 'Batata Frita', quantity: 1 }] },
      { id: '3', status: 'ready', createdAt: new Date().toISOString(), total: 22.0, canal: 'BALCAO', items: [{ name: 'Salada', quantity: 1 }] }
    ] as unknown as Order[]); 
  }
  
  getMyOrders(): Observable<any[]> {
    return this._localMyOrders.asObservable();
  }
  
  async addOrder(orderData: any): Promise<Order> {
    try {
      const req = this.http.post<Order>(`${environment.apiUrl}/pedidos`, orderData);
      const res = await req.toPromise();
      if (res) {
        this.adicionarPedidoLocal(res);
        return res;
      }
    } catch (e) {
     
      console.warn('[OrdersService] Backend indisponível, registrando pedido via mock local.');
    }

    const mockNovoPedido = {
      id: String(Math.floor(Math.random() * 9000) + 1000),
      status: 'pending',
      createdAt: new Date().toISOString(),
      ...orderData
    };

    this.adicionarPedidoLocal(mockNovoPedido);
    return mockNovoPedido as unknown as Order;
  }

  private adicionarPedidoLocal(pedido: any) {
    const atual = this._localMyOrders.value;
    this._localMyOrders.next([pedido, ...atual]);
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