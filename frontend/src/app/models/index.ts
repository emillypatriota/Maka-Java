// ============================================================
// MAKA — Enterprise Models & Types
// ============================================================

// ── Status & Role Types ───────────────────────────────────────
export type OrderStatus =
  | 'pending'
  | 'confirmed'
  | 'preparing'
  | 'ready'
  | 'delivering'
  | 'delivered'
  | 'cancelled';

export type UserRole = 'manager' | 'kitchen' | 'delivery' | null;
export type PaymentMethod = 'credit' | 'debit' | 'pix' | 'cash';
export type MenuCategory =
  | 'entradas'
  | 'carnes'
  | 'peixes'
  | 'massas'
  | 'acompanhamentos'
  | 'sobremesas'
  | 'bebidas';

export type OrderType = 'delivery' | 'mesa' | 'retirada';

// ── Menu ──────────────────────────────────────────────────────
export interface MenuItem {
  id: string;
  name: string;
  description: string;
  price: number;
  category: MenuCategory | string;
  image: string;
  isSpecial?: boolean;
  active?: boolean;
  prepTime?: number;     // minutes
  calories?: number;
  tags?: string[];       // 'vegano', 'sem-gluten', etc.
  createdAt?: Date;
  status?: DishStatus;          // banco: ATIVO, PAUSADO, INATIVO
  preparationMethod?: string;   // modo de preparo (RF-014)
  // Só na gestão (/api/admin/pratos): custo da ficha técnica, null se não houver ficha
  hasTechnicalSheet?: boolean;
  cost?: number | null;
  foodCost?: number | null;     // % do preço
  foodCostLevel?: FoodCostLevel | null;
  foodCostWarning?: boolean | null;  // RN02: food cost acima de 35%
}

export type DishStatus = 'active' | 'paused' | 'inactive';
export type FoodCostLevel = 'green' | 'yellow' | 'red';

export interface Category {
  id: string;
  name: string;
  status: 'active' | 'inactive';   // banco: ATIVO, INATIVO
}

export interface MenuSummary {
  total: number;
  active: number;
  paused: number;
  inactive: number;
}

// Resposta paginada do Spring (Page<T>)
export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Ficha técnica (RF-011). quantity e unitCost na unidade do ingrediente
export interface TechnicalSheetItem {
  ingredientId: number;
  ingredientName: string;
  unit: string;
  unitCost: number;
  quantity: number;
  correctionFactor: number;
  cost: number;
}

export interface TechnicalSheet {
  dishId: string;
  dishName: string;
  price: number;
  status: DishStatus;
  items: TechnicalSheetItem[];
  cost: number | null;
  foodCost: number | null;
  foodCostLevel: FoodCostLevel | null;
  foodCostWarning: boolean;
}

export interface CartItem extends MenuItem {
  quantity: number;
  observations?: string;
}

// ── Customer ──────────────────────────────────────────────────
export interface Customer {
  name: string;
  phone: string;
  address: string;
  email?: string;
  uid?: string;
}

// ── Order ─────────────────────────────────────────────────────
export interface Order {
  id: string;
  orderNumber: string;
  items: CartItem[];
  customer: Customer;
  paymentMethod: string;
  total: number;
  deliveryFee: number;
  status: OrderStatus;
  orderType: OrderType;
  createdAt: Date;
  updatedAt?: Date;
  deliveryPersonId?: string;
  observations?: string;
  isLocal?: boolean;
  customerUid?: string;
  estimatedTime?: number;   // minutes
  tableNumber?: number;
  cancelReason?: string;
}

// ── Auth ──────────────────────────────────────────────────────
export interface User {
  uid: string;
  email: string;
  role: UserRole;
  username: string;
  avatar?: string;
  phone?: string;
  createdAt?: Date;
}

// ── Status Config ─────────────────────────────────────────────
export const STATUS_CONFIG: Record<
  OrderStatus,
  { label: string; color: string; bg: string; border: string; cssClass: string }
> = {
  pending:    { label: 'Aguardando',   color: '#a1a1aa', bg: 'rgba(113,113,122,.1)',  border: 'rgba(113,113,122,.2)', cssClass: 'badge-pending'    },
  confirmed:  { label: 'Confirmado',   color: '#a5b4fc', bg: 'rgba(99,102,241,.1)',   border: 'rgba(99,102,241,.2)',  cssClass: 'badge-ready'      },
  preparing:  { label: 'Preparando',   color: '#fbbf24', bg: 'rgba(245,158,11,.1)',   border: 'rgba(245,158,11,.2)', cssClass: 'badge-preparing'  },
  ready:      { label: 'Pronto',       color: '#34d399', bg: 'rgba(16,185,129,.1)',   border: 'rgba(16,185,129,.2)', cssClass: 'badge-ready'      },
  delivering: { label: 'A Caminho',    color: '#34d399', bg: 'rgba(16,185,129,.1)',   border: 'rgba(16,185,129,.2)', cssClass: 'badge-delivering' },
  delivered:  { label: 'Entregue',     color: '#34d399', bg: 'rgba(16,185,129,.1)',   border: 'rgba(16,185,129,.2)', cssClass: 'badge-delivered'  },
  cancelled:  { label: 'Cancelado',    color: '#f87171', bg: 'rgba(239,68,68,.1)',    border: 'rgba(239,68,68,.2)',  cssClass: 'badge-cancelled'  },
};

// ── Navigation ────────────────────────────────────────────────
export interface NavItem {
  label: string;
  route: string;
  icon: string;
  badge?: number;
  exact?: boolean;
}

export interface NavSection {
  label: string;
  items: NavItem[];
}

// ── Dashboard KPI ─────────────────────────────────────────────
export interface KpiCard {
  label: string;
  value: string | number;
  trend?: number;
  trendLabel?: string;
  icon: string;
  iconBg: string;
  accentColor: string;
}

// ── Delivery ──────────────────────────────────────────────────
export interface DeliveryPerson {
  id: string;
  name: string;
  phone: string;
  vehicle: string;
  status: 'available' | 'busy' | 'offline';
  currentOrderId?: string;
  totalDeliveries?: number;
  earnings?: number;
}
