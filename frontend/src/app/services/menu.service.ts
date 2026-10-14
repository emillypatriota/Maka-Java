import { Injectable, signal } from '@angular/core';
import {
  collection, addDoc, updateDoc, deleteDoc,
  doc, onSnapshot, query, orderBy, serverTimestamp
} from 'firebase/firestore';
import { firestore } from '../core/firebase/firebase.config';
import { MenuItem } from '../models';

// Fotos escolhidas a dedo — todas testadas, apetitosas e do tema steakhouse
export const INITIAL_MENU: Omit<MenuItem, 'id'>[] = [
  // ── Carnes Nobres ─────────────────────────────────────────────
  {
    name: 'Wagyu A5 Ribeye 200g',
    description: 'O nível máximo de marmoreio japonês. Textura sedosa que derrete na boca. Servido com sal negro havaiano, manteiga clarificada e brioche tostado artesanal.',
    price: 490, category: 'Carnes Nobres', isSpecial: true, prepTime: 25,
    image: 'https://images.unsplash.com/photo-1546039907-7fa05f864c02?w=800&q=85',
  },
  {
    name: 'Tomahawk Angus 1.2kg',
    description: 'Corte espetacular com osso longo de 30cm, maturado 30 dias em câmara própria. Chimichurri artesanal e farofa crocante de panko.',
    price: 380, category: 'Carnes Nobres', isSpecial: true, prepTime: 35,
    image: 'https://images.unsplash.com/photo-1600891964092-4316c288032e?w=800&q=85',
  },
  {
    name: 'Picanha Maturada 400g',
    description: 'A rainha das carnes brasileiras, selecionada e maturada 21 dias. Gordura dourada e crocante, interior rosado e suculento. Acompanha vinagrete artesanal.',
    price: 148, category: 'Carnes Nobres', isSpecial: true, prepTime: 20,
    image: 'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?w=800&q=85',
  },
  {
    name: 'Ancho Angus 400g',
    description: 'Corte nobre argentino, famoso pelo marmoreio generoso e maciez incomparável. Maturado 21 dias, grelhado na temperatura exata do seu ponto.',
    price: 148, category: 'Carnes Nobres', prepTime: 20,
    image: 'https://images.unsplash.com/photo-1558030006-450675393462?w=800&q=85',
  },
  {
    name: 'Filet Mignon 300g',
    description: 'O corte mais macio do boi, envolto em fatias de bacon defumado artesanal e flambado na manteiga clarificada com tomilho e alecrim frescos.',
    price: 138, category: 'Carnes Nobres', prepTime: 18,
    image: 'https://images.unsplash.com/photo-1546964124-0cce460c82fd?w=800&q=85',
  },
  {
    name: 'Costela Short Rib 600g',
    description: 'Assada em baixa temperatura por 12 horas com ervas e vinho tinto. Desfia no garfo com facilidade e finaliza caramelizada na brasa para textura perfeita.',
    price: 128, category: 'Carnes Nobres', prepTime: 15,
    image: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=800&q=85',
  },
  {
    name: 'Costela Angus 48h',
    description: 'Maturada e assada por 48 horas em forno a lenha em baixíssima temperatura. Desfiada na hora, servida com polenta cremosa e molho de pimenta defumada.',
    price: 118, category: 'Carnes Nobres', prepTime: 15,
    image: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=85',
  },

  // ── Entradas ──────────────────────────────────────────────────
  {
    name: 'Steak Tartare',
    description: 'Filé cortado à faca na hora, temperado com gema curada em sal e açúcar, alcaparras, mostarda Dijon e tabasco. Acompanha torradas artesanais crocantes.',
    price: 68, category: 'Entradas', prepTime: 10,
    image: 'https://images.unsplash.com/photo-1530469525856-cf37954301f7?w=800&q=85',
  },
  {
    name: 'Carpaccio de Filé Mignon',
    description: 'Finíssimas lâminas de filé mignon cru, cama de rúcula selvagem, parmesão 36 meses em lascas, alcaparras e finalização com azeite trufado da Itália.',
    price: 62, category: 'Entradas', prepTime: 8,
    image: 'https://images.unsplash.com/photo-1588944651162-57080995b92d?w=800&q=85',
  },
  {
    name: 'Burrata Artesanal',
    description: 'Burrata produzida artesanalmente com leite integral, tomates confit em azeite de ervas da Provence, pesto de manjericão fresco e ciabatta tostada.',
    price: 56, category: 'Entradas', prepTime: 8,
    image: 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=800&q=85',
  },
  {
    name: 'Tábua de Frios Premium',
    description: 'Seleção de queijos artesanais, presunto Parma 24 meses, salame italiano Felino, copa, geleias de frutas vermelhas e pães de fermentação natural.',
    price: 82, category: 'Entradas', prepTime: 5,
    image: 'https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=800&q=85',
  },
  {
    name: 'Camarão na Manteiga de Ervas',
    description: 'Camarões jumbo VG salteados em manteiga de ervas com alho negro caramelizado, limão siciliano e pimenta-do-reino fresca. Servidos em panela de ferro fundido.',
    price: 76, category: 'Entradas', prepTime: 12,
    image: 'https://images.unsplash.com/photo-1599021419847-d8a7a6aba5b4?w=800&q=85',
  },

  // ── Peixes & Frutos do Mar ────────────────────────────────────
  {
    name: 'Salmão Grelhado 300g',
    description: 'Salmão norueguês premium grelhado na manteiga de ervas frescas. Acompanha risoto cremoso de limão siciliano com aspargos e alcaparras fritas.',
    price: 98, category: 'Peixes & Frutos do Mar', prepTime: 20,
    image: 'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=800&q=85',
  },
  {
    name: 'Lagosta Grelhada',
    description: 'Lagosta inteira grelhada com manteiga clarificada e hervas frescas de Provence. Acompanha molho bisque cremoso feito com as cascas e legumes ao vapor.',
    price: 198, category: 'Peixes & Frutos do Mar', prepTime: 25,
    image: 'https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=800&q=85',
  },
  {
    name: 'Polvo Grelhado ao Azeite',
    description: 'Polvo português de alta qualidade, cozido em baixa temperatura e finalizado na brasa. Azeite extra virgem português, batatas rústicas ao murro e vinagrete de pimentões.',
    price: 142, category: 'Peixes & Frutos do Mar', prepTime: 22,
    image: 'https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=800&q=85',
  },

  // ── Acompanhamentos ───────────────────────────────────────────
  {
    name: 'Purê de Batata Trufado',
    description: 'Batata Asterix cozida e espremida com manteiga clarificada francesa, creme de leite fresco e trufa negra italiana Périgord ralada generosamente na hora.',
    price: 38, category: 'Acompanhamentos', prepTime: 10,
    image: 'https://images.unsplash.com/photo-1624968814155-236efede1cec?w=800&q=85',
  },
  {
    name: 'Aspargos na Brasa',
    description: 'Aspargos verdes frescos importados, grelhados com fio de azeite extra virgem, raspas de limão siciliano e cobertos com parmesão em lascas e flor de sal.',
    price: 32, category: 'Acompanhamentos', prepTime: 10,
    image: 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=800&q=85',
  },
  {
    name: 'Farofa Gourmet de Bacon',
    description: 'Farofa de mandioca torrada na manteiga de garrafa nordestina com bacon defumado artesanal crocante, castanha de caju e ervas frescas picadas.',
    price: 28, category: 'Acompanhamentos', prepTime: 8,
    image: 'https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=800&q=85',
  },
  {
    name: 'Batata Rústica ao Alecrim',
    description: 'Batatas cortadas à mão em gomos, assadas com alecrim fresco, alho confitado, sal grosso e finalização generosa em azeite extra virgem. Crocante por fora, macia por dentro.',
    price: 30, category: 'Acompanhamentos', prepTime: 25,
    image: 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=800&q=85',
  },

  // ── Sobremesas ────────────────────────────────────────────────
  {
    name: 'Petit Gâteau de Valrhona',
    description: 'Bolo de chocolate Valrhona 70% cacau com centro vulcânico líquido, servido com sorvete artesanal de baunilha Madagascar e calda de caramelo fleur de sel.',
    price: 42, category: 'Sobremesas', prepTime: 12,
    image: 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=800&q=85',
  },
  {
    name: 'Crème Brûlée de Lavanda',
    description: 'Creme sedoso infusionado com lavanda francesa e fava de baunilha Bourbon, caramelizado com maçarico na hora. Servido com frutas vermelhas frescas da estação.',
    price: 38, category: 'Sobremesas', prepTime: 10,
    image: 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800&q=85',
  },
  {
    name: 'Cheesecake de Frutas Vermelhas',
    description: 'Base amanteigada de biscoito Graham crocante, recheio aveludado de cream cheese com baunilha e calda generosa de frutas vermelhas frescas e coulis de framboesa.',
    price: 36, category: 'Sobremesas', prepTime: 8,
    image: 'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=800&q=85',
  },
  {
    name: 'Tiramisu Clássico',
    description: 'Receita italiana original: mascarpone fresco batido, biscoito champagne embebido em espresso duplo e licor de Amaretto, finalizado com cacau belga em pó.',
    price: 40, category: 'Sobremesas', prepTime: 8,
    image: 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=800&q=85',
  },

  // ── Bebidas ───────────────────────────────────────────────────
  {
    name: 'Vinho Malbec Taça',
    description: 'Malbec argentino selecionado da região de Mendoza. Notas intensas de frutas vermelhas escuras, especiarias e toque amadeirado. Servido à temperatura ideal 16–18°C.',
    price: 38, category: 'Bebidas', prepTime: 2,
    image: 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800&q=85',
  },
  {
    name: 'Suco Prensado a Frio 400ml',
    description: 'Laranja com maracujá, abacaxi com hortelã ou melancia com gengibre. Extraídos a frio para preservar 100% dos nutrientes e vitaminas. Zero açúcar adicionado.',
    price: 24, category: 'Bebidas', prepTime: 3,
    image: 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=800&q=85',
  },
  {
    name: 'Chopp Artesanal 500ml',
    description: 'Chopp pilsen gelado de cervejaria artesanal local, leve e refrescante com espuma cremosa perfeita. Tirado na hora diretamente do barril pressurizado.',
    price: 22, category: 'Bebidas', prepTime: 2,
    image: 'https://images.unsplash.com/photo-1567696911169-ab4bde6fc571?w=800&q=85',
  },
  {
    name: 'Água San Pellegrino 500ml',
    description: 'Água mineral italiana premium, natural da fonte San Pellegrino nas Alpes Lombardas. Com ou sem gás. Servida gelada com gelo e fatia de limão siciliano a pedido.',
    price: 18, category: 'Bebidas', prepTime: 1,
    image: 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=800&q=85',
  },
];

export const CATEGORIES = ['Carnes Nobres', 'Entradas', 'Peixes & Frutos do Mar', 'Acompanhamentos', 'Sobremesas', 'Bebidas'];

@Injectable({ providedIn: 'root' })
export class MenuService {
  private _items  = signal<MenuItem[]>([]);
  private _loaded = signal(false);

  readonly items$  = this._items.asReadonly();
  readonly loaded$ = this._loaded.asReadonly();

  constructor() { this.listenToMenu(); }

  private listenToMenu(): void {
    const q = query(collection(firestore, 'menu'), orderBy('category'));
    onSnapshot(q, async (snapshot) => {
      if (snapshot.empty) {
        try {
          await this.seedMenu();
          // loaded remains false — next snapshot (post-seed) will set it
        } catch (e) {
          console.error('[MenuService] Seed error (verifique regras do Firestore):', e);
          this._loaded.set(true); // para o skeleton mesmo com erro de seed
        }
        return;
      }
      this._items.set(snapshot.docs.map(d => ({ id: d.id, ...d.data() } as MenuItem)));
      this._loaded.set(true);
    }, err => {
      console.error('[MenuService] Firestore read error:', err);
      this._loaded.set(true); // para o skeleton mesmo com erro
    });
  }

  private async seedMenu(): Promise<void> {
    for (const item of INITIAL_MENU) {
      await addDoc(collection(firestore, 'menu'), { ...item, createdAt: serverTimestamp(), active: true });
    }
  }

  getAll(): MenuItem[] { return this._items(); }
  getCategories(): string[] { return CATEGORIES; }
  getByCategory(cat: string): MenuItem[] { return this._items().filter(i => i.category === cat); }
  getSpecials(): MenuItem[] { return this._items().filter(i => i.isSpecial).slice(0, 3); }

  async addItem(item: Omit<MenuItem, 'id'>): Promise<void> {
    await addDoc(collection(firestore, 'menu'), { ...item, createdAt: serverTimestamp(), active: true });
  }
  async updateItem(id: string, data: Partial<MenuItem>): Promise<void> {
    await updateDoc(doc(firestore, 'menu', id), data);
  }
  async deleteItem(id: string): Promise<void> {
    await deleteDoc(doc(firestore, 'menu', id));
  }
  async toggleItem(id: string, active: boolean): Promise<void> {
    await updateDoc(doc(firestore, 'menu', id), { active });
  }
}
