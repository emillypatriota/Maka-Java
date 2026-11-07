import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrdersService } from '../../../services/orders.service';
import { Order } from '../../../models'; 
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-kitchen-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kitchen-dashboard.component.html'
})
export class KitchenDashboardComponent implements OnInit {
  pedidosRecebidos$!: Observable<Order[]>;

  constructor(private ordersService: OrdersService) {}

  ngOnInit(): void {
    this.pedidosRecebidos$ = this.ordersService.getAdminPedidos().pipe(
      map(pedidos => pedidos.filter(p => String(p.status) === 'RECEBIDO'))
    );
  }

  iniciarPreparo(id: string | number): void {
    this.ordersService.atualizarStatus(id, 'EM_PREPARO').subscribe(() => {
      console.log(`Pedido ${id} movido para EM_PREPARO`);
    });
  }
}