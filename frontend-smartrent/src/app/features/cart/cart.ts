import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth/auth.service';
import { MsalService } from '@azure/msal-angular';
import { ApiService } from '../../core/services/api/api.service';

interface CartItem {
  machine: any;
  type: 'express' | 'quote';
}

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule],
  selector: 'app-cart',
  styleUrl: './cart.css',
  templateUrl: './cart.html',
})
export class Cart implements OnInit {
  authService = inject(AuthService);
  msalService = inject(MsalService);
  router = inject(Router);
  apiService = inject(ApiService);

  expressItems: CartItem[] = [];
  quoteItems: CartItem[] = [];

  ngOnInit() {
    this.loadCart();
  }

  loadCart() {
    const cart: CartItem[] = JSON.parse(localStorage.getItem('smartrent_cart') || '[]');
    this.expressItems = cart.filter(item => item.type === 'express');
    this.quoteItems = cart.filter(item => item.type === 'quote');
  }

  getImageUrl(machine: any): string {
    if (!machine || !machine.category) return 'https://images.unsplash.com/photo-1621905252507-b35492cc74b4?auto=format&fit=crop&q=80&w=800';
    const cat = machine.category.toLowerCase();
    if (cat.includes('excavación')) return 'https://images.unsplash.com/photo-1579730691238-ebbc2fec68b8?auto=format&fit=crop&q=80&w=800';
    if (cat.includes('carga')) return 'https://images.unsplash.com/photo-1600860570390-c116bebc4eef?auto=format&fit=crop&q=80&w=800';
    if (cat.includes('elevación') || cat.includes('grúa')) return 'https://images.unsplash.com/photo-1504307651254-35680f356f67?auto=format&fit=crop&q=80&w=800';
    return 'https://images.unsplash.com/photo-1621905252507-b35492cc74b4?auto=format&fit=crop&q=80&w=800';
  }

  removeItem(item: CartItem) {
    let cart: CartItem[] = JSON.parse(localStorage.getItem('smartrent_cart') || '[]');
    cart = cart.filter(c => c.machine.id !== item.machine.id);
    localStorage.setItem('smartrent_cart', JSON.stringify(cart));
    this.loadCart();
  }

  private syncAndCheckout(type: 'express' | 'quote') {
    const allItems = [...this.expressItems, ...this.quoteItems].map(item => ({
      machineId: item.machine.id,
      machineName: item.machine.name,
      type: item.type,
      dailyPrice: item.machine.dailyPrice || item.machine.pricePerDay
    }));

    this.apiService.syncCart(allItems).subscribe({
      next: () => {
        this.apiService.checkoutCart(type).subscribe({
          next: (res) => {
            alert(res.message);
            // Remove checkout items from local storage
            let cart: CartItem[] = JSON.parse(localStorage.getItem('smartrent_cart') || '[]');
            cart = cart.filter(c => c.type !== type);
            localStorage.setItem('smartrent_cart', JSON.stringify(cart));
            this.loadCart();
          },
          error: (err) => {
            console.error(err);
            alert('Error al procesar el checkout');
          }
        });
      },
      error: (err) => {
        console.error(err);
        alert('Error al sincronizar el carrito con el servidor');
      }
    });
  }

  proceedToCheckout() {
    if (!this.authService.account()) {
      alert('Debes iniciar sesión para proceder al pago.');
      this.msalService.loginRedirect();
      return;
    }
    if (this.expressItems.length === 0) return;
    void this.router.navigate(['/checkout'], { queryParams: { type: 'express' } });
  }

  requestQuote() {
    if (!this.authService.account()) {
      alert('Debes iniciar sesión para solicitar una cotización.');
      this.msalService.loginRedirect();
      return;
    }
    if (this.quoteItems.length === 0) return;
    void this.router.navigate(['/checkout'], { queryParams: { type: 'quote' } });
  }

  get totalExpress() {
    return this.expressItems.reduce((acc, item) => acc + item.machine.dailyPrice, 0);
  }
}
