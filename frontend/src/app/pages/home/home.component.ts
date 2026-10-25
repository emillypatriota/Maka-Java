import { Component, OnInit, effect } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgFor, NgIf, NgClass } from '@angular/common';
import { MenuService } from '../../services/menu.service';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { MenuItem } from '../../models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, NgFor, NgIf, NgClass],
  template: `
    <!-- HERO -->
    <section class="hero">
      <img class="hero-img" src="https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?w=1600&q=90" alt="Maka Steakhouse">
      <div class="hero-overlay"></div>
      <div class="container" style="position:relative;z-index:1;padding-top:2rem">
        <div class="hero-content animate-fade-up">
          <div class="hero-tag">
            <div class="hero-tag-line"></div>
            <span>EXCELÊNCIA EM CARNES NOBRES DESDE 2018</span>
          </div>
          <h1>MAKA<br><span style="color:var(--amber-500)">STEAKHOUSE</span></h1>
          <p>Carnes nobres maturadas, entregues na sua porta.<br>Do corte à mesa — experiência premium no delivery.</p>
          <div class="hero-actions">
            <a routerLink="/cardapio" class="btn btn-primary btn-lg">
              <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
              Ver Cardápio
            </a>
            <a routerLink="/meus-pedidos" class="btn btn-outline btn-lg" style="border-color:rgba(255,255,255,.3);color:#fff">
              Rastrear Pedido
            </a>
          </div>
          <div class="hero-stats">
            <div *ngFor="let s of stats">
              <div class="hero-stat-value">{{ s.value }}</div>
              <div class="hero-stat-label">{{ s.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- DELIVERY INFO BAR -->
    <div style="background:var(--stone-900);border-bottom:1px solid var(--stone-800)">
      <div class="container" style="display:flex;align-items:center;justify-content:center;gap:3rem;padding:1rem 1.25rem;flex-wrap:wrap">
        <div *ngFor="let info of deliveryInfo" style="display:flex;align-items:center;gap:.625rem;color:var(--stone-300);font-size:.82rem">
          <span style="font-size:1.1rem">{{ info.icon }}</span>
          <span>{{ info.text }}</span>
        </div>
      </div>
    </div>

    <!-- DESTAQUES -->
    <section style="padding:5rem 0;background:#fff">
      <div class="container">
        <div style="text-align:center;margin-bottom:3rem">
          <div class="section-tag" style="justify-content:center">
            <div class="section-tag-line"></div>
            <span>CORTES PREMIUM</span>
            <div class="section-tag-line"></div>
          </div>
          <h2 class="section-title">Nossos Destaques</h2>
          <p style="color:var(--stone-500);margin-top:.75rem;max-width:480px;margin-left:auto;margin-right:auto">Selecionados pelo nosso chef, maturados com perfeição e entregues no ponto certo</p>
        </div>
        <div class="grid-3">
          <div *ngFor="let item of specials; let i = index" class="menu-card card-hover animate-fade-up" [style.animation-delay]="(i*0.1)+'s'">
            <div class="menu-card-img">
              <img [src]="item.image" [alt]="item.name" loading="lazy">
              <span class="special-tag">★ DESTAQUE</span>
              <div style="position:absolute;inset:0;background:linear-gradient(to top,rgba(0,0,0,.5) 0%,transparent 50%)"></div>
            </div>
            <div class="menu-card-body">
              <h3 style="font-family:var(--font-serif);font-size:1.3rem;margin-bottom:.375rem;color:var(--stone-900)">{{ item.name }}</h3>
              <p style="font-size:.82rem;color:var(--stone-500);line-height:1.6;margin-bottom:1.25rem;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden">{{ item.description }}</p>
              <div style="display:flex;align-items:center;justify-content:space-between;border-top:1px solid var(--stone-100);padding-top:1rem">
                <div>
                  <p style="font-size:.7rem;color:var(--stone-400);letter-spacing:.08em">A PARTIR DE</p>
                  <span class="menu-card-price">R$ {{ item.price.toFixed(2) }}</span>
                </div>
                <button class="btn btn-secondary btn-sm" (click)="addToCart(item)">+ Adicionar</button>
              </div>
            </div>
          </div>
        </div>
        <div style="text-align:center;margin-top:2.5rem">
          <a routerLink="/cardapio" class="btn btn-outline btn-lg">Ver Cardápio Completo →</a>
        </div>
      </div>
    </section>

    <!-- COMO FUNCIONA -->
    <section style="padding:5rem 0;background:var(--stone-50)">
      <div class="container">
        <div style="text-align:center;margin-bottom:3.5rem">
          <div class="section-tag" style="justify-content:center">
            <div class="section-tag-line"></div>
            <span>SIMPLES E RÁPIDO</span>
            <div class="section-tag-line"></div>
          </div>
          <h2 class="section-title">Como funciona</h2>
        </div>
        <div class="grid-4">
          <div *ngFor="let step of howItWorks; let i = index" style="text-align:center;padding:1.5rem 1rem" class="animate-fade-up" [style.animation-delay]="(i*0.1)+'s'">
            <div style="width:4rem;height:4rem;border-radius:50%;background:var(--stone-900);color:#fff;display:flex;align-items:center;justify-content:center;font-size:1.5rem;margin:0 auto 1.25rem;position:relative">
              {{ step.icon }}
              <div style="position:absolute;top:-.5rem;right:-.5rem;width:1.5rem;height:1.5rem;border-radius:50%;background:var(--amber-600);color:#fff;font-size:.72rem;font-weight:700;display:flex;align-items:center;justify-content:center">{{ i+1 }}</div>
            </div>
            <h3 style="font-family:var(--font-serif);font-size:1.1rem;margin-bottom:.5rem;color:var(--stone-900)">{{ step.title }}</h3>
            <p style="font-size:.82rem;color:var(--stone-500);line-height:1.7">{{ step.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- DIFERENCIAIS -->
    <section style="padding:5rem 0;background:var(--stone-900)">
      <div class="container">
        <div style="text-align:center;margin-bottom:3.5rem">
          <div class="section-tag" style="justify-content:center">
            <div class="section-tag-line"></div>
            <span style="color:var(--amber-500)">POR QUE ESCOLHER A MAKA</span>
            <div class="section-tag-line"></div>
          </div>
          <h2 class="section-title" style="color:#fff">Qualidade que você sente</h2>
        </div>
        <div class="grid-3" style="gap:2rem">
          <div *ngFor="let d of diferenciais" style="text-align:center;padding:2rem 1.5rem;border:1px solid rgba(255,255,255,.08);border-radius:var(--r-2xl);background:rgba(255,255,255,.03);transition:all .3s" onmouseenter="this.style.background='rgba(217,119,6,.08)';this.style.borderColor='rgba(217,119,6,.25)'" onmouseleave="this.style.background='rgba(255,255,255,.03)';this.style.borderColor='rgba(255,255,255,.08)'">
            <div style="font-size:2.5rem;margin-bottom:1.25rem">{{ d.icon }}</div>
            <h3 style="font-family:var(--font-serif);font-size:1.3rem;color:#fff;margin-bottom:.75rem">{{ d.title }}</h3>
            <p style="color:var(--stone-400);font-size:.85rem;line-height:1.75">{{ d.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA FINAL -->
    <section class="promo-banner">
      <div class="container" style="display:flex;align-items:center;justify-content:space-between;gap:2rem;flex-wrap:wrap">
        <div style="color:#fff">
          <h2 style="font-family:var(--font-serif);font-size:2rem;margin-bottom:.5rem">Peça agora e ganhe frete grátis</h2>
          <p style="color:var(--stone-400);font-size:.9rem">Em pedidos acima de R$ 150,00 · Entrega em 40–60 min</p>
        </div>
        <a routerLink="/cardapio" class="btn btn-primary btn-lg" style="flex-shrink:0">
          Pedir Agora 🚀
        </a>
      </div>
    </section>
  `,
})
export class HomeComponent implements OnInit {
  specials: MenuItem[] = [];
  stats = [
    { value: '4.9★', label: 'AVALIAÇÃO MÉDIA' },
    { value: '+2000', label: 'PEDIDOS/MÊS' },
    { value: '40min', label: 'TEMPO MÉDIO' },
  ];
  deliveryInfo = [
    { icon: '🚚', text: 'Entrega em 40–60 min' },
    { icon: '🎁', text: 'Frete grátis acima de R$ 150' },
    { icon: '🥩', text: 'Carnes maturadas premium' },
    { icon: '🔒', text: 'Pagamento 100% seguro' },
    { icon: '⭐', text: 'Nota 4.9 no iFood' },
  ];
  howItWorks = [
    { icon: '📋', title: 'Escolha seus pratos', text: 'Navegue pelo cardápio e adicione seus cortes favoritos ao carrinho.' },
    { icon: '📍', title: 'Informe o endereço', text: 'Preencha o endereço de entrega e escolha como quer pagar.' },
    { icon: '👨‍🍳', title: 'Preparamos com cuidado', text: 'Nossa cozinha prepara seu pedido com os mais altos padrões de qualidade.' },
    { icon: '🏠', title: 'Receba em casa', text: 'Em 40–60 minutos seu pedido chega quentinho e embalado com perfeição.' },
  ];
  diferenciais = [
    { icon: '🥩', title: 'Carnes Dry-Aged', text: 'Processo de maturação a seco por até 45 dias que intensifica o sabor e transforma a textura da carne.' },
    { icon: '📦', title: 'Embalagem Térmica', text: 'Caixas térmicas desenvolvidas exclusivamente para manter a temperatura e qualidade até sua porta.' },
    { icon: '⭐', title: 'Chef Premiado', text: 'Nossa equipe é liderada por chef com passagem pelos melhores restaurantes do mundo.' },
    { icon: '🌱', title: 'Origem Rastreada', text: 'Sabemos de onde vem cada corte. Parceria direta com fazendas certificadas do sul do Brasil.' },
    { icon: '⚡', title: 'Entrega Expressa', text: '40 a 60 minutos garantidos ou seu próximo pedido tem frete grátis.' },
    { icon: '💳', title: 'Múltiplos Pagamentos', text: 'Pix, cartões de crédito e débito, e dinheiro. Você escolhe a forma mais conveniente.' },
  ];

  constructor(private menu: MenuService, private cart: CartService, private toast: ToastService) {
    effect(() => { this.specials = this.menu.getSpecials(); });
  }
  ngOnInit() {}
  addToCart(item: MenuItem) { this.cart.addItem(item); this.toast.show(`${item.name} adicionado!`); }
}
