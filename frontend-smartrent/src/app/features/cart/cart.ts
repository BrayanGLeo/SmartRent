import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth/auth.service';
import { CartService } from '../../core/services/cart/cart.service';
import { CartItem } from '../../core/models';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule],
  selector: 'app-cart',
  styleUrl: './cart.css',
  templateUrl: './cart.html',
})
export class Cart implements OnInit {
  authService = inject(AuthService);
  cartService = inject(CartService);
  router = inject(Router);

  ngOnInit() {
    // El servicio reactivo ya carga y sincroniza el estado
  }

  get expressItems(): CartItem[] {
    return this.cartService.expressItems();
  }

  get quoteItems(): CartItem[] {
    return this.cartService.quoteItems();
  }

  get totalItemsCount(): number {
    return this.cartService.totalItemsCount();
  }

  get summary() {
    return this.cartService.summary();
  }

  getImageUrl(item: CartItem): string {
    const m = item.machine;
    if (m?.imageUrl) return m.imageUrl;
    const name = (item.machineName || m?.name || '').toLowerCase();
    const sku = (m?.sku || m?.serialNumber || '').toLowerCase();

    if (name.includes('cat') || name.includes('320') || sku.includes('cat-320')) return '/images/cat-320.jpg';
    if (name.includes('deere') || name.includes('310') || sku.includes('jd-310')) return '/images/jd-310l.jpg';
    if (name.includes('bomag') || name.includes('211') || sku.includes('bmg')) return '/images/bomag-bw211.jpg';
    if (name.includes('makita') || sku.includes('mak')) return '/images/makita-18v.jpg';
    if (name.includes('bosch') || name.includes('gsh') || sku.includes('bos')) return '/images/bosch-gsh11e.jpg';
    if (name.includes('dewalt') || name.includes('circular') || sku.includes('dw')) return '/images/dewalt-dwe575.jpg';
    if (name.includes('jlg') || name.includes('450') || sku.includes('jlg')) return '/images/jlg-450aj.jpg';
    if (name.includes('yale') || name.includes('montacargas') || sku.includes('yal')) return '/images/yale-3t.jpg';
    if (name.includes('cummins') || sku.includes('cum')) return '/images/cummins-50kva.jpg';
    if (name.includes('eu22') || name.includes('inverter') || sku.includes('hon-eu')) return '/images/honda-eu2200i.jpg';
    if (name.includes('sullair') || name.includes('185') || sku.includes('sul')) return '/images/sullair-185.jpg';
    if (name.includes('husqvarna') || name.includes('motosierra') || sku.includes('hus')) return '/images/husqvarna-450.jpg';
    if (name.includes('hrn') || name.includes('pasto') || name.includes('césped') || sku.includes('hon-cp')) return '/images/honda-hrn216.jpg';

    const cat = (m?.category ? (typeof m.category === 'string' ? m.category : m.category.name || '') : '').toLowerCase();
    if (cat.includes('pesada') || cat.includes('excav')) return '/images/cat-320.jpg';
    if (cat.includes('elevación') || cat.includes('grúa') || cat.includes('yale')) return '/images/jlg-450aj.jpg';
    if (cat.includes('generador') || cat.includes('energía') || cat.includes('compresor')) return '/images/cummins-50kva.jpg';
    if (cat.includes('eléctrica') || cat.includes('herramienta')) return '/images/makita-18v.jpg';
    if (cat.includes('jardín') || cat.includes('paisajismo')) return '/images/husqvarna-450.jpg';
    return '/images/cat-320.jpg';
  }

  updateDays(machineId: number | string, delta: number, currentDays: number = 1): void {
    const nextDays = Math.max(1, currentDays + delta);
    this.cartService.updateRentalDays(machineId, nextDays);
  }

  removeItem(machineId: number | string): void {
    this.cartService.removeItem(machineId);
  }

  proceedToCheckout(type: 'express' | 'quote'): void {
    if (!this.authService.account()) {
      alert('Se requiere autenticación o datos de empresa para formalizar el arriendo.');
      this.authService.login();
      return;
    }

    void this.router.navigate(['/checkout'], { queryParams: { type } });
  }

  clearCart(): void {
    if (confirm('¿Deseas vaciar todos los equipos seleccionados de tu cotización?')) {
      this.cartService.clear();
    }
  }
}
