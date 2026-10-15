import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../core/services/auth/auth.service';
import { ApiService, Rental, Machine } from '../core/services/api/api.service';
import { CartService } from '../core/services/cart/cart.service';
import { RENTAL_STATUS_MAP } from '../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  authService = inject(AuthService);
  apiService = inject(ApiService);
  cartService = inject(CartService);

  recentRentals: Rental[] = [];
  fleetSummary = {
    totalUnits: 36,
    activeUnits: 28,
    maintenanceUnits: 4,
    availableUnits: 4,
    utilizationRate: 77.8
  };

  readonly statusMap = RENTAL_STATUS_MAP;

  ngOnInit(): void {
    this.apiService.getRentals().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.recentRentals = data.slice(0, 5);
        } else {
          this.recentRentals = this.getMockRecentRentals();
        }
      },
      error: () => {
        this.recentRentals = this.getMockRecentRentals();
      }
    });
  }

  private getMockRecentRentals(): Rental[] {
    return [
      {
        id: 1042,
        machineId: 1,
        userId: 'usr_corp_01',
        machineName: 'Excavadora Oruga Cat 320',
        lesseeName: 'Constructora Echeverría Izquierdo S.A.',
        startDate: '2026-10-10',
        endDate: '2026-10-24',
        status: 'EN_TERRENO',
        dailyRate: 150000,
        workSiteAddress: 'Autopista AVO II Tramo B'
      },
      {
        id: 1043,
        machineId: 2,
        userId: 'usr_corp_02',
        machineName: 'Retroexcavadora John Deere 310L',
        lesseeName: 'Besalco Minería S.A.',
        startDate: '2026-10-12',
        endDate: '2026-10-19',
        status: 'EN_TERRENO',
        dailyRate: 120000,
        workSiteAddress: 'Mina Los Pelambres'
      },
      {
        id: 1044,
        machineId: 7,
        userId: 'usr_corp_03',
        machineName: 'Plataforma Articulada JLG 450AJ',
        lesseeName: 'Sigdo Koppers',
        startDate: '2026-10-16',
        endDate: '2026-10-23',
        status: 'EN_PREPARACION',
        dailyRate: 95000,
        workSiteAddress: 'Planta Celulosa Arauco'
      }
    ];
  }
}
