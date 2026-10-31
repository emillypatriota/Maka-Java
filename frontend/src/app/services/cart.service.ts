import { Injectable, effect, signal } from '@angular/core';
import { BehaviorSubject, Observable, map } from 'rxjs';
import { CartItem, MenuItem } from '../models';
import { MenuService } from './menu.service';

const STORAGE_KEY = 'maka_cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private items$ = new BehaviorSubject<CartItem[]>(this.loadFromStorage());

  // Nomes dos pratos tirados do carrinho porque saíram do cardápio (pausados ou inativos)
  private _removed = signal<string[]>([]);
  readonly removed$ = this._removed.asReadonly();

  constructor(private menu: MenuService) {
    // Persiste no localStorage a cada mudança
    this.items$.subscribe(items => this.saveToStorage(items));
    // O carrinho fica salvo no navegador: sempre que o cardápio chega da API, confere se continua valendo
    effect(() => {
      if (this.menu.loadedOk$()) this.syncWithMenu(this.menu.items$());
    }, { allowSignalWrites: true });
  }

  get items(): Observable<CartItem[]> { return this.items$.asObservable(); }
  get count(): Observable<number> { return this.items$.pipe(map(items => items.reduce((s, i) => s + i.quantity, 0))); }
  get total(): Observable<number> { return this.items$.pipe(map(items => items.reduce((s, i) => s + i.price * i.quantity, 0))); }
  getItems(): CartItem[] { return this.items$.value; }
  getTotal(): number { return this.items$.value.reduce((s, i) => s + i.price * i.quantity, 0); }
  getCount(): number { return this.items$.value.reduce((s, i) => s + i.quantity, 0); }

  addItem(item: MenuItem): void {
    const cur = this.items$.value;
    const existing = cur.find(i => i.id === item.id);
    if (existing) {
      this.items$.next(cur.map(i => i.id === item.id ? { ...i, quantity: i.quantity + 1 } : i));
    } else {
      this.items$.next([...cur, { ...item, quantity: 1 }]);
    }
  }

  updateQuantity(id: string, qty: number): void {
    if (qty <= 0) { this.removeItem(id); return; }
    this.items$.next(this.items$.value.map(i => i.id === id ? { ...i, quantity: qty } : i));
  }

  removeItem(id: string): void {
    this.items$.next(this.items$.value.filter(i => i.id !== id));
  }

  clear(): void { this.items$.next([]); }

  dismissRemoved(): void { this._removed.set([]); }

  // Tira o que não está mais à venda e atualiza preço, nome e foto com os dados atuais do cardápio
  private syncWithMenu(menu: MenuItem[]): void {
    const cart = this.items$.value;
    if (cart.length === 0) return;
    const byId = new Map(menu.map(i => [i.id, i]));
    const removed = cart.filter(i => !byId.has(i.id)).map(i => i.name);
    const synced = cart
      .filter(i => byId.has(i.id))
      .map(i => {
        const current = byId.get(i.id)!;
        return { ...i, name: current.name, price: current.price, image: current.image };
      });
    const changed = removed.length > 0 || synced.some(i => {
      const old = cart.find(c => c.id === i.id)!;
      return old.price !== i.price || old.name !== i.name || old.image !== i.image;
    });
    if (removed.length) this._removed.update(prev => [...prev, ...removed]);
    if (changed) this.items$.next(synced);
  }

  private loadFromStorage(): CartItem[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as CartItem[]) : [];
    } catch {
      return [];
    }
  }

  private saveToStorage(items: CartItem[]): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      // storage quota exceeded — ignora silenciosamente
    }
  }
}
