import { Component, EventEmitter, Input, OnInit, Output, computed, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe, NgFor, NgIf } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MenuService } from '../../../services/menu.service';
import { Ingrediente, IngredienteService } from '../../../services/ingrediente.service';
import { FoodCostLevel, MenuItem, TechnicalSheet } from '../../../models';

// Mesmas faixas do CustoPratoService (backend): verde até 30%, amarelo até 35% (RN02), vermelho acima
const LIMITE_VERDE = 30;
const LIMITE_ALERTA = 35;

export function foodCostLevel(foodCost: number): FoodCostLevel {
  if (foodCost <= LIMITE_VERDE) return 'green';
  return foodCost <= LIMITE_ALERTA ? 'yellow' : 'red';
}

export const FOOD_COST_COLORS: Record<FoodCostLevel, { bg: string; text: string; border: string; label: string }> = {
  green:  { bg: 'var(--success-bg)', text: 'var(--success-text)', border: 'var(--success-border)', label: 'Saudável' },
  yellow: { bg: 'var(--warning-bg)', text: 'var(--warning-text)', border: 'var(--warning-border)', label: 'Atenção' },
  red:    { bg: 'var(--danger-bg)',  text: 'var(--danger-text)',  border: 'var(--danger-border)',  label: 'Acima de 35%' },
};

type ItemForm = FormGroup<{
  ingredientId: FormControl<number | null>;
  quantity: FormControl<number | null>;
  correctionFactor: FormControl<number | null>;
}>;

@Component({
  selector: 'app-technical-sheet-modal',
  standalone: true,
  imports: [NgFor, NgIf, CurrencyPipe, DecimalPipe, ReactiveFormsModule],
  template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="modal" style="max-width:760px" (click)="$event.stopPropagation()">

        <div class="modal-header">
          <div>
            <h3>Ficha técnica</h3>
            <div style="font-size:var(--text-sm);color:var(--text-tertiary);margin-top:.125rem">
              {{ dish.name }} · preço de venda {{ dish.price | currency:'BRL':'symbol':'1.2-2' }}
            </div>
          </div>
          <button class="btn btn-ghost btn-icon" type="button" (click)="close.emit()" aria-label="Fechar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <div class="modal-body" style="display:flex;flex-direction:column;gap:1rem">

          <div *ngIf="loading()" style="padding:2rem;text-align:center;color:var(--text-tertiary)">Carregando ficha técnica...</div>

          <ng-container *ngIf="!loading()">
            <p *ngIf="ingredients().length === 0" style="color:var(--warning-text);font-size:var(--text-sm)">
              Nenhum ingrediente cadastrado. Cadastre em <strong>Gerente → Ingredientes</strong> antes de montar a ficha.
            </p>

            <!-- Linhas da ficha (FormArray) -->
            <form [formGroup]="form" (ngSubmit)="save()" style="display:flex;flex-direction:column;gap:.625rem">
              <div class="sheet-row sheet-head">
                <span>Ingrediente</span><span>Quantidade</span><span title="Fator de correção (RN08: mínimo 1,0)">Fator</span><span style="text-align:right">Custo</span><span></span>
              </div>

              <div formArrayName="items" style="display:flex;flex-direction:column;gap:.5rem">
                <div *ngFor="let row of items.controls; let i = index" [formGroupName]="i" class="sheet-row">
                  <select class="form-input" formControlName="ingredientId" [attr.aria-label]="'Ingrediente da linha ' + (i + 1)">
                    <option [ngValue]="null" disabled>Escolha...</option>
                    <option *ngFor="let ing of ingredients()" [ngValue]="ing.id">{{ ing.nome }} ({{ ing.unidadeMedida }})</option>
                  </select>
                  <div style="display:flex;align-items:center;gap:.375rem">
                    <input class="form-input" type="number" step="0.001" min="0" formControlName="quantity" placeholder="0,000"
                           [attr.aria-label]="'Quantidade da linha ' + (i + 1)">
                    <span style="font-size:var(--text-xs);color:var(--text-tertiary);min-width:1.5rem">{{ unitOf(i) }}</span>
                  </div>
                  <input class="form-input" type="number" step="0.01" min="1" formControlName="correctionFactor"
                         [attr.aria-label]="'Fator de correção da linha ' + (i + 1)">
                  <span style="text-align:right;font-variant-numeric:tabular-nums;color:var(--text-secondary)">
                    {{ rowCost(i) | currency:'BRL':'symbol':'1.2-2' }}
                  </span>
                  <button type="button" class="btn btn-ghost btn-icon" (click)="removeRow(i)" [attr.aria-label]="'Remover linha ' + (i + 1)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </button>
                  <div *ngIf="rowError(i) as err" class="sheet-error">{{ err }}</div>
                </div>
              </div>

              <button type="button" class="btn btn-ghost btn-sm" style="align-self:flex-start" (click)="addRow()"
                      [disabled]="ingredients().length === 0">
                + Adicionar ingrediente
              </button>
            </form>

            <!-- Resultado: custo e food cost (RF-012, RF-013) -->
            <div style="display:flex;gap:1rem;flex-wrap:wrap;padding:1rem;border-radius:var(--r-lg);border:1px solid var(--border);background:var(--bg-elevated)">
              <div style="flex:1;min-width:140px">
                <div class="sheet-label">Custo do prato</div>
                <div class="sheet-value">{{ preview().cost | currency:'BRL':'symbol':'1.2-2' }}</div>
              </div>
              <div style="flex:1;min-width:140px">
                <div class="sheet-label">Food cost</div>
                <div class="sheet-value" *ngIf="preview().level as level" [style.color]="colors[level].text">
                  {{ preview().foodCost | number:'1.2-2' }}%
                </div>
                <div class="sheet-value" *ngIf="!preview().level" style="color:var(--text-tertiary)">—</div>
              </div>
              <div *ngIf="preview().level as level" style="align-self:center">
                <span class="badge" [style.background]="colors[level].bg" [style.color]="colors[level].text"
                      [style.border]="'1px solid ' + colors[level].border">{{ colors[level].label }}</span>
              </div>
            </div>

            <!-- RN02 -->
            <div *ngIf="preview().warning" role="alert"
                 style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:.75rem 1rem;font-size:var(--text-sm);color:var(--danger-text)">
              Food cost acima de 35%: o prato está dando pouca margem. Revise as quantidades ou o preço de venda.
            </div>

            <p *ngIf="items.length === 0" style="font-size:var(--text-sm);color:var(--text-tertiary)">
              Sem ingredientes, o prato não pode ficar ativo no cardápio (RN01). Salvar uma ficha vazia pausa o prato.
            </p>

            <div *ngIf="error()" role="alert"
                 style="background:var(--danger-bg);border:1px solid var(--danger-border);border-radius:var(--r-lg);padding:.75rem 1rem;font-size:var(--text-sm);color:var(--danger-text)">
              {{ error() }}
            </div>
          </ng-container>
        </div>

        <div class="modal-footer">
          <button class="btn btn-ghost" type="button" (click)="close.emit()">Cancelar</button>
          <button class="btn btn-primary" type="button" (click)="save()" [disabled]="saving() || loading()">
            {{ saving() ? 'Salvando...' : 'Salvar ficha técnica' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .sheet-row { display:grid; grid-template-columns: minmax(0,2.2fr) minmax(0,1.3fr) minmax(0,.8fr) minmax(0,1fr) 2.25rem; gap:.5rem; align-items:center; }
    .sheet-head { font-size:var(--text-xs); color:var(--text-tertiary); letter-spacing:.06em; text-transform:uppercase; }
    .sheet-error { grid-column:1/-1; font-size:var(--text-xs); color:var(--danger-text); margin-top:-.25rem; }
    .sheet-label { font-size:var(--text-xs); color:var(--text-tertiary); letter-spacing:.08em; text-transform:uppercase; margin-bottom:.25rem; }
    .sheet-value { font-family:var(--font-display); font-size:1.5rem; color:var(--text-primary); font-variant-numeric:tabular-nums; }
    @media (max-width: 640px) {
      .sheet-row { grid-template-columns: 1fr 1fr; }
      .sheet-head { display:none; }
    }
  `]
})
export class TechnicalSheetModalComponent implements OnInit {
  @Input({ required: true }) dish!: MenuItem;
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<TechnicalSheet>();

  readonly colors = FOOD_COST_COLORS;
  loading = signal(true);
  saving = signal(false);
  error = signal('');
  ingredients = signal<Ingrediente[]>([]);
  private submitted = false;

  form = this.fb.group({ items: this.fb.array<ItemForm>([]) });
  get items(): FormArray<ItemForm> { return this.form.controls.items; }

  // Recalcula a cada tecla: valueChanges vira signal
  private formValue = signal(this.form.getRawValue());

  preview = computed(() => {
    this.formValue();
    const cost = this.items.controls.reduce((sum, _, i) => sum + this.rowCost(i), 0);
    const price = Number(this.dish?.price) || 0;
    if (this.items.length === 0 || price <= 0) return { cost, foodCost: 0, level: null as FoodCostLevel | null, warning: false };
    const foodCost = Math.round((cost * 100 / price) * 100) / 100;
    return { cost, foodCost, level: foodCostLevel(foodCost), warning: foodCost > LIMITE_ALERTA };
  });

  constructor(private fb: FormBuilder, private menu: MenuService, private ingredientService: IngredienteService) {
    this.form.valueChanges.subscribe(() => this.formValue.set(this.form.getRawValue()));
  }

  async ngOnInit() {
    try {
      const [page, sheet] = await Promise.all([
        firstValueFrom(this.ingredientService.listar(0, 100)),
        this.menu.getTechnicalSheet(this.dish.id),
      ]);
      this.ingredients.set(page.content);
      sheet.items.forEach(item => this.addRow(item.ingredientId, item.quantity, item.correctionFactor));
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'Erro ao carregar a ficha técnica.');
    } finally {
      this.loading.set(false);
    }
  }

  addRow(ingredientId: number | null = null, quantity: number | null = null, correctionFactor: number | null = 1) {
    this.items.push(this.fb.group({
      ingredientId: this.fb.control<number | null>(ingredientId, Validators.required),
      quantity: this.fb.control<number | null>(quantity, [Validators.required, Validators.min(0.0001)]),
      // RN08: fator de correção maior ou igual a 1,0
      correctionFactor: this.fb.control<number | null>(correctionFactor, [Validators.required, Validators.min(1)]),
    }));
  }

  removeRow(i: number) { this.items.removeAt(i); }

  unitOf(i: number): string {
    return this.ingredientOf(i)?.unidadeMedida ?? '';
  }

  rowCost(i: number): number {
    const row = this.items.at(i).getRawValue();
    const ing = this.ingredientOf(i);
    if (!ing || !row.quantity || !row.correctionFactor) return 0;
    return Number(row.quantity) * Number(row.correctionFactor) * Number(ing.custoUnitario);
  }

  rowError(i: number): string | null {
    const row = this.items.at(i);
    if (!row.touched && !this.submitted) return null;
    const c = row.controls;
    if (c.ingredientId.invalid) return 'Escolha o ingrediente.';
    if (c.quantity.invalid) return 'A quantidade deve ser maior que zero.';
    if (c.correctionFactor.invalid) return 'O fator de correção deve ser maior ou igual a 1,0 (RN08).';
    if (this.isDuplicate(i)) return 'Este ingrediente já está na ficha.';
    return null;
  }

  async save() {
    this.submitted = true;
    this.items.markAllAsTouched();
    if (this.items.invalid || this.items.controls.some((_, i) => this.isDuplicate(i))) {
      this.error.set('Corrija os campos destacados antes de salvar.');
      return;
    }
    this.error.set('');
    this.saving.set(true);
    try {
      const sheet = await this.menu.saveTechnicalSheet(this.dish.id, this.items.getRawValue().map(r => ({
        ingredientId: Number(r.ingredientId),
        quantity: Number(r.quantity),
        correctionFactor: Number(r.correctionFactor),
      })));
      this.saved.emit(sheet);
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'Erro ao salvar a ficha técnica.');
    } finally {
      this.saving.set(false);
    }
  }

  private ingredientOf(i: number): Ingrediente | undefined {
    const id = this.items.at(i).controls.ingredientId.value;
    return this.ingredients().find(ing => ing.id === id);
  }

  private isDuplicate(i: number): boolean {
    const id = this.items.at(i).controls.ingredientId.value;
    return id != null && this.items.controls.findIndex(r => r.controls.ingredientId.value === id) !== i;
  }
}
