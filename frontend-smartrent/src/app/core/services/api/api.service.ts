import { Injectable, NgZone, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import {
  Machine,
  Category,
  Rental,
  RentalStatus,
  KpiData,
  AuditEvent,
  AuditTimeline,
  CartItem,
  CheckoutEvent,
  CheckoutRequest,
  Order,
  OrderCreatedEvent
} from '../../models';

export type {
  Machine,
  Category,
  Rental,
  RentalStatus,
  KpiData,
  AuditEvent,
  AuditTimeline,
  CartItem,
  CheckoutEvent,
  CheckoutRequest,
  Order,
  OrderCreatedEvent
};

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly ngZone = inject(NgZone);
  private readonly baseUrl = environment.apiBaseUrl;

  private readonly mockCatalog: Machine[] = [
    {
      id: 1,
      name: 'Excavadora Oruga Cat 320',
      serialNumber: 'CAT-320-001',
      category: 'Maquinaria Pesada',
      dailyPrice: 150000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 150000,
      brand: 'Caterpillar',
      modelYear: 2023,
      warrantyDepositClp: 1500000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'EQ-CAT-320',
      description: 'Excavadora hidráulica sobre orugas de 22.5 toneladas métricas. Motor Cat C4.4 de 122 kW (164 hp). Profundidad máxima de excavación 6.72 m y balde de 1.19 m³.',
      specs: {
        operatingWeightKg: 22500,
        powerHp: 164,
        maxReachMeters: 9.86,
        fuelType: 'DIESEL',
        bucketCapacityM3: 1.19,
        brand: 'Caterpillar',
        year: 2023
      }
    },
    {
      id: 2,
      name: 'Retroexcavadora John Deere 310L',
      serialNumber: 'JD-310L-042',
      category: 'Maquinaria Pesada',
      dailyPrice: 120000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 120000,
      brand: 'John Deere',
      modelYear: 2022,
      warrantyDepositClp: 1200000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'EQ-JD-310L',
      description: 'Retroexcavadora 4x4 versátil de 7.5 toneladas. Motor diésel John Deere PowerTech 4.5L de 86 hp. Balde frontal de 0.96 m³ y brazo extensible a 4.3 m.',
      specs: {
        operatingWeightKg: 7490,
        powerHp: 86,
        maxReachMeters: 4.3,
        fuelType: 'DIESEL',
        bucketCapacityM3: 0.96,
        brand: 'John Deere',
        year: 2022
      }
    },
    {
      id: 3,
      name: 'Rodillo Compactador Bomag BW 211',
      serialNumber: 'BMG-RC-909',
      category: 'Maquinaria Pesada',
      dailyPrice: 85000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 85000,
      brand: 'Bomag',
      modelYear: 2022,
      warrantyDepositClp: 900000,
      locationDepot: 'Patio La Negra (Antofagasta)',
      sku: 'EQ-BMG-211',
      description: 'Rodillo vibratorio monocilíndrico de 11.2 toneladas para compactación de suelos y bases granulares. Ancho de rodillo 2.13 m con sistema Terrameter.',
      specs: {
        operatingWeightKg: 11200,
        powerHp: 130,
        fuelType: 'DIESEL',
        brand: 'Bomag',
        year: 2022
      }
    },
    {
      id: 4,
      name: 'Taladro Percutor Makita 18V LXT',
      serialNumber: 'MAK-TP-01',
      category: 'Herramientas Eléctricas',
      dailyPrice: 15000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 15000,
      brand: 'Makita',
      modelYear: 2024,
      warrantyDepositClp: 150000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'HER-MAK-18V',
      description: 'Taladro percutor inalámbrico Brushless con 2 baterías de 5.0Ah y cargador rápido. Torque máximo 115 Nm, 2 velocidades y mandril metálico de 13 mm.',
      specs: {
        operatingWeightKg: 2.7,
        powerHp: 2,
        fuelType: 'BATERIA_ION_LITIO',
        brand: 'Makita',
        year: 2024
      }
    },
    {
      id: 5,
      name: 'Demoledor Bosch GSH 11 E Professional',
      serialNumber: 'BOS-GSH-11E',
      category: 'Herramientas Eléctricas',
      dailyPrice: 25000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 25000,
      brand: 'Bosch',
      modelYear: 2023,
      warrantyDepositClp: 300000,
      locationDepot: 'Patio San Pedro (Concepción)',
      sku: 'HER-BOS-11E',
      description: 'Martillo demoledor de 11 kg encastre SDS-Max. Potencia 1.500 W con energía de impacto de 16.8 Joules. Sistema Vario-Lock de 12 posiciones de cincel.',
      specs: {
        operatingWeightKg: 11.0,
        powerHp: 2,
        fuelType: 'ELECTRICO_TRIFASICO',
        brand: 'Bosch',
        year: 2023
      }
    },
    {
      id: 6,
      name: 'Sierra Circular DeWalt DWE575',
      serialNumber: 'DW-SC-700',
      category: 'Herramientas Eléctricas',
      dailyPrice: 12000,
      isAvailable: false,
      status: 'ARRENDADO',
      pricePerDay: 12000,
      brand: 'DeWalt',
      modelYear: 2023,
      warrantyDepositClp: 120000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'HER-DW-700',
      description: 'Sierra circular ligera de 7-1/4" (184 mm). Potencia motor 1.600W a 5.200 RPM con base de aluminio resistente a impactos.',
      specs: {
        operatingWeightKg: 4.0,
        powerHp: 2.1,
        brand: 'DeWalt',
        year: 2023
      }
    },
    {
      id: 7,
      name: 'Plataforma Articulada JLG 450AJ',
      serialNumber: 'JLG-450-X',
      category: 'Equipos de Elevación',
      dailyPrice: 95000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 95000,
      brand: 'JLG',
      modelYear: 2022,
      warrantyDepositClp: 1000000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'EQ-JLG-450',
      description: 'Brazo articulado diésel 4x4 con altura de trabajo de 15.77 m y alcance horizontal de 7.62 m. Capacidad de plataforma 250 kg para 2 operarios.',
      specs: {
        operatingWeightKg: 6490,
        loadCapacityKg: 250,
        maxReachMeters: 15.77,
        powerHp: 49,
        fuelType: 'DIESEL',
        brand: 'JLG',
        year: 2022
      }
    },
    {
      id: 8,
      name: 'Montacargas Yale 3 Toneladas Diésel',
      serialNumber: 'YAL-MC-03',
      category: 'Equipos de Elevación',
      dailyPrice: 75000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 75000,
      brand: 'Yale',
      modelYear: 2023,
      warrantyDepositClp: 800000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'EQ-YAL-3T',
      description: 'Grúa horquilla diésel de 3.000 kg de capacidad con mástil triple triplex de 4.8 m de elevación, desplazador lateral y neumáticos todo terreno.',
      specs: {
        operatingWeightKg: 4650,
        loadCapacityKg: 3000,
        maxReachMeters: 4.8,
        powerHp: 58,
        fuelType: 'DIESEL',
        brand: 'Yale',
        year: 2023
      }
    },
    {
      id: 9,
      name: 'Generador Diésel Cummins 50kVA Insonorizado',
      serialNumber: 'CUM-50-GEN',
      category: 'Generadores y Energía',
      dailyPrice: 60000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 60000,
      brand: 'Cummins',
      modelYear: 2023,
      warrantyDepositClp: 700000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'EQ-CUM-50K',
      description: 'Grupo electrógeno trifásico 380V/220V insonorizado (68 dBA a 7m). Motor Cummins 4BTA3.9 con estanque de combustible de 150L y autonomía de 16h continuas.',
      specs: {
        operatingWeightKg: 1350,
        powerHp: 67,
        noiseLevelDb: 68,
        fuelType: 'DIESEL',
        brand: 'Cummins',
        year: 2023
      }
    },
    {
      id: 10,
      name: 'Generador Portátil Honda EU2200i Inverter',
      serialNumber: 'HON-EU-22',
      category: 'Generadores y Energía',
      dailyPrice: 20000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 20000,
      brand: 'Honda',
      modelYear: 2024,
      warrantyDepositClp: 200000,
      locationDepot: 'Patio San Pedro (Concepción)',
      sku: 'EQ-HON-2200',
      description: 'Generador inverter ultrasilencioso (48-57 dBA) de 2.200W pico. Ideal para equipos electrónicos de precisión, estaciones topográficas y oficinas de faena.',
      specs: {
        operatingWeightKg: 21.1,
        powerHp: 3.6,
        noiseLevelDb: 53,
        fuelType: 'BENCINA',
        brand: 'Honda',
        year: 2024
      }
    },
    {
      id: 11,
      name: 'Compresor de Aire Sullair 185 CFM',
      serialNumber: 'SUL-185-AIR',
      category: 'Generadores y Energía',
      dailyPrice: 45000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 45000,
      brand: 'Sullair',
      modelYear: 2022,
      warrantyDepositClp: 500000,
      locationDepot: 'Patio La Negra (Antofagasta)',
      sku: 'EQ-SUL-185',
      description: 'Compresor de tornillo portátil de 185 CFM a 100 PSI montado sobre tráiler de remolque. Motor diésel Caterpillar C2.2 para martillos neumáticos y arenado.',
      specs: {
        operatingWeightKg: 1020,
        powerHp: 49,
        fuelType: 'DIESEL',
        brand: 'Sullair',
        year: 2022
      }
    },
    {
      id: 12,
      name: 'Motosierra Profesional Husqvarna 450 Rancher',
      serialNumber: 'HUS-450-MS',
      category: 'Jardinería y Paisajismo',
      dailyPrice: 18000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 18000,
      brand: 'Husqvarna',
      modelYear: 2023,
      warrantyDepositClp: 180000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'HER-HUS-450',
      description: 'Motosierra con motor X-Torq de 50.2 cc y espada de 18 pulgadas (45 cm). Sistema LowVib de amortiguación para corte de tala y despeje de predios.',
      specs: {
        operatingWeightKg: 5.1,
        powerHp: 3.2,
        fuelType: 'BENCINA',
        brand: 'Husqvarna',
        year: 2023
      }
    },
    {
      id: 13,
      name: 'Cortadora de Césped Industrial Honda HRN216',
      serialNumber: 'HON-CP-11',
      category: 'Jardinería y Paisajismo',
      dailyPrice: 15000,
      isAvailable: true,
      status: 'DISPONIBLE',
      pricePerDay: 15000,
      brand: 'Honda',
      modelYear: 2024,
      warrantyDepositClp: 150000,
      locationDepot: 'Patio Pudahuel (Santiago)',
      sku: 'HER-HON-CP11',
      description: 'Cortacésped autopropulsado con motor GCV170 de 166 cc. Chasis de acero de 21" con doble cuchilla MicroCut Twin Blades y bolsa colectora de 66L.',
      specs: {
        operatingWeightKg: 36.0,
        powerHp: 4.8,
        fuelType: 'BENCINA',
        brand: 'Honda',
        year: 2024
      }
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
            const machines: Machine[] = data.map(m => ({
              id: m.id ? m.id.toString() : crypto.randomUUID(),
              name: m.name,
              serialNumber: m.serialNumber,
              description: m.description || 'Sin descripción',
              category: m.category?.name || 'General',
              status: m.isAvailable ? 'DISPONIBLE' : 'ARRENDADO',
              isAvailable: !!m.isAvailable,
              dailyPrice: m.dailyPrice || 0,
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

  updateRentalStatus(id: string | number, status: string): Observable<Rental> {
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
