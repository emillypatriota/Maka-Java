import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrdersService } from '../../services/orders.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss'
})
export class CheckoutComponent {
  customerName = '';
  customerAddress = '';
  customerPhone = '';

  constructor(
    private ordersService: OrdersService,
    private router: Router
  ) {}

  async finalizarCompra() {
    if (!this.customerName || !this.customerAddress) {
      alert('Por favor, preencha o nome e a morada.');
      return;
    }

    try {
      const novoPedido = await this.ordersService.addOrder({
        items: [],
        customer: {
          name: this.customerName,
          address: this.customerAddress,
          phone: this.customerPhone
        },
        paymentMethod: 'Dinheiro',
        total: 0.00,
        deliveryFee: 0.00
      });

      this.router.navigate(['/confirmation', novoPedido.id]);
    } catch (error) {
      console.error('Erro ao finalizar o pedido:', error);
    }
  }
}