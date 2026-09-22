import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, Rental, Machine } from '../../core/services/api/api.service';
import { AuthService } from '../../core/services/auth/auth.service';

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-rentals',
  styleUrl: './rentals.css',
  templateUrl: './rentals.html',
})
export class Rentals implements OnInit {
  apiService = inject(ApiService);
  authService = inject(AuthService);

  rentals: Rental[] = [];
  machines: Machine[] = []; // for dropdown
  
  // Form model
  newRental = {
    machineId: '',
    startDate: '',
    endDate: ''
  };

  ngOnInit() {
    this.loadRentals();
    if (this.isArrendatario) {
      this.apiService.getCatalog().subscribe(m => this.machines = m.filter(x => x.status === 'DISPONIBLE'));
    }
  }

  loadRentals() {
    this.apiService.getRentals().subscribe(data => this.rentals = data);
  }

  get isArrendatario(): boolean {
    return this.authService.userRole() === 'Arrendatario' || this.authService.userRole() === 'Admin';
  }

  get isJefeBodega(): boolean {
    return this.authService.userRole() === 'JefeBodega' || this.authService.userRole() === 'Admin';
  }

  requestRental() {
    if (!this.newRental.machineId || !this.newRental.startDate || !this.newRental.endDate) {
      alert('Por favor completa todos los campos.');
      return;
    }

    const payload: Partial<Rental> = {
      ...this.newRental,
      status: 'SOLICITADO',
      lesseeId: this.authService.account()?.localAccountId || 'unknown'
    };

    this.apiService.requestRental(payload).subscribe(() => {
      alert('Arriendo solicitado correctamente');
      this.loadRentals();
    });
  }

  updateStatus(rentalId: string, event: Event) {
    const select = event.target as HTMLSelectElement;
    const newStatus = select.value;
    
    this.apiService.updateRentalStatus(rentalId, newStatus).subscribe(() => {
      // Refresh or show toast
    });
  }
}
