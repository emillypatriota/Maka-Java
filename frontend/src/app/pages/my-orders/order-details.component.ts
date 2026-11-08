import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrdersService } from '../../services/orders.service';

const STATUS_CONFIG: Record<string, { label: string; color: string; bg: string; desc: string }> = {
  pending:    { label: 'Aguardando',   color: '#78716c', bg: '#f5f5f4', desc: 'Pedido recebido. Aguardando confirmação da cozinha.' },
  preparing:  { label: 'Preparando',   color: '#b45309', bg: '#fef3c7', desc: 'Nossa cozinha está preparando seu pedido com cuidado!' },
  ready:      { label: 'Pronto',       color: '#1d4ed8', bg: '#dbeafe', desc: 'Pedido pronto! O entregador irá buscá-lo em instantes.' },
  delivering: { label: 'A caminho',    color: '#065f46', bg: '#d1fae5', desc: 'Seu pedido saiu para entrega. Fique de olho! 🚚' },
  delivered:  { label: 'Entregue',     color: '#15803d', bg: '#dcfce7', desc: 'Pedido entregue com sucesso. Bom apetite! 🥩' },
  cancelled:  { label: 'Cancelado',    color: '#b91c1c', bg: '#fee2e2', desc: 'Este pedido foi cancelado.' },
};

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  template: `
    <div style="padding: 2.5rem 0 4rem; background: var(--stone-50, #f9fafb); min-height: 70vh;">
      <div class="container" style="max-width: 740px; margin: 0 auto; padding: 0 1rem;">
        
        <div style="margin-bottom: 1.5rem;">
          <a routerLink="/meus-pedidos" style="color: var(--stone-600); text-decoration: none; font-size: 0.875rem; font-weight: 500;">
            ← Voltar para Meus Pedidos
          </a>
        </div>

        <div *ngIf="loading" style="text-align: center; padding: 4rem; color: var(--stone-500);">
          Carregando detalhes do pedido...
        </div>

        <div *ngIf="!loading && !order" style="text-align: center; padding: 4rem; background: #fff; border-radius: 12px; border: 1px solid var(--stone-200);">
          <h3 style="font-family: var(--font-serif); color: var(--stone-900); margin-bottom: 0.5rem;">Pedido não encontrado</h3>
          <p style="color: var(--stone-500); margin-bottom: 1.5rem;">Não foi possível localizar as informações deste pedido.</p>
          <a routerLink="/meus-pedidos" class="btn btn-primary">Meus Pedidos</a>
        </div>

        <div *ngIf="!loading && order" style="background: #fff; border: 1px solid var(--stone-200, #e5e7eb); border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
          
          <div style="height: 6px; width: 100%;" [style.background]="cfg(order.status).color"></div>

          <div style="padding: 2rem;">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; border-bottom: 1px solid var(--stone-100, #f3f4f6); padding-bottom: 1rem;">
              <div>
                <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem;">
                  <span style="font-family: monospace; font-size: 0.9rem; background: var(--stone-100); padding: 0.25rem 0.6rem; border-radius: 6px; color: var(--stone-600); font-weight: 600;">
                    #{{ order.orderNumber || order.id }}
                  </span>
                  <span class="badge" [style.background]="cfg(order.status).bg" [style.color]="cfg(order.status).color" style="padding: 0.3rem 0.75rem; border-radius: 999px; font-weight: 600; font-size: 0.8rem;">
                    {{ cfg(order.status).label }}
                  </span>
                </div>
                <p style="font-size: 0.82rem; color: var(--stone-400);">
                  Realizado em {{ order.createdAt | date:'dd/MM/yyyy' }} às {{ order.createdAt | date:'HH:mm' }}
                </p>
              </div>
              <div style="text-align: right;">
                <p style="font-family: var(--font-serif); font-size: 1.75rem; color: var(--stone-900); font-weight: bold;">
                  R$ {{ order.total.toFixed(2) }}
                </p>
                <p style="font-size: 0.8rem; color: var(--stone-500);">Pagamento: <strong>{{ order.paymentMethod || 'PIX' }}</strong></p>
              </div>
            </div>

            <div style="background: var(--stone-50); border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem; border-left: 4px solid var(--stone-400);" [style.border-left-color]="cfg(order.status).color">
              <p style="font-size: 0.875rem; color: var(--stone-700); font-weight: 500;">{{ cfg(order.status).desc }}</p>
            </div>

            <div style="margin-bottom: 1.5rem;">
              <h4 style="font-size: 0.75rem; font-weight: 600; color: var(--stone-400); letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 0.75rem;">Itens Adquiridos</h4>
              <div *ngFor="let item of order.items" style="display: flex; align-items: center; justify-content: space-between; padding: 0.625rem 0; border-bottom: 1px solid var(--stone-100);">
                <div style="display: flex; align-items: center; gap: 0.75rem;">
                  <img *ngIf="item.image" [src]="item.image" [alt]="item.name" style="width: 3rem; height: 3rem; border-radius: 6px; object-fit: cover;" onerror="this.style.display='none'">
                  <div>
                    <span style="font-size: 0.9rem; font-weight: 500; color: var(--stone-800); display: block;">{{ item.name }}</span>
                    <span style="font-size: 0.8rem; color: var(--stone-400);">Qtd: {{ item.quantity }} x R$ {{ (item.price || 0).toFixed(2) }}</span>
                  </div>
                </div>
                <span style="font-size: 0.9rem; font-weight: 600; color: var(--stone-900);">
                  R$ {{ ((item.price || 0) * item.quantity).toFixed(2) }}
                </span>
              </div>
            </div>

            <div style="background: var(--stone-50); border-radius: 8px; padding: 1.25rem; margin-bottom: 1.5rem;">
              <h4 style="font-size: 0.75rem; font-weight: 600; color: var(--stone-400); letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 0.75rem;">Endereço de Entrega</h4>
              <p style="font-size: 0.9rem; color: var(--stone-700); display: flex; align-items: flex-start; gap: 0.5rem; margin-bottom: 0.5rem;">
                <span>📍</span> <span>{{ order.customer?.address || 'Endereço não informado' }}</span>
              </p>
              <p style="font-size: 0.85rem; color: var(--stone-500);">
                Recebedor: <strong>{{ order.customer?.name || 'Cliente' }}</strong> ({{ order.customer?.phone || 'Telefone não informado' }})
              </p>
            </div>

            <div *ngIf="order.observations" style="background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 1rem; color: #92400e; font-size: 0.875rem;">
              <strong>📝 Observações:</strong> {{ order.observations }}
            </div>

          </div>
        </div>

      </div>
    </div>
  `
})
export class OrderDetailsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private ordersService = inject(OrdersService);

  order: any = null;
  loading = true;

  cfg(status: string) {
    return STATUS_CONFIG[status] ?? STATUS_CONFIG['pending'];
  }

  ngOnInit() {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (orderId) {
      this.ordersService.getMyOrders().subscribe({
        next: (list) => {
          this.order = list.find((o: any) => String(o.id) === orderId || String(o.orderNumber) === orderId);
          if (this.order && typeof this.order.createdAt === 'string') {
            this.order.createdAt = new Date(this.order.createdAt);
          }
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
    } else {
      this.loading = false;
    }
  }
}