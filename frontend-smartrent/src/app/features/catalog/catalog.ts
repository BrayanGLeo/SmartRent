import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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
  router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  
  machines: Machine[] = [];

  ngOnInit() {
    this.apiService.getCatalog().subscribe({
      next: (data) => {
        this.machines = data;
        this.cdr.detectChanges();
      },
      error: () => {
        // Si la API falla (ej: 401 del API Gateway sin autenticación),
        // usamos datos mock para que el catálogo sea visible sin login.
        this.machines = this.apiService.getMockCatalog();
        this.cdr.detectChanges();
      }
    });
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

  isHeavyMachinery(category: string): boolean {
    const heavyCategories = ['Maquinaria Pesada', 'Equipos de Elevación', 'Generadores', 'Excavación', 'Carga'];
    return heavyCategories.includes(category);
  }

  getImageUrl(machine: Machine): string {
    const cat = machine.category.toLowerCase();
    if (cat.includes('excavación')) return '/images/excavator.jpg';
    if (cat.includes('carga')) return '/images/loader.jpg';
    if (cat.includes('elevación') || cat.includes('grúa')) return '/images/crane.jpg';
    return '/images/generator.jpg'; // Default heavy machinery
  }

  async requestQuote(machine: Machine) {
    // Guardar en el carrito (localStorage temporalmente hasta construir el microservicio)
    const cart = JSON.parse(localStorage.getItem('smartrent_cart') || '[]');
    cart.push({ machine, type: 'quote' });
    localStorage.setItem('smartrent_cart', JSON.stringify(cart));
    
    alert(`Máquina agregada al portafolio de cotización: ${machine.name}`);
    await this.router.navigate(['/cart']);
  }

  async rentDirect(machine: Machine) {
    // Guardar en el carrito (localStorage temporalmente)
    const cart = JSON.parse(localStorage.getItem('smartrent_cart') || '[]');
    cart.push({ machine, type: 'express' });
    localStorage.setItem('smartrent_cart', JSON.stringify(cart));
    
    alert(`Herramienta agregada al carrito de arriendo rápido: ${machine.name}`);
    await this.router.navigate(['/cart']);
  }
}
