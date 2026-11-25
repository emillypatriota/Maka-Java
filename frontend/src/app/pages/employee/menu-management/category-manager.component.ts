import { Component, EventEmitter, OnInit, Output, signal } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MenuService } from '../../../services/menu.service';
import { ToastService } from '../../../services/toast.service';
import { Category } from '../../../models';

@Component({
  selector: 'app-category-manager',
  standalone: true,
  imports: [NgFor, NgIf, FormsModule],
  template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="modal" style="max-width:520px" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Categorias</h3>
          <button class="btn btn-ghost btn-icon" type="button" (click)="close.emit()" aria-label="Fechar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <div class="modal-body" style="display:flex;flex-direction:column;gap:1rem">
          <form (ngSubmit)="add()" style="display:flex;gap:.5rem">
            <input class="form-input" name="newName" [(ngModel)]="newName" placeholder="Nova categoria (ex: Massas)" maxlength="60"
                   aria-label="Nome da nova categoria" style="flex:1">
            <button class="btn btn-primary" type="submit" [disabled]="busy() || !newName.trim()">Adicionar</button>
          </form>

          <div *ngIf="loading()" style="padding:1rem;text-align:center;color:var(--text-tertiary)">Carregando...</div>

          <ul *ngIf="!loading()" style="list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:.5rem">
            <li *ngFor="let c of categories()" class="cat-row" [style.opacity]="c.status === 'inactive' ? '.6' : '1'">
              <ng-container *ngIf="editingId !== c.id; else editing">
                <span style="flex:1;color:var(--text-primary)">{{ c.name }}</span>
                <span *ngIf="c.status === 'inactive'" class="badge"
                      style="background:var(--neutral-bg);color:var(--neutral-text);border:1px solid var(--neutral-border)">Inativa</span>
                <button class="btn btn-ghost btn-sm" type="button" (click)="startEdit(c)">Renomear</button>
                <button *ngIf="c.status === 'active'" class="btn btn-sm" type="button" (click)="deactivate(c)" [disabled]="busy()"
                        style="background:var(--danger-bg);color:var(--danger-text);border:1px solid var(--danger-border)">Desativar</button>
                <button *ngIf="c.status === 'inactive'" class="btn btn-sm" type="button" (click)="reactivate(c)" [disabled]="busy()"
                        style="background:var(--success-bg);color:var(--success-text);border:1px solid var(--success-border)">Reativar</button>
              </ng-container>
              <ng-template #editing>
                <input class="form-input" [(ngModel)]="editName" [name]="'edit-' + c.id" maxlength="60" style="flex:1"
                       aria-label="Novo nome da categoria" (keyup.enter)="saveEdit(c)">
                <button class="btn btn-primary btn-sm" type="button" (click)="saveEdit(c)" [disabled]="busy() || !editName.trim()">Salvar</button>
                <button class="btn btn-ghost btn-sm" type="button" (click)="editingId = null">Cancelar</button>
              </ng-template>
            </li>
          </ul>

          <p style="font-size:var(--text-xs);color:var(--text-tertiary);line-height:1.5">
            Desativar uma categoria tira os pratos dela do cardápio do cliente, mas eles continuam salvos.
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .cat-row { display:flex; align-items:center; gap:.5rem; padding:.625rem .75rem; border:1px solid var(--border); border-radius:var(--r-lg); background:var(--bg-elevated); flex-wrap:wrap; }
  `]
})
export class CategoryManagerComponent implements OnInit {
  @Output() close = new EventEmitter<void>();
  @Output() changed = new EventEmitter<void>();

  categories = signal<Category[]>([]);
  loading = signal(true);
  busy = signal(false);
  newName = '';
  editingId: string | null = null;
  editName = '';

  constructor(private menu: MenuService, private toast: ToastService) {}

  ngOnInit() { this.load(); }

  async load() {
    try {
      this.categories.set((await this.menu.listAdminCategories()).content);
    } catch (e) {
      this.toast.show(e instanceof Error ? e.message : 'Erro ao carregar categorias.', 'error');
    } finally {
      this.loading.set(false);
    }
  }

  add() {
    const name = this.newName.trim();
    if (!name) return;
    this.run(() => this.menu.addCategory(name), `Categoria "${name}" criada`, () => this.newName = '');
  }

  startEdit(c: Category) {
    this.editingId = c.id;
    this.editName = c.name;
  }

  saveEdit(c: Category) {
    const name = this.editName.trim();
    if (!name) return;
    this.run(() => this.menu.updateCategory(c.id, { name }), 'Categoria renomeada', () => this.editingId = null);
  }

  deactivate(c: Category) {
    this.run(() => this.menu.deleteCategory(c.id), `"${c.name}" desativada`);
  }

  reactivate(c: Category) {
    this.run(() => this.menu.updateCategory(c.id, { name: c.name, status: 'active' }), `"${c.name}" reativada`);
  }

  private async run(action: () => Promise<unknown>, success: string, after?: () => void) {
    this.busy.set(true);
    try {
      await action();
      this.toast.show(success);
      after?.();
      await this.load();
      this.changed.emit();
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Erro ao salvar.';
      // O backend devolve 409 genérico para nome repetido
      this.toast.show(message.includes('conflita') ? 'Já existe uma categoria com esse nome.' : message, 'error');
    } finally {
      this.busy.set(false);
    }
  }
}
