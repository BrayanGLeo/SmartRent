import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, Machine } from '../../core/services/api/api.service';
import { AuthService } from '../../core/services/auth/auth.service';

@Component({
  imports: [CommonModule],
  selector: 'app-catalog',
  styleUrl: './catalog.css',
  templateUrl: './catalog.html',
})
export class Catalog implements OnInit {
  apiService = inject(ApiService);
  authService = inject(AuthService);
  
  machines: Machine[] = [];

  ngOnInit() {
    this.apiService.getCatalog().subscribe(
      data => {
        this.machines = data;
      }
    );
  }

  get isAdmin(): boolean {
    return this.authService.userRole() === 'Admin';
  }

  addMachine() {
    // Para simplificar, agregamos una máquina por defecto
    // En un escenario real, esto abriría un modal con un formulario
    const newMachine = {
      name: 'Nueva Retroexcavadora',
      description: 'Modelo XYZ 2024',
      category: 'Excavación',
      status: 'DISPONIBLE',
      pricePerDay: 150000
    };

    this.apiService.addMachine(newMachine).subscribe(
      machine => {
        this.machines.push(machine);
        alert('Máquina agregada exitosamente');
      }
    );
  }
}
