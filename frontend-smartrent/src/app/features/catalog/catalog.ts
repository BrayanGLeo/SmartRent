import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService, Machine } from '../../core/services/api/api.service';
import { AuthService } from '../../core/services/auth/auth.service';
import { CartService } from '../../core/services/cart/cart.service';
import { AccessDeniedService } from '../../core/services/modal/access-denied.service';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  selector: 'app-catalog',
  styleUrl: './catalog.css',
  templateUrl: './catalog.html',
})
export class Catalog implements OnInit {
  readonly apiService = inject(ApiService);
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  readonly accessDeniedService = inject(AccessDeniedService);
  readonly router = inject(Router);
  readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

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
    if (this.authService.userRole() === 'Administrador') {
      this.accessDeniedService.show('No tiene permiso para acceder a esta página.', '/');
      void this.router.navigate(['/']);
      return;
    }

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
        m.name?.toLowerCase().includes(term) ||
        m.serialNumber?.toLowerCase().includes(term) ||
        m.sku?.toLowerCase().includes(term) ||
        m.brand?.toLowerCase().includes(term) ||
        m.description?.toLowerCase().includes(term)
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
    return this.authService.userRole() === 'Administrador';
  }

  isHeavyMachinery(category: any): boolean {
    const catName = typeof category === 'string' ? category : category?.name || '';
    const heavyCategories = ['Maquinaria Pesada', 'Equipos de Elevación', 'Generadores'];
    return heavyCategories.some(h => catName.toLowerCase().includes(h.toLowerCase()));
  }

  private static readonly IMAGE_MAPPINGS = [
    { patterns: ['cat', '320', 'cat-320', 'pesada', 'excav'], url: '/images/cat-320.jpg' },
    { patterns: ['deere', '310', 'jd-310'], url: '/images/jd-310l.jpg' },
    { patterns: ['bomag', '211', 'bmg'], url: '/images/bomag-bw211.jpg' },
    { patterns: ['makita', 'mak', 'eléctrica', 'herramienta'], url: '/images/makita-18v.jpg' },
    { patterns: ['bosch', 'gsh', 'bos'], url: '/images/bosch-gsh11e.jpg' },
    { patterns: ['dewalt', 'circular', 'dw'], url: '/images/dewalt-dwe575.jpg' },
    { patterns: ['jlg', '450', 'elevación', 'grúa'], url: '/images/jlg-450aj.jpg' },
    { patterns: ['yale', 'montacargas', 'yal'], url: '/images/yale-3t.jpg' },
    { patterns: ['cummins', 'cum', 'generador', 'energía', 'compresor'], url: '/images/cummins-50kva.jpg' },
    { patterns: ['eu22', 'inverter', 'hon-eu'], url: '/images/honda-eu2200i.jpg' },
    { patterns: ['sullair', '185', 'sul'], url: '/images/sullair-185.jpg' },
    { patterns: ['husqvarna', 'motosierra', 'hus', 'jardín', 'paisajismo'], url: '/images/husqvarna-450.jpg' },
    { patterns: ['hrn', 'pasto', 'césped', 'hon-cp'], url: '/images/honda-hrn216.jpg' }
  ];

  getImageUrl(machine: Machine): string {
    if (machine.imageUrl) return machine.imageUrl;

    const catName = typeof machine.category === 'string' ? machine.category : machine.category?.name || '';
    const text = `${machine.name || ''} ${machine.sku || ''} ${machine.serialNumber || ''} ${catName}`.toLowerCase();

    const match = Catalog.IMAGE_MAPPINGS.find(m => m.patterns.some(p => text.includes(p)));
    return match ? match.url : '/images/cat-320.jpg';
  }

  requestQuote(machine: Machine): void {
    if (!machine.isAvailable || (machine.availableStock !== undefined && machine.availableStock <= 0)) {
      this.showToast(`Equipo sin stock disponible en patio: ${machine.name}. No es posible generar pedidos.`, 'quote');
      return;
    }

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
    if (!machine.isAvailable || (machine.availableStock !== undefined && machine.availableStock <= 0)) {
      this.showToast(`Stock agotado: ${machine.name} se encuentra en faena y no admite nuevos pedidos.`, 'express');
      return;
    }

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
