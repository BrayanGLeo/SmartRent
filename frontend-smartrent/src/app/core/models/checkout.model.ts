/**
 * Modelos de Dominio: Checkout y Órdenes de Arriendo
 * Alineado con microservicio: ms-smartrent-checkout
 * DB Tables: checkout_orders, checkout_order_items
 */

export type CustomerType = 'PERSON' | 'COMPANY';
export type OrderType = 'EXPRESS' | 'QUOTE';
export type DeliveryType = 'PICKUP' | 'DISPATCH';
export type PaymentMethod = 'TRANSFER' | 'CARD' | 'CREDIT_LINE_30D';
export type OrderStatus = 'PENDING' | 'COMPLETED' | 'QUOTED' | 'CANCELLED' | 'IN_REVIEW';

export interface OrderItem {
  id?: number;
  machineId: number;
  machineName: string;
  dailyPrice: number;
  days?: number;
  subtotal?: number;
}

export interface Order {
  id?: number;
  userId: string;
  type: OrderType | string;
  customerType: CustomerType | string;
  rut: string;
  name: string;
  phone: string;
  deliveryType: DeliveryType | string;
  deliveryAddress?: string;
  paymentMethod: PaymentMethod | string;
  status: OrderStatus | string;
  createdAt?: string;
  items: OrderItem[];
  totalAmountClp?: number;
}

export interface CheckoutItemPayload {
  machineId: number;
  machineName: string;
  dailyPrice: number;
}

export interface CheckoutRequest {
  type: OrderType | string;
  customerType: CustomerType | string;
  rut: string;
  name: string;
  phone: string;
  deliveryType: DeliveryType | string;
  deliveryAddress?: string;
  paymentMethod: PaymentMethod | string;
  items: CheckoutItemPayload[];
}

export interface OrderCreatedEvent {
  orderId: number;
  userId: string;
  rut: string;
  name: string;
  phone: string;
  customerType: CustomerType | string;
  orderType: OrderType | string;
  deliveryType: DeliveryType | string;
  deliveryAddress?: string;
  paymentMethod: PaymentMethod | string;
  items: CheckoutItemPayload[];
}
