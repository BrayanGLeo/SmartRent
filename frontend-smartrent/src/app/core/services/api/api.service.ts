import { Injectable, NgZone, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface Machine {
  id: string;
  name: string;
  description: string;
  category: string;
  status: string; // 'DISPONIBLE', 'EN_MANTENCION', 'ARRENDADO'
  pricePerDay: number;
}

export interface Rental {
  id: string;
  machineId: string;
  lesseeId: string;
  startDate: string;
  endDate: string;
  status: string; // 'SOLICITADO', 'APROBADO', 'EN_PREPARACION', 'EN_TERRENO', 'DEVUELTO', 'RECHAZADO'
}

export interface KpiData {
  rentalsPerDay: number[];
  labels: string[];
}

export interface AuditEvent {
  id: string;
  timestamp: string;
  user: string;
  action: string;
  details: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly ngZone = inject(NgZone);
  private readonly baseUrl = environment.apiBaseUrl;

  private readonly mockCatalog: Machine[] = [
    {
      id: '1',
      name: 'Excavadora Caterpillar 320',
      description: 'Excavadora sobre orugas de 20 toneladas, ideal para construcción pesada.',
      category: 'Excavación',
      status: 'DISPONIBLE',
      pricePerDay: 180000
    },
    {
      id: '2',
      name: 'Retroexcavadora John Deere 310L',
      description: 'Retroexcavadora versátil para trabajos urbanos y agrícolas.',
      category: 'Excavación',
      status: 'DISPONIBLE',
      pricePerDay: 120000
    },
    {
      id: '3',
      name: 'Grúa Horquilla Komatsu 3 Ton',
      description: 'Grúa horquilla diésel de 3 toneladas para movimiento de materiales.',
      category: 'Carga',
      status: 'ARRENDADO',
      pricePerDay: 65000
    }
  ];

  getMockCatalog(): Machine[] {
    return [...this.mockCatalog];
  }

  getCatalog(): Observable<Machine[]> {
    return new Observable<Machine[]>(observer => {
      fetch(`${this.baseUrl}/catalog/services`)
        .then(response => {
          if (!response.ok) throw new Error(`HTTP ${response.status}`);
          return response.json();
        })
        .then((data: any[]) => {
          this.ngZone.run(() => {
            const machines = data.map(m => ({
              id: m.id ? m.id.toString() : crypto.randomUUID(),
              name: m.name,
              description: m.description || 'Sin descripción',
              category: m.category?.name || 'General',
              status: m.isAvailable ? 'DISPONIBLE' : 'ARRENDADO',
              pricePerDay: m.dailyPrice || 0
            }));
            observer.next(machines);
            observer.complete();
          });
        })
        .catch(err => {
          this.ngZone.run(() => {
            observer.error(err);
          });
        });
    });
  }

  addMachine(machine: Partial<Machine>): Observable<Machine> {
    const newMachine = { ...machine, id: crypto.randomUUID() } as Machine;
    this.mockCatalog.push(newMachine);
    return of(newMachine).pipe(delay(800));
  }

  // Rentals
  getRentals(): Observable<Rental[]> {
    return this.http.get<Rental[]>(`${this.baseUrl}/rentals`);
  }

  requestRental(rental: Partial<Rental>): Observable<Rental> {
    return this.http.post<Rental>(`${this.baseUrl}/rentals`, rental);
  }

  updateRentalStatus(id: string, status: string): Observable<Rental> {
    return this.http.put<Rental>(`${this.baseUrl}/rentals/${id}/status`, { status });
  }

  // Reports
  getKpis(): Observable<KpiData> {
    return this.http.get<KpiData>(`${this.baseUrl}/reports/kpis`);
  }

  // Audit
  getAuditTimeline(): Observable<AuditEvent[]> {
    return this.http.get<AuditEvent[]>(`${this.baseUrl}/audit/events`);
  }

  // Cart
  syncCart(items: any[]): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/cart/sync`, items);
  }

  checkoutCart(type: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/cart/checkout/${type}`, {});
  }

  processCheckout(request: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/checkout/process`, request);
  }
}
