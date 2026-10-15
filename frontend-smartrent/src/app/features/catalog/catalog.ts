import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService, Machine } from '../../core/services/api/api.service';
import { AuthService } from '../../core/services/auth/auth.service';
import { CartService } from '../../core/services/cart/cart.service';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  selector: 'app-catalog',
  styleUrl: './catalog.css',
  templateUrl: './catalog.html',
})
export class Catalog implements OnInit {
  apiService = inject(ApiService);
  authService = inject(AuthService);
  cartService = inject(CartService);
  router = inject(Router);
  route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  machines: Machine[] = [];
  categories: string[] = [
    'Todas',
    'Maquinaria Pesada',
    'Equipos de Elevación',
    'Generadores y Energía',
    'Herramientas Eléctricas',
    'Jardinería y Paisajismo'
  ];

  // Filtros técnicos
  searchTerm = '';
  selectedCategory = 'Todas';
  onlyAvailable = false;
  sortBy = 'default';

  // Fechas contractuales
  rentalStartDate = this.getDefaultDate(0);
  rentalEndDate = this.getDefaultDate(7);

  // Notificación técnica
  notificationMessage = '';
  notificationType: 'quote' | 'express' | '' = '';
  showNotification = false;

  ngOnInit() {
    // Escuchar query params iniciales desde el buscador del Home
    this.route.queryParams.subscribe(params => {
      if (params['category']) {
        this.selectedCategory = params['category'];
      }
      if (params['startDate']) {
        this.rentalStartDate = params['startDate'];
      }
      if (params['endDate']) {
        this.rentalEndDate = params['endDate'];
      }
    });

    this.loadCatalog();
  }

  loadCatalog(): void {
    this.apiService.getCatalog().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.machines = data;
        } else {
          this.machines = this.apiService.getMockCatalog();
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.machines = this.apiService.getMockCatalog();
        this.cdr.detectChanges();
      }
    });
  }

  get rentalDays(): number {
    if (!this.rentalStartDate || !this.rentalEndDate) return 1;
    const start = new Date(this.rentalStartDate).getTime();
    const end = new Date(this.rentalEndDate).getTime();
    const diffDays = Math.ceil((end - start) / (1000 * 60 * 60 * 24));
    return Math.max(1, diffDays);
  }

  get filteredMachines(): Machine[] {
    let result = [...this.machines];

    // Filtro por categoría
    if (this.selectedCategory && this.selectedCategory !== 'Todas') {
      result = result.filter(m => {
        const catName = typeof m.category === 'string' ? m.category : m.category?.name || '';
        return catName.toLowerCase().includes(this.selectedCategory.toLowerCase());
      });
    }

    // Filtro por disponibilidad
    if (this.onlyAvailable) {
      result = result.filter(m => m.isAvailable);
    }

    // Filtro por búsqueda de texto (nombre, SKU o marca)
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      result = result.filter(m =>
        m.name.toLowerCase().includes(term) ||
        (m.serialNumber && m.serialNumber.toLowerCase().includes(term)) ||
        (m.sku && m.sku.toLowerCase().includes(term)) ||
        (m.brand && m.brand.toLowerCase().includes(term)) ||
        (m.description && m.description.toLowerCase().includes(term))
      );
    }

    // Ordenamiento técnico
    if (this.sortBy === 'price_asc') {
      result.sort((a, b) => a.dailyPrice - b.dailyPrice);
    } else if (this.sortBy === 'price_desc') {
      result.sort((a, b) => b.dailyPrice - a.dailyPrice);
    } else if (this.sortBy === 'name') {
      result.sort((a, b) => a.name.localeCompare(b.name));
    }

    return result;
  }

  get isAdmin(): boolean {
    return this.authService.userRole() === 'Admin';
  }

  isHeavyMachinery(category: any): boolean {
    const catName = typeof category === 'string' ? category : category?.name || '';
    const heavyCategories = ['Maquinaria Pesada', 'Equipos de Elevación', 'Generadores'];
    return heavyCategories.some(h => catName.toLowerCase().includes(h.toLowerCase()));
  }

  getImageUrl(machine: Machine): string {
    if (machine.imageUrl) return machine.imageUrl;
    const name = (machine.name || '').toLowerCase();
    const sku = (machine.sku || machine.serialNumber || '').toLowerCase();

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

    const cat = (typeof machine.category === 'string' ? machine.category : machine.category?.name || '').toLowerCase();
    if (cat.includes('pesada') || cat.includes('excav')) return '/images/cat-320.jpg';
    if (cat.includes('elevación') || cat.includes('grúa') || cat.includes('yale')) return '/images/jlg-450aj.jpg';
    if (cat.includes('generador') || cat.includes('energía') || cat.includes('compresor')) return '/images/cummins-50kva.jpg';
    if (cat.includes('eléctrica') || cat.includes('herramienta')) return '/images/makita-18v.jpg';
    if (cat.includes('jardín') || cat.includes('paisajismo')) return '/images/husqvarna-450.jpg';
    return '/images/cat-320.jpg';
  }

  requestQuote(machine: Machine): void {
    this.cartService.addItem({
      machineId: machine.id,
      machineName: machine.name,
      type: 'quote',
      dailyPrice: machine.dailyPrice,
      days: this.rentalDays,
      startDate: this.rentalStartDate,
      endDate: this.rentalEndDate,
      warrantyDeposit: machine.warrantyDepositClp || (machine.dailyPrice * 1.5),
      machine
    });

    this.showToast(`Equipo agregado al dossier de cotización: ${machine.name} (${this.rentalDays} días)`, 'quote');
  }

  rentDirect(machine: Machine): void {
    this.cartService.addItem({
      machineId: machine.id,
      machineName: machine.name,
      type: 'express',
      dailyPrice: machine.dailyPrice,
      days: this.rentalDays,
      startDate: this.rentalStartDate,
      endDate: this.rentalEndDate,
      warrantyDeposit: machine.warrantyDepositClp || (machine.dailyPrice * 1.5),
      machine
    });

    this.showToast(`Herramienta agregada a orden de arriendo express: ${machine.name}`, 'express');
  }

  private showToast(message: string, type: 'quote' | 'express'): void {
    this.notificationMessage = message;
    this.notificationType = type;
    this.showNotification = true;
    setTimeout(() => {
      this.showNotification = false;
      this.cdr.detectChanges();
    }, 4500);
  }

  dismissToast(): void {
    this.showNotification = false;
  }

  private getDefaultDate(daysOffset: number): string {
    const d = new Date();
    d.setDate(d.getDate() + daysOffset);
    return d.toISOString().split('T')[0];
  }
}
