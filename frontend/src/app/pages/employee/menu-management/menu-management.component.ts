import { Component, OnDestroy, signal } from '@angular/core';
import { NgFor, NgIf, CurrencyPipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MenuService } from '../../../services/menu.service';
import { ToastService } from '../../../services/toast.service';
import { Category, DishStatus, MenuItem, MenuSummary, TechnicalSheet } from '../../../models';
import { TechnicalSheetModalComponent, FOOD_COST_COLORS } from './technical-sheet-modal.component';
import { CategoryManagerComponent } from './category-manager.component';

interface MenuForm {
  name: string;
  description: string;
  price: number | null;
  category: string;
  image: string;
  isSpecial: boolean;
  prepTime: number | null;
  preparationMethod: string;
}

const EMPTY_FORM: MenuForm = {
  name: '', description: '', price: null,
  category: '', image: '',
  isSpecial: false, prepTime: null, preparationMethod: '',
};

const PAGE_SIZE = 12;

const STATUS_FILTERS: { value: DishStatus | ''; label: string }[] = [
  { value: '',         label: 'Todos' },
  { value: 'active',   label: 'Ativos' },
  { value: 'paused',   label: 'Pausados' },
  { value: 'inactive', label: 'Inativos' },
];

@Component({
  selector: 'app-menu-management',
  standalone: true,
  imports: [NgFor, NgIf, CurrencyPipe, DecimalPipe, FormsModule, TechnicalSheetModalComponent, CategoryManagerComponent],
  template: `
    <div style="max-width:1280px;margin:0 auto;padding:0 1.5rem">

      <!-- Page header -->
      <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:2rem;flex-wrap:wrap;gap:1rem" class="animate-fade-up">
        <div>
          <h1 style="font-family:var(--font-display);font-size:1.875rem;color:var(--text-primary);margin-bottom:.25rem">
            Cardápio
          </h1>
          <p style="color:var(--text-tertiary);font-size:var(--text-base)">
            Gerencie pratos, categorias, fichas técnicas e food cost
          </p>
        </div>
        <div style="display:flex;gap:.5rem;flex-wrap:wrap">
          <button class="btn btn-ghost" (click)="showCategories.set(true)">Categorias</button>
          <button class="btn btn-primary" (click)="openAdd()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Novo Item
          </button>
        </div>
      </div>

      <!-- Toolbar -->
      <div style="display:flex;align-items:center;gap:1rem;margin-bottom:1rem;flex-wrap:wrap">
        <!-- Search -->
        <div class="search-input-wrapper" style="flex:1;min-width:220px;max-width:360px">
          <div class="search-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
          </div>
          <input class="form-input" type="text" placeholder="Buscar pelo nome..." aria-label="Buscar prato pelo nome"
                 [ngModel]="search" (ngModelChange)="onSearch($event)">
        </div>

        <!-- Category filter: width:0 impede as pílulas de alargar a página no tablet (RNF09) -->
        <div class="cat-nav dark-pills" style="flex:1 1 260px;width:0;min-width:0">
          <button class="cat-pill" [class.active]="!activeCategory" (click)="setCategory('')">Todas</button>
          <button *ngFor="let c of categories()" class="cat-pill" [class.active]="activeCategory === c.name"
                  (click)="setCategory(c.name)" [style.opacity]="c.status === 'inactive' ? '.55' : '1'">
            {{ c.name }}
          </button>
        </div>
      </div>

      <!-- Status filter -->
      <div style="display:flex;gap:.5rem;margin-bottom:1.5rem;flex-wrap:wrap" role="group" aria-label="Filtrar por status">
        <button *ngFor="let f of statusFilters" class="cat-pill" [class.active]="statusFilter === f.value" (click)="setStatus(f.value)">
          {{ f.label }}
        </button>
      </div>

      <!-- Stats row -->
      <div style="display:flex;gap:1rem;margin-bottom:1.5rem;flex-wrap:wrap" class="stagger">
        <div *ngFor="let s of statCards"
             style="flex:1;min-width:140px;padding:1rem 1.25rem;background:var(--bg-card);border:1px solid var(--border);border-radius:var(--r-xl)">
          <div style="font-size:var(--text-xs);color:var(--text-tertiary);letter-spacing:.08em;text-transform:uppercase;margin-bottom:.25rem">{{ s.label }}</div>
          <div style="font-family:var(--font-display);font-size:1.5rem;color:var(--text-primary)" [style.color]="s.color">{{ s.value }}</div>
        </div>
      </div>

      <!-- Load error -->
      <div *ngIf="loadError()" role="alert"
           style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:1rem;margin-bottom:1.5rem;color:var(--danger-text);display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap">
        <span>{{ loadError() }}</span>
        <button class="btn btn-ghost btn-sm" (click)="reload()">Tentar de novo</button>
      </div>

      <!-- Grid of items -->
      <div *ngIf="!loading()" class="stagger"
           style="display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:1.25rem">

        <div *ngFor="let item of items(); trackBy: trackById"
             class="menu-card-admin"
             [style.opacity]="item.status === 'inactive' ? '.6' : '1'">

          <!-- Image -->
          <div class="menu-card-img">
            <img [src]="item.image" [alt]="item.name" loading="lazy"
                 style="width:100%;height:100%;object-fit:cover"
                 onerror="this.src='https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400&q=60'">
            <div *ngIf="item.status !== 'active'" class="inactive-overlay">{{ item.status === 'paused' ? 'PAUSADO' : 'INATIVO' }}</div>
            <div *ngIf="item.isSpecial" style="position:absolute;top:.75rem;left:.75rem;background:var(--brand-600);color:#fff;padding:.2rem .6rem;font-size:.65rem;letter-spacing:.1em;font-weight:600;border-radius:var(--r-sm)">
              DESTAQUE
            </div>
          </div>

          <!-- Body -->
          <div class="menu-card-body">
            <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:.5rem;margin-bottom:.375rem">
              <h3 style="font-size:var(--text-base);font-weight:600;color:var(--text-primary);line-height:1.3">
                {{ item.name }}
              </h3>
              <span style="font-family:var(--font-display);font-size:1.1rem;color:var(--brand-400);flex-shrink:0;font-weight:600">
                {{ item.price | currency:'BRL':'symbol':'1.2-2' }}
              </span>
            </div>

            <div style="margin-bottom:.625rem;display:flex;flex-wrap:wrap;gap:.375rem">
              <span class="chip">{{ item.category }}</span>
              <span *ngIf="item.prepTime" class="chip">⏱ {{ item.prepTime }}min</span>
            </div>

            <!-- Food cost (RF-013) -->
            <div *ngIf="item.hasTechnicalSheet && item.foodCostLevel as level" class="food-cost"
                 [style.background]="colors[level].bg" [style.color]="colors[level].text" [style.border]="'1px solid ' + colors[level].border"
                 [attr.title]="item.foodCostWarning ? 'Food cost acima de 35% (RN02)' : null">
              <span>Custo {{ item.cost | currency:'BRL':'symbol':'1.2-2' }}</span>
              <strong>Food cost {{ item.foodCost | number:'1.2-2' }}%{{ item.foodCostWarning ? ' ⚠' : '' }}</strong>
            </div>
            <div *ngIf="!item.hasTechnicalSheet" class="food-cost"
                 style="background:var(--neutral-bg);color:var(--neutral-text);border:1px dashed var(--neutral-border)">
              <span>Sem ficha técnica</span>
              <strong>não pode ser ativado</strong>
            </div>

            <p style="font-size:var(--text-sm);color:var(--text-tertiary);line-height:1.55;margin:.75rem 0 1rem;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical">
              {{ item.description }}
            </p>

            <!-- Actions -->
            <div style="display:flex;align-items:center;gap:.5rem;flex-wrap:wrap">
              <button class="btn btn-ghost btn-sm" (click)="openEdit(item)" style="flex:1;justify-content:center">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                Editar
              </button>

              <button class="btn btn-ghost btn-sm" (click)="sheetDish.set(item)" style="flex:1;justify-content:center">
                Ficha técnica
              </button>

              <button *ngIf="item.status === 'active'" class="btn btn-sm" (click)="changeStatus(item, 'paused')"
                      style="background:var(--neutral-bg);color:var(--neutral-text);border:1px solid var(--neutral-border)">
                Pausar
              </button>
              <button *ngIf="item.status === 'paused'" class="btn btn-sm" (click)="changeStatus(item, 'active')"
                      style="background:var(--success-bg);color:var(--success-text);border:1px solid var(--success-border)">
                Ativar
              </button>
              <button *ngIf="item.status === 'inactive'" class="btn btn-sm" (click)="changeStatus(item, 'paused')"
                      style="background:var(--info-bg);color:var(--info-text);border:1px solid var(--info-border)">
                Restaurar
              </button>

              <button *ngIf="item.status !== 'inactive'" class="btn btn-sm" [attr.aria-label]="'Remover ' + item.name"
                      style="background:var(--danger-bg);color:var(--danger-text);border:1px solid var(--danger-border)"
                      (click)="deleteTarget.set(item)">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6M10 11v6M14 11v6M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Empty state -->
        <div *ngIf="items().length === 0 && !loadError()" style="grid-column:1/-1">
          <div class="empty-state">
            <div class="empty-icon">🍽️</div>
            <h3>Nenhum item encontrado</h3>
            <p>Tente ajustar os filtros ou adicione um novo item ao cardápio.</p>
            <button class="btn btn-primary" style="margin-top:1.25rem" (click)="openAdd()">
              Adicionar Item
            </button>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <footer *ngIf="!loading() && totalElements() > 0"
              style="display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap;padding:1.25rem 0">
        <span style="font-size:var(--text-sm);color:var(--text-tertiary)">
          {{ totalElements() }} prato(s) · Página {{ page() + 1 }} de {{ totalPages() }}
        </span>
        <div style="display:flex;gap:.5rem">
          <button class="btn btn-ghost btn-sm" type="button" [disabled]="page() === 0" (click)="goTo(page() - 1)">Anterior</button>
          <button class="btn btn-ghost btn-sm" type="button" [disabled]="page() + 1 >= totalPages()" (click)="goTo(page() + 1)">Próxima</button>
        </div>
      </footer>

      <!-- Skeleton loading -->
      <div *ngIf="loading()" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:1.25rem" aria-busy="true">
        <div *ngFor="let _ of [1,2,3,4,5,6]"
             style="background:var(--bg-card);border:1px solid var(--border);border-radius:var(--r-xl);overflow:hidden">
          <div class="skeleton" style="height:180px;border-radius:0"></div>
          <div style="padding:1rem">
            <div class="skeleton skeleton-text" style="height:16px;width:70%;margin-bottom:.75rem"></div>
            <div class="skeleton skeleton-text" style="height:12px;width:50%;margin-bottom:.5rem"></div>
            <div class="skeleton skeleton-text" style="height:12px;width:90%;margin-bottom:.5rem"></div>
            <div class="skeleton skeleton-text" style="height:12px;width:75%"></div>
          </div>
        </div>
      </div>

    </div>

    <!-- ── Add / Edit Modal ─────────────────────────────── -->
    <div *ngIf="showModal()" class="modal-overlay" (click)="closeModal()">
      <div class="modal" style="max-width:580px" (click)="$event.stopPropagation()">

        <div class="modal-header">
          <h3>{{ editingId ? 'Editar Item' : 'Novo Item do Cardápio' }}</h3>
          <button class="btn btn-ghost btn-icon" (click)="closeModal()" aria-label="Fechar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <div class="modal-body" style="display:flex;flex-direction:column;gap:1.25rem">

          <!-- Image preview -->
          <div *ngIf="form.image"
               style="width:100%;height:160px;border-radius:var(--r-lg);overflow:hidden;border:1px solid var(--border)">
            <img [src]="form.image" alt="" style="width:100%;height:100%;object-fit:cover"
                 onerror="this.style.display='none'">
          </div>

          <p *ngIf="!editingId" style="font-size:var(--text-sm);color:var(--text-tertiary);line-height:1.5">
            O prato novo fica <strong>pausado</strong> até ter ficha técnica (RN01). Depois de salvar, você já monta a ficha.
          </p>

          <div class="item-form-grid">
            <div class="form-group" style="grid-column:1/-1">
              <label class="form-label" for="item-name">Nome do item *</label>
              <input id="item-name" class="form-input" [(ngModel)]="form.name" placeholder="Ex: Wagyu A5 Ribeye 200g">
            </div>

            <div class="form-group" style="grid-column:1/-1">
              <label class="form-label" for="item-description">Descrição *</label>
              <textarea id="item-description" class="form-input" [(ngModel)]="form.description" rows="3" placeholder="Descreva os ingredientes e diferenciais do prato..."></textarea>
            </div>

            <div class="form-group">
              <label class="form-label" for="item-price">Preço (R$) *</label>
              <input id="item-price" class="form-input" type="number" [(ngModel)]="form.price" placeholder="0,00" min="0" step="0.01">
            </div>

            <div class="form-group">
              <label class="form-label" for="item-category">Categoria *</label>
              <select id="item-category" class="form-input" [(ngModel)]="form.category">
                <option *ngFor="let c of formCategories()" [value]="c">{{ c }}</option>
              </select>
            </div>

            <div class="form-group" style="grid-column:1/-1">
              <label class="form-label" for="item-image">URL da imagem</label>
              <input id="item-image" class="form-input" [(ngModel)]="form.image" placeholder="https://...">
            </div>

            <div class="form-group">
              <label class="form-label" for="item-prep">Tempo de preparo (min)</label>
              <input id="item-prep" class="form-input" type="number" [(ngModel)]="form.prepTime" placeholder="Ex: 20" min="1">
            </div>

            <div class="form-group" style="display:flex;align-items:center;justify-content:space-between;flex-direction:row;padding:.75rem 1rem;background:var(--bg-elevated);border-radius:var(--r-lg);border:1px solid var(--border)">
              <div>
                <div style="font-size:var(--text-base);font-weight:500;color:var(--text-secondary)">Item em Destaque</div>
                <div style="font-size:var(--text-xs);color:var(--text-tertiary)">Aparece na seção de destaques</div>
              </div>
              <label class="toggle">
                <input type="checkbox" [(ngModel)]="form.isSpecial" aria-label="Item em destaque">
                <div class="toggle-track"></div>
                <div class="toggle-thumb"></div>
              </label>
            </div>

            <!-- RF-014 -->
            <div class="form-group" style="grid-column:1/-1">
              <label class="form-label" for="item-method">Modo de preparo</label>
              <textarea id="item-method" class="form-input" [(ngModel)]="form.preparationMethod" rows="4"
                        placeholder="Passo a passo para a cozinha: 1. Selar a carne... 2. ..."></textarea>
            </div>
          </div>

          <!-- Form error -->
          <div *ngIf="formError" role="alert"
               style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:.75rem 1rem;font-size:var(--text-sm);color:var(--danger-text);display:flex;align-items:center;gap:.5rem">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="flex-shrink:0">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {{ formError }}
          </div>

        </div>

        <div class="modal-footer">
          <button class="btn btn-ghost" (click)="closeModal()">Cancelar</button>
          <button class="btn btn-primary" (click)="saveItem()" [disabled]="saving()">
            <span *ngIf="saving()" style="width:14px;height:14px;border:2px solid rgba(255,255,255,.3);border-top-color:#fff;border-radius:50%;animation:spin 1s linear infinite"></span>
            {{ saving() ? 'Salvando...' : (editingId ? 'Salvar Alterações' : 'Adicionar ao Cardápio') }}
          </button>
        </div>
      </div>
    </div>

    <!-- ── Delete Confirm Modal ─────────────────────────── -->
    <div *ngIf="deleteTarget()" class="modal-overlay" (click)="deleteTarget.set(null)">
      <div class="modal" style="max-width:420px" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Remover Item</h3>
        </div>
        <div class="modal-body">
          <p style="color:var(--text-secondary);line-height:1.65">
            Remover <strong style="color:var(--text-primary)">{{ deleteTarget()?.name }}</strong> do cardápio?
            O prato fica inativo e some para o cliente, mas continua salvo: dá para restaurar depois.
          </p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" (click)="deleteTarget.set(null)">Cancelar</button>
          <button class="btn btn-danger" (click)="deleteItem()">Remover Item</button>
        </div>
      </div>
    </div>

    <app-technical-sheet-modal *ngIf="sheetDish() as dish" [dish]="dish"
                               (close)="sheetDish.set(null)" (saved)="onSheetSaved($event)">
    </app-technical-sheet-modal>

    <app-category-manager *ngIf="showCategories()" (close)="showCategories.set(false)" (changed)="onCategoriesChanged()">
    </app-category-manager>
  `,
  styles: [`
    @keyframes spin { to { transform:rotate(360deg); } }
    .food-cost { display:flex; justify-content:space-between; gap:.5rem; flex-wrap:wrap; padding:.4rem .625rem; border-radius:var(--r-md); font-size:var(--text-xs); }
    .item-form-grid { display:grid; grid-template-columns:1fr 1fr; gap:1.25rem; }
    @media (max-width: 560px) { .item-form-grid { grid-template-columns:1fr; } }
  `]
})
export class MenuManagementComponent implements OnDestroy {
  readonly colors = FOOD_COST_COLORS;
  readonly statusFilters = STATUS_FILTERS;

  items         = signal<MenuItem[]>([]);
  loading       = signal(true);
  loadError     = signal('');
  page          = signal(0);
  totalPages    = signal(0);
  totalElements = signal(0);
  summary       = signal<MenuSummary | null>(null);
  categories    = signal<Category[]>([]);

  showModal      = signal(false);
  saving         = signal(false);
  deleteTarget   = signal<MenuItem | null>(null);
  sheetDish      = signal<MenuItem | null>(null);
  showCategories = signal(false);

  search = '';
  activeCategory = '';
  statusFilter: DishStatus | '' = '';
  editingId: string | null = null;
  formError = '';
  form: MenuForm = { ...EMPTY_FORM };

  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor(private menuService: MenuService, private toast: ToastService) {
    this.reload();
    this.loadCategories();
  }

  ngOnDestroy() { clearTimeout(this.searchTimer); }

  // Só categorias ativas aceitam prato novo; na edição, mantém a atual mesmo se inativa
  formCategories(): string[] {
    const active = this.categories().filter(c => c.status === 'active').map(c => c.name);
    return this.form.category && !active.includes(this.form.category) ? [this.form.category, ...active] : active;
  }

  get statCards() {
    const s = this.summary();
    return [
      { label: 'Total de Itens', value: s?.total ?? '—',    color: 'var(--text-primary)' },
      { label: 'Ativos',         value: s?.active ?? '—',   color: 'var(--success-text)' },
      { label: 'Pausados',       value: s?.paused ?? '—',   color: 'var(--warning-text)' },
      { label: 'Inativos',       value: s?.inactive ?? '—', color: 'var(--neutral-text)' },
      { label: 'Categorias',     value: this.categories().filter(c => c.status === 'active').length, color: 'var(--info-text)' },
    ];
  }

  trackById(_: number, item: MenuItem) { return item.id; }

  // ── Carregamento e filtros ─────────────────────────────────

  async reload(page = this.page()): Promise<void> {
    this.loading.set(true);
    this.loadError.set('');
    try {
      const [result, summary] = await Promise.all([
        this.menuService.listAdmin({
          page, size: PAGE_SIZE, category: this.activeCategory, status: this.statusFilter, search: this.search,
        }),
        this.menuService.summary(),
      ]);
      // Se a página ficou vazia (ex.: removeu o último item dela), volta uma
      if (result.content.length === 0 && page > 0) return this.reload(page - 1);
      this.items.set(result.content);
      this.page.set(result.number);
      this.totalPages.set(result.totalPages);
      this.totalElements.set(result.totalElements);
      this.summary.set(summary);
    } catch (e) {
      this.items.set([]);
      this.totalElements.set(0);
      this.loadError.set(e instanceof Error ? e.message : 'Erro ao carregar o cardápio.');
    } finally {
      this.loading.set(false);
    }
  }

  async loadCategories() {
    try {
      this.categories.set((await this.menuService.listAdminCategories()).content);
    } catch {
      this.categories.set([]);
    }
  }

  goTo(page: number) { this.reload(page); }

  setCategory(name: string) {
    this.activeCategory = name;
    this.reload(0);
  }

  setStatus(status: DishStatus | '') {
    this.statusFilter = status;
    this.reload(0);
  }

  onSearch(value: string) {
    this.search = value;
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.reload(0), 300);
  }

  onCategoriesChanged() {
    this.loadCategories();
    this.reload();
  }

  // ── Criar / editar ─────────────────────────────────────────

  openAdd() {
    this.editingId = null;
    this.form = { ...EMPTY_FORM, category: this.formCategories()[0] ?? '' };
    this.formError = '';
    this.showModal.set(true);
  }

  openEdit(item: MenuItem) {
    this.editingId = item.id;
    this.form = {
      name:              item.name,
      description:       item.description ?? '',
      price:             item.price,
      category:          item.category,
      image:             item.image ?? '',
      isSpecial:         !!item.isSpecial,
      prepTime:          item.prepTime ?? null,
      preparationMethod: item.preparationMethod ?? '',
    };
    this.formError = '';
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.editingId = null;
    this.form = { ...EMPTY_FORM };
    this.formError = '';
  }

  async saveItem() {
    if (!this.form.name.trim()) { this.formError = 'O nome do item é obrigatório.'; return; }
    if (!this.form.description.trim()) { this.formError = 'A descrição é obrigatória.'; return; }
    if (!this.form.price || this.form.price <= 0) { this.formError = 'Informe um preço válido.'; return; }
    if (!this.form.category) { this.formError = 'Escolha uma categoria (crie uma em "Categorias").'; return; }

    this.formError = '';
    this.saving.set(true);

    const payload = {
      name:              this.form.name.trim(),
      description:       this.form.description.trim(),
      price:             Number(this.form.price),
      category:          this.form.category,
      image:             this.form.image.trim() || 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&q=85',
      isSpecial:         this.form.isSpecial,
      prepTime:          this.form.prepTime ? Number(this.form.prepTime) : undefined,
      preparationMethod: this.form.preparationMethod.trim(),
    };

    try {
      if (this.editingId) {
        await this.menuService.updateItem(this.editingId, payload);
        this.toast.show('Item atualizado com sucesso!');
        this.closeModal();
      } else {
        const created = await this.menuService.addItem(payload);
        this.toast.show('Prato criado como pausado. Monte a ficha técnica para ativar.');
        this.closeModal();
        this.sheetDish.set(created);
      }
      this.reload();
    } catch (e) {
      this.formError = e instanceof Error ? e.message : 'Erro ao salvar. Verifique a conexão e tente novamente.';
    } finally {
      this.saving.set(false);
    }
  }

  // ── Status, exclusão e ficha técnica ───────────────────────

  async changeStatus(item: MenuItem, status: DishStatus) {
    const messages: Record<DishStatus, string> = {
      active: `"${item.name}" ativado`,
      paused: item.status === 'inactive' ? `"${item.name}" restaurado como pausado` : `"${item.name}" pausado`,
      inactive: `"${item.name}" inativado`,
    };
    try {
      await this.menuService.setStatus(item.id, status);
      this.toast.show(messages[status]);
      this.reload();
    } catch (e) {
      this.toast.show(e instanceof Error ? e.message : 'Erro ao atualizar o item.', 'error');
    }
  }

  async deleteItem() {
    const target = this.deleteTarget();
    if (!target) return;
    try {
      await this.menuService.deleteItem(target.id);
      this.toast.show(`"${target.name}" removido do cardápio`);
      this.reload();
    } catch (e) {
      this.toast.show(e instanceof Error ? e.message : 'Erro ao remover o item.', 'error');
    } finally {
      this.deleteTarget.set(null);
    }
  }

  onSheetSaved(sheet: TechnicalSheet) {
    this.sheetDish.set(null);
    const warning = sheet.foodCostWarning ? ' Atenção: food cost acima de 35%.' : '';
    this.toast.show(sheet.items.length
      ? `Ficha técnica salva. Food cost ${sheet.foodCost?.toFixed(2)}%.${warning}`
      : 'Ficha técnica vazia: o prato foi pausado (RN01).', sheet.foodCostWarning ? 'error' : undefined);
    this.reload();
  }
}
