import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { OrdersService } from '../../../services/orders.service';
import { Order } from '../../../models'; 
import { Observable } from 'rxjs';

@Component({
  selector: 'app-orders-management',
  standalone: true,
  imports: [CommonModule, FormsModule], 
  templateUrl: './orders-management.component.html'
})
export class OrdersManagementComponent implements OnInit {
  pedidos$!: Observable<Order[]>;
  
  
  filtroStatus: string = '';
  filtroCanal: string = '';
  filtroData: string = ''; 

  constructor(private ordersService: OrdersService) {}

  ngOnInit(): void {
  
    this.pedidos$ = this.ordersService.getAdminPedidos();
  }

  
  aplicarFiltros(pedidos: Order[] | null): Order[] {
    if (!pedidos) return [];
    
    return pedidos.filter(p => {
      
      const matchStatus = this.filtroStatus ? String(p.status) === this.filtroStatus : true;
      
      
      const matchCanal = this.filtroCanal ? ((p as any).canal || 'APP') === this.filtroCanal : true;
      
      
      let matchData = true;
      if (this.filtroData && p.createdAt) {
        
        const dataPedido = new Date(p.createdAt).toISOString().split('T')[0];
        matchData = dataPedido === this.filtroData;
      }

      return matchStatus && matchCanal && matchData;
    });
  }
}