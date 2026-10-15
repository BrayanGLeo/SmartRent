import { Injectable, computed, signal, inject } from '@angular/core';
import { CartItem, CartSummary } from '../../models';
import { ApiService } from '../api/api.service';

const CART_STORAGE_KEY = 'smartrent_cart';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private apiService = inject(ApiService);

  private itemsSignal = signal<CartItem[]>(this.loadFromStorage());

  public items = computed(() => this.itemsSignal());

  public totalItemsCount = computed(() => this.itemsSignal().length);

  public expressItems = computed(() => this.itemsSignal().filter(i => i.type === 'express'));
  public quoteItems = computed(() => this.itemsSignal().filter(i => i.type === 'quote'));

  public summary = computed<CartSummary>(() => {
    const items = this.itemsSignal();
    const expressCount = items.filter(i => i.type === 'express').length;
    const quoteCount = items.filter(i => i.type === 'quote').length;

    let estimatedNet = 0;
    let totalDeposit = 0;

    for (const item of items) {
      const days = Math.max(1, item.days || 1);
      const price = item.dailyPrice || 0;
      estimatedNet += price * days;
      totalDeposit += item.warrantyDeposit || (price * 1.5);
    }

    const iva = Math.round(estimatedNet * 0.19);
    const total = estimatedNet + iva;

    return {
      expressCount,
      quoteCount,
      totalItems: items.length,
      estimatedNetClp: estimatedNet,
      totalDepositClp: Math.round(totalDeposit),
      estimatedIvaClp: iva,
      estimatedTotalClp: total
    };
  });

  private loadFromStorage(): CartItem[] {
    try {
      const data = localStorage.getItem(CART_STORAGE_KEY);
      if (!data) return [];
      const parsed = JSON.parse(data);
      if (!Array.isArray(parsed)) return [];

      return parsed.map((item: any) => {
        // Normalización si el formato anterior guardaba { machine, type }
        if (item.machine) {
          const m = item.machine;
          return {
            machineId: m.id,
            machineName: m.name,
            type: item.type || 'quote',
            dailyPrice: m.dailyPrice || m.pricePerDay || 0,
            days: item.days || 3,
            warrantyDeposit: m.warrantyDepositClp || ((m.dailyPrice || m.pricePerDay || 0) * 1.5),
            machine: m
          } as CartItem;
        }
        return item as CartItem;
      });
    } catch {
      return [];
    }
  }

  private persist(items: CartItem[]): void {
    this.itemsSignal.set(items);
    try {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
    } catch (e) {
      console.error('Error guardando carrito en storage', e);
    }
  }

  public addItem(item: CartItem): void {
    const current = [...this.itemsSignal()];
    const existingIndex = current.findIndex(c => String(c.machineId) === String(item.machineId));
    if (existingIndex >= 0) {
      current[existingIndex] = { ...current[existingIndex], ...item };
    } else {
      current.push(item);
    }
    this.persist(current);
  }

  public removeItem(machineId: number | string): void {
    const current = this.itemsSignal().filter(c => String(c.machineId) !== String(machineId));
    this.persist(current);
  }

  public updateRentalDays(machineId: number | string, days: number, startDate?: string, endDate?: string): void {
    const current = this.itemsSignal().map(item => {
      if (String(item.machineId) === String(machineId)) {
        return {
          ...item,
          days: Math.max(1, days),
          startDate: startDate || item.startDate,
          endDate: endDate || item.endDate
        };
      }
      return item;
    });
    this.persist(current);
  }

  public clear(): void {
    this.persist([]);
  }
}
