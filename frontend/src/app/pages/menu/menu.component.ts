import { Component, signal, computed } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MenuService, CATEGORIES } from '../../services/menu.service';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { MenuItem } from '../../models';

const CAT_ICONS: Record<string, string> = {
  'Carnes Nobres': '🥩',
  'Entradas': '🫙',
  'Peixes & Frutos do Mar': '🦞',
  'Acompanhamentos': '🥗',
  'Sobremesas': '🍮',
  'Bebidas': '🍷',
};

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [NgFor, NgIf, RouterLink],
  template: `
    <!-- Hero do cardápio -->
    <div style="background:linear-gradient(160deg,#0a0a0c 0%,#1c1917 60%,#2d1a0a 100%);padding:3.5rem 0 0;position:relative;overflow:hidden">
      <div style="position:absolute;top:-80px;right:-80px;width:400px;height:400px;border-radius:50%;background:radial-gradient(circle,rgba(245,158,11,.08),transparent 70%);pointer-events:none"></div>

      <div class="container" style="text-align:center;position:relative;z-index:1">
        <div class="section-tag" style="justify-content:center;margin-bottom:.875rem">
          <div class="section-tag-line"></div>
          <span style="color:var(--amber-400)">À LA CARTE</span>
          <div class="section-tag-line"></div>
        </div>
        <h1 style="font-family:var(--font-serif);font-size:clamp(2.5rem,5vw,4rem);color:#fff;margin-bottom:.625rem">Cardápio</h1>
        <p style="color:rgba(255,255,255,.45);font-size:.9rem;margin-bottom:2rem">{{ allItems().length }} opções selecionadas · Carnes nobres, entradas e muito mais</p>

        <!-- Search bar -->
        <div style="max-width:500px;margin:0 auto 0">
          <div style="position:relative">
            <svg style="position:absolute;left:1.125rem;top:50%;transform:translateY(-50%);color:rgba(255,255,255,.35);pointer-events:none" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
            <input
              type="text"
              placeholder="Buscar prato, ingrediente..."
              [value]="search()"
              (input)="search.set($any($event.target).value)"
              style="width:100%;padding:.875rem 1rem .875rem 3rem;border-radius:var(--r-full);border:1.5px solid rgba(255,255,255,.12);background:rgba(255,255,255,.07);color:#fff;font-size:.9rem;backdrop-filter:blur(8px);outline:none;transition:all .2s"
              onfocus="this.style.borderColor='rgba(245,158,11,.4)';this.style.background='rgba(255,255,255,.1)'"
              onblur="this.style.borderColor='rgba(255,255,255,.12)';this.style.background='rgba(255,255,255,.07)'">
            <button *ngIf="search()" (click)="search.set('')"
              style="position:absolute;right:1rem;top:50%;transform:translateY(-50%);color:rgba(255,255,255,.45);font-size:1.1rem;line-height:1;transition:color .2s"
              onmouseenter="this.style.color='#fff'" onmouseleave="this.style.color='rgba(255,255,255,.45)'">×</button>
          </div>
        </div>
      </div>

      <!-- Category nav -->
      <div style="margin-top:2rem;border-top:1px solid rgba(255,255,255,.06)">
        <div class="container" style="padding-top:0;padding-bottom:0">
          <div style="display:flex;gap:0;overflow-x:auto;scrollbar-width:none" class="cat-tab-row">
            <button *ngFor="let cat of categories"
              (click)="selectCategory(cat)"
              [class.cat-tab-active]="selectedCategory()===cat && !search()"
              class="cat-tab"
              style="flex-shrink:0;display:flex;align-items:center;gap:.5rem;padding:.875rem 1.25rem;font-size:.82rem;font-weight:500;white-space:nowrap;border-bottom:2px solid transparent;color:rgba(255,255,255,.4);transition:all .2s;background:none">
              <span>{{ catIcon(cat) }}</span>
              <span>{{ cat }}</span>
              <span style="font-size:.7rem;opacity:.6">({{ countByCategory(cat) }})</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Skeleton loading -->
    <div *ngIf="loading()" style="background:var(--stone-50);padding:2.5rem 0 4rem">
      <div class="container">
        <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:1.375rem">
          <div *ngFor="let s of skeletons" class="menu-card-premium" style="border-radius:var(--r-xl);overflow:hidden;background:#fff">
            <div style="aspect-ratio:16/10;background:linear-gradient(90deg,#e7e5e4 25%,#d6d3d1 50%,#e7e5e4 75%);background-size:200% 100%;animation:shimmer 1.4s infinite"></div>
            <div style="padding:1.125rem 1.25rem 1.25rem">
              <div style="height:1rem;border-radius:4px;background:linear-gradient(90deg,#e7e5e4 25%,#d6d3d1 50%,#e7e5e4 75%);background-size:200% 100%;animation:shimmer 1.4s infinite;margin-bottom:.625rem;width:70%"></div>
              <div style="height:.75rem;border-radius:4px;background:linear-gradient(90deg,#e7e5e4 25%,#d6d3d1 50%,#e7e5e4 75%);background-size:200% 100%;animation:shimmer 1.4s infinite;margin-bottom:.375rem;width:100%"></div>
              <div style="height:.75rem;border-radius:4px;background:linear-gradient(90deg,#e7e5e4 25%,#d6d3d1 50%,#e7e5e4 75%);background-size:200% 100%;animation:shimmer 1.4s infinite;margin-bottom:1rem;width:85%"></div>
              <div style="height:2.25rem;border-radius:var(--r-lg);background:linear-gradient(90deg,#e7e5e4 25%,#d6d3d1 50%,#e7e5e4 75%);background-size:200% 100%;animation:shimmer 1.4s infinite"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Items grid -->
    <div *ngIf="!loading()" style="background:var(--stone-50);padding:2.5rem 0 4rem">
      <div class="container">

        <!-- Search results header -->
        <div *ngIf="search()" style="margin-bottom:1.5rem;display:flex;align-items:center;gap:.75rem;flex-wrap:wrap">
          <h2 style="font-family:var(--font-serif);font-size:1.5rem;color:var(--stone-900)">Resultados para "<em>{{ search() }}</em>"</h2>
          <span style="background:var(--stone-200);color:var(--stone-600);padding:.2rem .65rem;border-radius:var(--r-full);font-size:.75rem;font-weight:600">{{ displayItems().length }} {{ displayItems().length===1?'item':'itens' }}</span>
        </div>

        <!-- Category header (when not searching) -->
        <div *ngIf="!search()" style="display:flex;align-items:baseline;justify-content:space-between;margin-bottom:1.75rem;flex-wrap:wrap;gap:.5rem">
          <div style="display:flex;align-items:center;gap:.75rem">
            <span style="font-size:1.75rem">{{ catIcon(selectedCategory()) }}</span>
            <h2 style="font-family:var(--font-serif);font-size:2rem;color:var(--stone-900)">{{ selectedCategory() }}</h2>
          </div>
          <p style="color:var(--stone-400);font-size:.85rem">{{ displayItems().length }} {{ displayItems().length===1?'prato':'pratos' }}</p>
        </div>

        <!-- Empty state -->
        <div *ngIf="displayItems().length===0" style="text-align:center;padding:4rem 2rem;background:#fff;border-radius:var(--r-2xl);border:1px solid var(--stone-100)">
          <div style="font-size:3.5rem;margin-bottom:1rem">🍽️</div>
          <h3 style="font-family:var(--font-serif);font-size:1.5rem;color:var(--stone-700);margin-bottom:.5rem">Nenhum item encontrado</h3>
          <p style="color:var(--stone-400);font-size:.875rem">Tente outro termo ou explore as categorias acima.</p>
        </div>

        <!-- Cards grid -->
        <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:1.375rem" class="stagger">
          <div *ngFor="let item of displayItems()" class="menu-card-premium animate-fade-up">

            <!-- Image -->
            <div style="position:relative;aspect-ratio:16/10;overflow:hidden;background:var(--stone-100)">
              <img [src]="item.image" [alt]="item.name" loading="lazy"
                   style="width:100%;height:100%;object-fit:cover;transition:transform .6s cubic-bezier(.4,0,.2,1)"
                   onerror="this.src='https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&q=80'">
              <div style="position:absolute;inset:0;background:linear-gradient(to top,rgba(0,0,0,.65) 0%,rgba(0,0,0,.1) 45%,transparent 70%)"></div>

              <!-- Tags -->
              <div style="position:absolute;top:.75rem;left:.75rem;display:flex;gap:.375rem;flex-wrap:wrap">
                <span *ngIf="item.isSpecial" style="background:var(--amber-600);color:#fff;padding:.25rem .65rem;font-size:.65rem;letter-spacing:.12em;font-weight:700;border-radius:var(--r-sm)">★ DESTAQUE</span>
              </div>

              <!-- Price overlay at bottom -->
              <div style="position:absolute;bottom:.875rem;left:.875rem;right:.875rem;display:flex;align-items:flex-end;justify-content:space-between">
                <div>
                  <p style="font-size:.6rem;color:rgba(255,255,255,.55);letter-spacing:.1em;margin-bottom:1px">A PARTIR DE</p>
                  <span style="font-family:var(--font-serif);font-size:1.5rem;color:#fff;font-weight:600;text-shadow:0 1px 4px rgba(0,0,0,.4)">R$ {{ item.price.toFixed(2) }}</span>
                </div>
                <span *ngIf="item.prepTime" style="background:rgba(0,0,0,.55);backdrop-filter:blur(4px);color:rgba(255,255,255,.8);padding:.25rem .6rem;border-radius:var(--r-full);font-size:.68rem;display:flex;align-items:center;gap:.3rem;border:1px solid rgba(255,255,255,.15)">
                  <svg width="10" height="10" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                  {{ item.prepTime }}min
                </span>
              </div>
            </div>

            <!-- Body -->
            <div style="padding:1.125rem 1.25rem 1.25rem;background:#fff">
              <h3 style="font-family:var(--font-serif);font-size:1.15rem;color:var(--stone-900);margin-bottom:.375rem;line-height:1.25">{{ item.name }}</h3>
              <p style="font-size:.8rem;color:var(--stone-500);line-height:1.65;margin-bottom:.75rem;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden">{{ item.description }}</p>

              <!-- Tags dietéticas -->
              <div *ngIf="item.tags && item.tags.length" style="display:flex;flex-wrap:wrap;gap:.3rem;margin-bottom:.875rem">
                <span *ngFor="let tag of item.tags"
                  style="font-size:.62rem;font-weight:600;letter-spacing:.08em;padding:.2rem .55rem;border-radius:var(--r-full);background:#f0fdf4;color:#15803d;border:1px solid #bbf7d0;text-transform:uppercase">
                  {{ tag }}
                </span>
              </div>

              <!-- Stepper se já está no carrinho, botão se não está -->
              <div *ngIf="getQty(item.id) === 0">
                <button class="btn btn-primary btn-sm" (click)="addToCart(item)"
                        style="width:100%;justify-content:center;gap:.5rem;border-radius:var(--r-lg)">
                  <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>
                  Adicionar ao Pedido
                </button>
              </div>
              <div *ngIf="getQty(item.id) > 0"
                   style="display:flex;align-items:center;justify-content:space-between;background:var(--stone-900);border-radius:var(--r-lg);padding:.25rem .375rem">
                <button (click)="decrease(item)"
                  style="width:2rem;height:2rem;border-radius:var(--r-md);background:rgba(255,255,255,.1);color:#fff;font-size:1.1rem;display:flex;align-items:center;justify-content:center;transition:background .15s;border:none;cursor:pointer"
                  onmouseenter="this.style.background='rgba(255,255,255,.2)'" onmouseleave="this.style.background='rgba(255,255,255,.1)'">−</button>
                <span style="color:#fff;font-weight:700;font-size:.95rem;min-width:1.5rem;text-align:center">{{ getQty(item.id) }}</span>
                <button (click)="increase(item)"
                  style="width:2rem;height:2rem;border-radius:var(--r-md);background:var(--amber-600);color:#fff;font-size:1.1rem;display:flex;align-items:center;justify-content:center;transition:background .15s;border:none;cursor:pointer"
                  onmouseenter="this.style.background='var(--amber-700)'" onmouseleave="this.style.background='var(--amber-600)'">+</button>
              </div>
            </div>

          </div>
        </div>

      </div>
    </div>

    <!-- Mobile floating cart button -->
    <div *ngIf="cartCount > 0" style="position:fixed;bottom:1.5rem;right:1.5rem;z-index:100" class="animate-fade-up">
      <a routerLink="/carrinho"
         style="display:flex;align-items:center;gap:.625rem;padding:.75rem 1.25rem;background:var(--stone-900);color:#fff;border-radius:var(--r-full);box-shadow:0 8px 24px rgba(0,0,0,.4),0 2px 6px rgba(0,0,0,.3);transition:all .2s;text-decoration:none;font-size:.875rem;font-weight:600"
         onmouseenter="this.style.transform='translateY(-2px)';this.style.background='var(--stone-800)'"
         onmouseleave="this.style.transform='';this.style.background='var(--stone-900)'">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
          <path d="M1 1h4l2.68 13.39a2 2 0 001.98 1.61h9.72a2 2 0 001.98-1.61L23 6H6"/>
        </svg>
        <span>Ver Carrinho</span>
        <span style="background:var(--amber-500);color:#fff;border-radius:var(--r-full);min-width:22px;height:22px;display:flex;align-items:center;justify-content:center;font-size:.72rem;font-weight:700;padding:0 .25rem">{{ cartCount }}</span>
      </a>
    </div>
  `,
  styles: [`
    @keyframes spin { to { transform:rotate(360deg); } }
    @keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }
    .cat-tab-row::-webkit-scrollbar { display:none; }
    .cat-tab { border-bottom: 2px solid transparent; }
    .cat-tab-active { color: var(--amber-400) !important; border-bottom-color: var(--amber-400) !important; }
    .cat-tab:hover:not(.cat-tab-active) { color:rgba(255,255,255,.7) !important; }
    .menu-card-premium { border-radius: var(--r-xl); overflow:hidden; box-shadow: 0 2px 8px rgba(0,0,0,.07); transition: transform .3s cubic-bezier(.4,0,.2,1), box-shadow .3s; }
    .menu-card-premium:hover { transform: translateY(-6px); box-shadow: 0 16px 40px rgba(0,0,0,.14); }
    .menu-card-premium:hover img { transform: scale(1.06); }
  `]
})
export class MenuComponent {
  categories = CATEGORIES;
  selectedCategory = signal(CATEGORIES[0]);
  search = signal('');
  cartCount = 0;
  cartQtyMap = signal<Map<string, number>>(new Map());

  skeletons = Array(6).fill(0);
  get allItems() { return this.menuService.items$; }
  loading = computed(() => this.menuService.items$().length === 0);

  displayItems = computed(() => {
    const q = this.search().toLowerCase().trim();
    if (q) {
      return this.allItems().filter(i =>
        i.active !== false && (
          i.name.toLowerCase().includes(q) ||
          i.description.toLowerCase().includes(q) ||
          i.category.toLowerCase().includes(q)
        )
      );
    }
    return this.allItems().filter(i => i.active !== false && i.category === this.selectedCategory());
  });

  constructor(private menuService: MenuService, private cart: CartService, private toast: ToastService) {
    this.cart.count.subscribe(c => this.cartCount = c);
    this.cart.items.subscribe(items =>
      this.cartQtyMap.set(new Map(items.map(i => [i.id, i.quantity])))
    );
  }

  selectCategory(cat: string) { this.selectedCategory.set(cat); this.search.set(''); }
  catIcon(cat: string) { return CAT_ICONS[cat] ?? '🍽️'; }
  countByCategory(cat: string) { return this.allItems().filter(i => i.active !== false && i.category === cat).length; }
  getQty(id: string) { return this.cartQtyMap().get(id) ?? 0; }

  addToCart(item: MenuItem) {
    this.cart.addItem(item);
    this.toast.show(`${item.name} adicionado! 🛒`);
  }
  increase(item: MenuItem) { this.cart.addItem(item); }
  decrease(item: MenuItem) { this.cart.updateQuantity(item.id, this.getQty(item.id) - 1); }
}
