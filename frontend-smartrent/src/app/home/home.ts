import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../core/services/auth/auth.service';

interface TechnicalCategory {
  id: string;
  name: string;
  slug: string;
  count: number;
  featuredMachines: string[];
  keySpec: string;
  badge: string;
}

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  selector: 'app-home',
  styleUrl: './home.css',
  templateUrl: './home.html',
})
export class Home {
  authService = inject(AuthService);
  router = inject(Router);

  // Formulario del buscador rápido por fecha y categoría
  searchForm = {
    category: '',
    startDate: this.getDefaultStartDate(),
    endDate: this.getDefaultEndDate(),
    depot: 'STGO_CENTRAL'
  };

  categories: TechnicalCategory[] = [
    {
      id: 'heavy',
      name: 'Maquinaria Pesada',
      slug: 'Maquinaria Pesada',
      count: 28,
      featuredMachines: ['Excavadora Cat 320 (22T)', 'Retroexcavadora John Deere 310L', 'Rodillo Bomag BW 211'],
      keySpec: 'Desde 120 kW • Peso hasta 24 Ton',
      badge: 'Faena Pesada'
    },
    {
      id: 'lifting',
      name: 'Equipos de Elevación',
      slug: 'Equipos de Elevación',
      count: 34,
      featuredMachines: ['Plataforma Articulada JLG 450AJ', 'Montacargas Yale 3T', 'Tijera Eléctrica Genie 19ft'],
      keySpec: 'Alturas de 6m a 28m • Certificación SEC',
      badge: 'Trabajo en Altura'
    },
    {
      id: 'power',
      name: 'Generación y Energía',
      slug: 'Generadores y Energía',
      count: 42,
      featuredMachines: ['Generador Diésel Cummins 50 kVA', 'Compresor Sullair 185 CFM', 'Torre Iluminación Atlas Copco'],
      keySpec: 'Cabinados insonorizados • Autonomía 24h',
      badge: 'Soporte Continuo'
    },
    {
      id: 'tools',
      name: 'Herramientas Eléctricas',
      slug: 'Herramientas Eléctricas',
      count: 55,
      featuredMachines: ['Demoledor Bosch GSH 11 E', 'Taladro Percutor Makita 18V', 'Sierra Circular DeWalt'],
      keySpec: 'Encastre SDS-Max / Batería Li-Ion',
      badge: 'Arriendo Express'
    }
  ];

  depots = [
    { id: 'STGO_CENTRAL', name: 'Patio Pudahuel (Región Metropolitana)' },
    { id: 'ANTOFAGASTA', name: 'Patio La Negra (Antofagasta)' },
    { id: 'CONCEPCION', name: 'Patio San Pedro (Biobío)' }
  ];

  private getDefaultStartDate(): string {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }

  private getDefaultEndDate(): string {
    const future = new Date();
    future.setDate(future.getDate() + 7);
    return future.toISOString().split('T')[0];
  }

  onQuickSearch(): void {
    if (this.authService.userRole() === 'Administrador') {
      void this.router.navigate(['/admin']);
      return;
    }
    const queryParams: Record<string, string> = {};
    if (this.searchForm.category) {
      queryParams['category'] = this.searchForm.category;
    }
    if (this.searchForm.startDate) {
      queryParams['startDate'] = this.searchForm.startDate;
    }
    if (this.searchForm.endDate) {
      queryParams['endDate'] = this.searchForm.endDate;
    }
    if (this.searchForm.depot) {
      queryParams['depot'] = this.searchForm.depot;
    }

    void this.router.navigate(['/catalog'], { queryParams });
  }

  filterByCategory(categoryName: string): void {
    if (this.authService.userRole() === 'Administrador') {
      void this.router.navigate(['/admin']);
      return;
    }
    void this.router.navigate(['/catalog'], {
      queryParams: { category: categoryName }
    });
  }
}
