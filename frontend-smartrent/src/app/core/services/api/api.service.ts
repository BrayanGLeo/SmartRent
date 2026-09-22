import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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
  private http = inject(HttpClient);
  private baseUrl = environment.apiBaseUrl; // e.g. http://localhost:8080/api

  // Catalog
  getCatalog(): Observable<Machine[]> {
    return this.http.get<Machine[]>(`${this.baseUrl}/catalog`);
  }

  addMachine(machine: Partial<Machine>): Observable<Machine> {
    return this.http.post<Machine>(`${this.baseUrl}/catalog`, machine);
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
}
