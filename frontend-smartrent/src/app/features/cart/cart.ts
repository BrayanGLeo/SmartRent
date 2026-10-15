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
    const cat = (m?.category ? (typeof m.category === 'string' ? m.category : m.category.name || '') : '').toLowerCase();
    if (cat.includes('pesada') || cat.includes('excav')) return '/images/excavator.jpg';
    if (cat.includes('elevación') || cat.includes('grúa') || cat.includes('yale')) return '/images/crane.jpg';
    if (cat.includes('generador') || cat.includes('energía') || cat.includes('compresor')) return '/images/generator.jpg';
    return '/images/loader.jpg';
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
