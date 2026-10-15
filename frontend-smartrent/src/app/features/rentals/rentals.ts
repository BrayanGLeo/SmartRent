import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, Rental, Machine, RentalStatus } from '../../core/services/api/api.service';
import { AuthService } from '../../core/services/auth/auth.service';
import { RENTAL_STATUS_MAP, RentalStatusConfig } from '../../core/models';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule],
  selector: 'app-rentals',
  styleUrl: './rentals.css',
  templateUrl: './rentals.html',
})
export class Rentals implements OnInit {
  apiService = inject(ApiService);
  authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  rentals: Rental[] = [];
  machines: Machine[] = [];
  selectedFilter: string = 'TODOS';
  searchTerm: string = '';

  // Formulario nuevo arriendo
  newRental = {
    machineId: '',
    startDate: this.getDefaultDate(0),
    endDate: this.getDefaultDate(7),
    workSiteAddress: 'Faena Lo Boza Módulo B, Pudahuel'
  };

  showNewModal = false;
  toastMessage = '';
  showToast = false;

  readonly statusMap = RENTAL_STATUS_MAP;

  readonly availableStatuses: RentalStatus[] = [
    'SOLICITADO',
    'APROBADO',
    'EN_PREPARACION',
    'EN_TERRENO',
    'DEVUELTO',
    'CANCELADO'
  ];

  ngOnInit() {
    this.loadRentals();
    this.loadMachines();
  }

  loadMachines() {
    this.apiService.getCatalog().subscribe({
      next: (data) => {
        this.machines = data.length > 0 ? data : this.apiService.getMockCatalog();
        this.cdr.detectChanges();
      },
      error: () => {
        this.machines = this.apiService.getMockCatalog();
        this.cdr.detectChanges();
      }
    });
  }

  loadRentals() {
    this.apiService.getRentals().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.rentals = this.enrichRentals(data);
        } else {
          this.rentals = this.getMockRentals();
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.rentals = this.getMockRentals();
        this.cdr.detectChanges();
      }
    });
  }

  private enrichRentals(data: Rental[]): Rental[] {
    return data.map(r => {
      const machine = this.machines.find(m => String(m.id) === String(r.machineId));
      return {
        ...r,
        machineName: machine ? machine.name : `Equipo Serie #${r.machineId}`,
        machine,
        dailyRate: machine ? machine.dailyPrice : 75000,
        workSiteAddress: r.workSiteAddress || 'Faena Central RM'
      };
    });
  }

  private getMockRentals(): Rental[] {
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
        totalDays: 14,
        totalCost: 2100000,
        workSiteAddress: 'Autopista Américo Vespucio Oriente Tramo II'
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
        totalDays: 7,
        totalCost: 840000,
        workSiteAddress: 'Mina Los Pelambres - Campamento Chacay'
      },
      {
        id: 1044,
        machineId: 7,
        userId: 'usr_corp_03',
        machineName: 'Plataforma Articulada JLG 450AJ',
        lesseeName: 'Ingeniería y Construcción Sigdo Koppers',
        startDate: '2026-10-16',
        endDate: '2026-10-23',
        status: 'EN_PREPARACION',
        dailyRate: 95000,
        totalDays: 7,
        totalCost: 665000,
        workSiteAddress: 'Planta Celulosa Arauco'
      },
      {
        id: 1045,
        machineId: 9,
        userId: 'usr_corp_04',
        machineName: 'Generador Diésel Cummins 50kVA Insonorizado',
        lesseeName: 'SalfaCorp Infraestructura',
        startDate: '2026-10-15',
        endDate: '2026-10-22',
        status: 'APROBADO',
        dailyRate: 60000,
        totalDays: 7,
        totalCost: 420000,
        workSiteAddress: 'Túnel El Melón II'
      },
      {
        id: 1046,
        machineId: 3,
        userId: 'usr_corp_05',
        machineName: 'Rodillo Compactador Bomag BW 211',
        lesseeName: 'Vialidad y Construcciones Claro Vicuña',
        startDate: '2026-10-18',
        endDate: '2026-10-28',
        status: 'SOLICITADO',
        dailyRate: 85000,
        totalDays: 10,
        totalCost: 850000,
        workSiteAddress: 'Ruta 160 San Pedro - Coronel'
      },
      {
        id: 1047,
        machineId: 5,
        userId: 'usr_corp_06',
        machineName: 'Demoledor Bosch GSH 11 E Professional',
        lesseeName: 'Constructora Moller y Pérez-Cotapos',
        startDate: '2026-10-01',
        endDate: '2026-10-08',
        status: 'DEVUELTO',
        dailyRate: 25000,
        totalDays: 7,
        totalCost: 175000,
        workSiteAddress: 'Edificio Los Conquistadores, Providencia'
      }
    ];
  }

  get filteredRentals(): Rental[] {
    let list = [...this.rentals];

    if (this.selectedFilter !== 'TODOS') {
      list = list.filter(r => r.status === this.selectedFilter);
    }

    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase().trim();
      list = list.filter(r =>
        String(r.id).includes(q) ||
        (r.machineName && r.machineName.toLowerCase().includes(q)) ||
        (r.lesseeName && r.lesseeName.toLowerCase().includes(q)) ||
        (r.workSiteAddress && r.workSiteAddress.toLowerCase().includes(q))
      );
    }

    return list;
  }

  get isArrendatario(): boolean {
    return this.authService.userRole() === 'Arrendatario' || this.authService.userRole() === 'Administrador';
  }

  get isJefeBodega(): boolean {
    return this.authService.userRole() === 'JefeBodega' || this.authService.userRole() === 'Administrador';
  }

  getStatusConfig(status: string): RentalStatusConfig {
    return this.statusMap[status] || {
      label: status,
      badgeClass: 'badge-neutral',
      description: 'Estado del arriendo'
    };
  }

  requestRental(): void {
    if (!this.newRental.machineId || !this.newRental.startDate || !this.newRental.endDate) {
      alert('Por favor completa todos los campos del contrato.');
      return;
    }

    const selectedMachine = this.machines.find(m => String(m.id) === String(this.newRental.machineId));
    const newEntry: Rental = {
      id: Math.floor(1050 + Math.random() * 100),
      machineId: this.newRental.machineId,
      userId: this.authService.account()?.localAccountId || 'usr_client',
      machineName: selectedMachine ? selectedMachine.name : 'Equipo Industrial',
      lesseeName: this.authService.account()?.name || 'Cliente Corporativo',
      startDate: this.newRental.startDate,
      endDate: this.newRental.endDate,
      status: 'SOLICITADO',
      dailyRate: selectedMachine ? selectedMachine.dailyPrice : 50000,
      totalDays: 7,
      workSiteAddress: this.newRental.workSiteAddress
    };

    this.rentals.unshift(newEntry);
    this.showNewModal = false;
    this.displayToast(`Arriendo #${newEntry.id} solicitado exitosamente para validación.`);
  }

  updateStatus(rentalId: string | number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const newStatus = select.value as RentalStatus;

    this.apiService.updateRentalStatus(rentalId, newStatus).subscribe({
      next: () => {
        this.applyLocalStatus(rentalId, newStatus);
      },
      error: () => {
        // Tolerancia offline / local state update
        this.applyLocalStatus(rentalId, newStatus);
      }
    });
  }

  private applyLocalStatus(rentalId: string | number, newStatus: string): void {
    const idx = this.rentals.findIndex(r => String(r.id) === String(rentalId));
    if (idx >= 0) {
      this.rentals[idx] = { ...this.rentals[idx], status: newStatus };
      this.displayToast(`Estado del arriendo #${rentalId} actualizado a ${newStatus}.`);
      this.cdr.detectChanges();
    }
  }

  private displayToast(msg: string): void {
    this.toastMessage = msg;
    this.showToast = true;
    setTimeout(() => {
      this.showToast = false;
      this.cdr.detectChanges();
    }, 4000);
  }

  private getDefaultDate(daysOffset: number): string {
    const d = new Date();
    d.setDate(d.getDate() + daysOffset);
    return d.toISOString().split('T')[0];
  }
}
