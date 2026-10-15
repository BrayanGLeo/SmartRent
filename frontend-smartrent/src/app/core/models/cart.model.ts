/**
 * Modelos de Dominio: Carrito de Arriendo y Cotización
 * Alineado con microservicio: ms-smartrent-cart
 * DB Tables: carts, cart_items
 */

import { Machine } from './catalog.model';

export type CartItemType = 'express' | 'quote';

export interface CartItem {
  id?: number;
  machineId: number | string;
  machineName: string;
  type: CartItemType;
  dailyPrice: number;
  // Campos calculados y parámetros de arriendo en faena
  days?: number;
  startDate?: string;
  endDate?: string;
  warrantyDeposit?: number;
  subtotal?: number;
  machine?: Machine;
}

export interface Cart {
  id?: number;
  userId: string;
  items: CartItem[];
  // Resumen calculado en cliente
  totalDays?: number;
  totalDailyNet?: number;
  totalDeposit?: number;
  totalEstimate?: number;
}

export interface CartItemDto {
  machineId: number;
  machineName: string;
  dailyPrice: number;
}

export interface CheckoutEvent {
  userId: string;
  type: CartItemType | string;
  items: CartItemDto[];
}

export interface CartSummary {
  expressCount: number;
  quoteCount: number;
  totalItems: number;
  estimatedNetClp: number;
  totalDepositClp: number;
  estimatedIvaClp: number;
  estimatedTotalClp: number;
}
