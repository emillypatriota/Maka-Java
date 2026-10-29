import { Component, OnInit, OnDestroy } from '@angular/core';
import { NgFor, NgIf, DatePipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { OrdersService } from '../../../services/orders.service';
import { ToastService } from '../../../services/toast.service';
import { Order, OrderStatus } from '../../../models';

@Component({
  selector: 'app-kitchen-dashboard',
  standalone: true,
  imports: [NgFor, NgIf, DatePipe],
  template: `
    <div style="padding:0 1.5rem;height:calc(100vh - 140px);display:flex;flex-direction:column">
      <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:1.5rem;flex-wrap:wrap;gap:1rem" class="animate-fade-up">
        <div>
          <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">Cozinha & Expedição</h1>
          <p style="color:var(--text-tertiary);font-size:var(--text-base)">Fluxo de pedidos em tempo real</p>
        </div>
        <div style="display:flex;align-items:center;gap:.75rem">
          <span class="online-badge"><span class="dot"></span>Ao vivo</span>
          <span style="padding:.4rem .875rem;border-radius:var(--r-full);background:var(--bg-card);border:1px solid var(--border);font-size:var(--text-sm);color:var(--text-secondary)">
            {{ allOrders.length }} pedido{{ allOrders.length !== 1 ? 's' : '' }} total
          </span>
        </div>
      </div>
      <div class="kanban" style="flex:1">
        <!-- Pendentes -->
        <div class="kanban-col col-pending">
          <div class="kanban-header">
            <div class="kanban-title"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="color:var(--neutral)"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>Aguardando</div>
            <span class="kanban-count">{{ pending.length }}</span>
          </div>
          <div class="kanban-body">
            <div *ngIf="pending.length === 0" class="empty-state" style="padding:2.5rem 1rem"><div class="empty-icon" style="font-size:1.25rem">⏳</div><p style="font-size:var(--text-sm)">Nenhum pedido aguardando</p></div>
            <div *ngFor="let o of pending; trackBy: trackById" class="kanban-card animate-scale-in">
              <div class="kanban-card-header">
                <div style="display:flex;align-items:center;gap:.5rem"><span class="order-number">#{{ o.orderNumber }}</span><span style="font-size:var(--text-xs);color:var(--text-tertiary);font-family:var(--font-mono)">{{ o.createdAt | date:'HH:mm' }}</span></div>
                <span class="badge badge-pending"><span class="badge-dot"></span>Aguardando</span>
              </div>
              <div style="font-size:var(--text-sm);font-weight:600;color:var(--text-primary);margin-bottom:.75rem">{{ o.customer.name }}</div>
              <div class="kanban-card-items"><div *ngFor="let item of o.items" class="kanban-item"><span class="item-qty">{{ item.quantity }}x</span><span>{{ item.name }}</span></div></div>
              <div *ngIf="o.observations" style="margin-bottom:.875rem;padding:.625rem .75rem;background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-md);font-size:var(--text-xs);color:var(--danger-text)">⚠️ {{ o.observations }}</div>
              <div style="font-size:var(--text-xs);color:var(--text-tertiary);margin-bottom:.875rem;display:flex;align-items:center;gap:.375rem"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>{{ o.customer.address }}</div>
              <button class="btn btn-secondary btn-sm" style="width:100%;justify-content:center" (click)="advance(o.id, 'preparing')">Iniciar Preparo<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg></button>
            </div>
          </div>
        </div>
        <!-- Em Preparo -->
        <div class="kanban-col col-preparing">
          <div class="kanban-header">
            <div class="kanban-title"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="color:var(--warning)"><path d="M18 8h1a4 4 0 010 8h-1M2 8h16v9a4 4 0 01-4 4H6a4 4 0 01-4-4V8zM6 1v3M10 1v3M14 1v3"/></svg>Em Preparo</div>
            <span class="kanban-count" style="background:var(--warning-bg);color:var(--warning-text)">{{ preparing.length }}</span>
          </div>
          <div class="kanban-body">
            <div *ngIf="preparing.length === 0" class="empty-state" style="padding:2.5rem 1rem"><div class="empty-icon" style="font-size:1.25rem">👨‍🍳</div><p style="font-size:var(--text-sm)">Nenhum prato sendo preparado</p></div>
            <div *ngFor="let o of preparing; trackBy: trackById" class="kanban-card animate-scale-in">
              <div class="kanban-card-header">
                <div style="display:flex;align-items:center;gap:.5rem"><span class="order-number">#{{ o.orderNumber }}</span><span style="font-size:var(--text-xs);color:var(--text-tertiary);font-family:var(--font-mono)">{{ o.createdAt | date:'HH:mm' }}</span></div>
                <span class="badge badge-preparing"><span class="badge-dot"></span>Preparando</span>
              </div>
              <div style="font-size:var(--text-sm);font-weight:600;color:var(--text-primary);margin-bottom:.75rem">{{ o.customer.name }}</div>
              <div class="kanban-card-items"><div *ngFor="let item of o.items" class="kanban-item"><span class="item-qty" style="background:var(--warning-bg);color:var(--warning-text)">{{ item.quantity }}x</span><span>{{ item.name }}</span></div></div>
              <div *ngIf="o.observations" style="margin-bottom:.875rem;padding:.625rem .75rem;background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-md);font-size:var(--text-xs);color:var(--danger-text)">⚠️ {{ o.observations }}</div>
              <div style="font-size:var(--text-xs);color:var(--text-tertiary);margin-bottom:.875rem;display:flex;align-items:center;gap:.375rem"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>{{ o.customer.address }}</div>
              <button class="btn btn-sm" style="width:100%;justify-content:center;background:var(--warning-bg);color:var(--warning-text);border:1px solid var(--warning-border)" (click)="advance(o.id, 'ready')">Marcar como Pronto<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg></button>
            </div>
          </div>
        </div>
        <!-- Prontos -->
        <div class="kanban-col col-ready">
          <div class="kanban-header">
            <div class="kanban-title"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="color:var(--success)"><polyline points="20 6 9 17 4 12"/></svg>Prontos para Entrega</div>
            <span class="kanban-count" style="background:var(--success-bg);color:var(--success-text)">{{ ready.length }}</span>
          </div>
          <div class="kanban-body">
            <div *ngIf="ready.length === 0" class="empty-state" style="padding:2.5rem 1rem"><div class="empty-icon" style="font-size:1.25rem">📦</div><p style="font-size:var(--text-sm)">Nenhum pedido pronto</p></div>
            <div *ngFor="let o of ready; trackBy: trackById" class="kanban-card animate-scale-in">
              <div class="kanban-card-header">
                <div style="display:flex;align-items:center;gap:.5rem"><span class="order-number">#{{ o.orderNumber }}</span><span style="font-size:var(--text-xs);color:var(--text-tertiary);font-family:var(--font-mono)">{{ o.createdAt | date:'HH:mm' }}</span></div>
                <span class="badge badge-ready"><span class="badge-dot"></span>Pronto</span>
              </div>
              <div style="font-size:var(--text-sm);font-weight:600;color:var(--text-primary);margin-bottom:.75rem">{{ o.customer.name }}</div>
              <div class="kanban-card-items"><div *ngFor="let item of o.items" class="kanban-item"><span class="item-qty" style="background:var(--success-bg);color:var(--success-text)">{{ item.quantity }}x</span><span>{{ item.name }}</span></div></div>
              <div style="font-size:var(--text-xs);color:var(--text-tertiary);margin-bottom:.875rem;display:flex;align-items:center;gap:.375rem"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>{{ o.customer.address }}</div>
              <button class="btn btn-sm" style="width:100%;justify-content:center;background:var(--success-bg);color:var(--success-text);border:1px solid var(--success-border)" (click)="advance(o.id, 'delivering')">Saiu para Entrega<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="3" width="15" height="13" rx="1"/><path d="M16 8h4l3 4v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg></button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class KitchenDashboardComponent implements OnInit, OnDestroy {
  allOrders: Order[] = [];
  pending:   Order[] = [];
  preparing: Order[] = [];
  ready:     Order[] = [];
  private sub?: Subscription;

  constructor(
    private ordersService: OrdersService,
    private toastService: ToastService 
  ) {}

  ngOnInit() {
    this.sub = this.ordersService.orders.subscribe({
      next: orders => {
        this.allOrders = orders;
        this.pending   = orders.filter(o => o.status === 'pending');
        this.preparing = orders.filter(o => o.status === 'preparing');
        this.ready     = orders.filter(o => o.status === 'ready');
      },
      error: err => console.error('[Kitchen] orders error:', err),
    });
  }

  ngOnDestroy() { this.sub?.unsubscribe(); }

  trackById(_: number, o: Order) { return o.id; }

  async advance(id: string, next: OrderStatus) {
    try {
      await this.ordersService.updateStatus(id, next);
      this.toastService.show('Status atualizado com sucesso!', 'success');
    } catch (err: any) {
      if (err.status === 409) {
        this.toastService.show(err.error?.message || 'Ação inválida.', 'error'); 
      } else {
        this.toastService.show('Erro ao atualizar o pedido.', 'error');
      }
    }
  }
}