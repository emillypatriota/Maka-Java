import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgFor, NgIf, DatePipe } from '@angular/common';
import { OrdersService } from '../../services/orders.service';
import { Order } from '../../models';

const STATUS_STEPS = ['pending','preparing','ready','delivering','delivered'];
const STATUS_CONFIG: Record<string, { label:string; color:string; bg:string; desc:string; }> = {
  pending:    { label:'Aguardando',   color:'#78716c', bg:'#f5f5f4', desc:'Pedido recebido. Aguardando confirmação da cozinha.' },
  preparing:  { label:'Preparando',  color:'#b45309', bg:'#fef3c7', desc:'Nossa cozinha está preparando seu pedido com cuidado!' },
  ready:      { label:'Pronto',      color:'#1d4ed8', bg:'#dbeafe', desc:'Pedido pronto! O entregador irá buscá-lo em instantes.' },
  delivering: { label:'A caminho',   color:'#065f46', bg:'#d1fae5', desc:'Seu pedido saiu para entrega. Fique de olho! 🚚' },
  delivered:  { label:'Entregue',    color:'#15803d', bg:'#dcfce7', desc:'Pedido entregue com sucesso. Bom apetite! 🥩' },
  cancelled:  { label:'Cancelado',   color:'#b91c1c', bg:'#fee2e2', desc:'Este pedido foi cancelado.' },
};

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [RouterLink, NgFor, NgIf, DatePipe],
  template: `
    <div style="padding:2.5rem 0 4rem;background:var(--stone-50);min-height:60vh">
      <div class="container" style="max-width:740px">
        <div style="margin-bottom:2rem">
          <h1 style="font-family:var(--font-serif);font-size:2.25rem;color:var(--stone-900)">Meus Pedidos</h1>
          <p style="color:var(--stone-500);font-size:.875rem">Acompanhe o status das suas entregas em tempo real</p>
        </div>

        <!-- Vazio -->
        <div *ngIf="orders.length===0" style="text-align:center;padding:5rem 2rem">
          <div style="width:5rem;height:5rem;border-radius:50%;background:var(--stone-100);display:flex;align-items:center;justify-content:center;font-size:2rem;margin:0 auto 1.5rem">📦</div>
          <h2 style="font-family:var(--font-serif);font-size:1.75rem;margin-bottom:.75rem;color:var(--stone-900)">Nenhum pedido ainda</h2>
          <p style="color:var(--stone-500);margin-bottom:2rem;font-size:.9rem">Faça seu primeiro pedido e acompanhe aqui em tempo real.</p>
          <a routerLink="/cardapio" class="btn btn-primary btn-lg">Ver Cardápio</a>
        </div>

        <!-- Lista de pedidos -->
        <div *ngFor="let order of orders" class="card animate-fade-in" style="margin-bottom:1.25rem;overflow:hidden">
          <div style="height:4px;width:100%" [style.background]="cfg(order.status).color"></div>
          <div class="card-body">

            <!-- Header do pedido -->
            <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:1.25rem">
              <div>
                <div style="display:flex;align-items:center;gap:.625rem;margin-bottom:.375rem">
                  <span style="font-family:monospace;font-size:.78rem;background:var(--stone-100);padding:.2rem .6rem;border-radius:4px;color:var(--stone-500)">#{{ order.orderNumber }}</span>
                  <span class="badge" [style.background]="cfg(order.status).bg" [style.color]="cfg(order.status).color">
                    {{ cfg(order.status).label }}
                  </span>
                </div>
                <p style="font-size:.78rem;color:var(--stone-400)">{{ order.createdAt | date:'dd/MM/yyyy' }} às {{ order.createdAt | date:'HH:mm' }}</p>
              </div>
              <div style="text-align:right">
                <p style="font-family:var(--font-serif);font-size:1.5rem;color:var(--stone-900)">R$ {{ order.total.toFixed(2) }}</p>
                <p style="font-size:.75rem;color:var(--stone-400)">{{ order.paymentMethod }}</p>
              </div>
            </div>

            <!-- Barra de progresso -->
            <div *ngIf="order.status!=='cancelled'" style="margin-bottom:1.25rem">
              <div class="progress-steps" style="margin-bottom:.5rem">
                <div *ngFor="let s of steps; let i=index" class="progress-step" [class.done]="i<=stepIndex(order.status)"></div>
              </div>
              <p style="font-size:.78rem;color:var(--stone-500)">{{ cfg(order.status).desc }}</p>
            </div>

            <!-- Itens -->
            <div style="background:var(--stone-50);border-radius:var(--r-lg);padding:1rem;margin-bottom:1rem">
              <p style="font-size:.68rem;font-weight:600;color:var(--stone-400);letter-spacing:.1em;margin-bottom:.625rem">ITENS DO PEDIDO</p>
              <div *ngFor="let item of order.items" style="display:flex;align-items:center;gap:.75rem;padding:.375rem 0;border-bottom:1px solid var(--stone-100)">
                <img [src]="item.image" [alt]="item.name" style="width:2.5rem;height:2.5rem;border-radius:6px;object-fit:cover;flex-shrink:0">
                <span style="flex:1;font-size:.85rem;color:var(--stone-700)">{{ item.quantity }}× {{ item.name }}</span>
                <span style="font-size:.85rem;font-weight:500;color:var(--stone-800)">R$ {{ (item.price*item.quantity).toFixed(2) }}</span>
              </div>
            </div>

            <!-- Endereço -->
            <p style="font-size:.8rem;color:var(--stone-400);display:flex;align-items:flex-start;gap:.375rem">
              <span style="flex-shrink:0">📍</span><span>{{ order.customer.address }}</span>
            </p>
            <p *ngIf="order.observations" style="font-size:.78rem;background:#fffbeb;border:1px solid #fde68a;padding:.5rem .75rem;border-radius:6px;color:#92400e;margin-top:.5rem">
              📝 {{ order.observations }}
            </p>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class MyOrdersComponent implements OnInit {
  orders: Order[] = [];
  steps = STATUS_STEPS;
  cfg(status: string) { return STATUS_CONFIG[status] ?? STATUS_CONFIG['pending']; }
  stepIndex(status: string) { return STATUS_STEPS.indexOf(status); }
  constructor(private ordersService: OrdersService) {}
  ngOnInit() {
    const myPhone = localStorage.getItem('maka_customer_phone');
    this.ordersService.orders.subscribe(all => {
      this.orders = myPhone
        ? all.filter(o => o.customer?.phone === myPhone)
        : all.filter(o => o.isLocal);
    });
  }
}
