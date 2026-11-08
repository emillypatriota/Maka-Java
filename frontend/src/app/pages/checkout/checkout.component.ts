import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrdersService } from '../../services/orders.service';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss'
})
export class CheckoutComponent {
  public cartService = inject(CartService); 
  private ordersService = inject(OrdersService);
  private router = inject(Router);

  customerName = '';
  customerAddress = '';
  customerPhone = '';
  paymentMethod = 'PIX';
  observations = '';

  async finalizarCompra() {
    if (!this.customerName || !this.customerAddress) {
      alert('Por favor, preencha o nome e o endereço.');
      return;
    }

    const items = this.cartService.getItems();
    if (items.length === 0) {
      alert('Seu carrinho está vazio.');
      return;
    }

    try {
      
      localStorage.setItem('maka_customer_phone', this.customerPhone);

      const novoPedido = await this.ordersService.addOrder({
        items: items.map(i => ({
          name: i.name,
          price: i.price,
          quantity: i.quantity,
          image: i.image
        })),
        customer: {
          name: this.customerName,
          address: this.customerAddress,
          phone: this.customerPhone
        },
        paymentMethod: this.paymentMethod,
        observations: this.observations,
        total: this.cartService.getTotal(),
        deliveryFee: 0.00
      });

      
      this.cartService.clear();

      
      this.router.navigate(['/meus-pedidos']);
    } catch (error) {
      console.error('Erro ao finalizar o pedido:', error);
      alert('Erro ao finalizar o pedido. Tente novamente.');
    }
  }
}