import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, AuditEvent, AuditTimeline } from '../../core/services/api/api.service';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule],
  selector: 'app-audit',
  styleUrl: './audit.css',
  templateUrl: './audit.html',
})
export class Audit implements OnInit {
  apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);

  events: AuditTimeline[] = [];
  selectedFilter: string = 'TODOS';
  searchTerm: string = '';
  expandedEventId: number | null = null;
  viewMode: 'timeline' | 'table' = 'timeline';

  eventTypes: string[] = [
    'TODOS',
    'ORDER_CREATED',
    'RENTAL_STATUS_CHANGED',
    'MACHINE_DISPATCHED',
    'INVENTORY_MAINTENANCE',
    'CART_CHECKOUT_INITIATED'
  ];

  ngOnInit() {
    this.loadTimeline();
  }

  loadTimeline() {
    this.apiService.getAuditTimeline().subscribe({
      next: (data: any) => {
        if (data && data.length > 0) {
          this.events = this.parseEvents(data);
        } else {
          this.events = this.getMockAuditTimeline();
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.events = this.getMockAuditTimeline();
        this.cdr.detectChanges();
      }
    });
  }

  private parseEvents(data: any[]): AuditTimeline[] {
    return data.map(item => {
      let parsedPayload: Record<string, unknown> | null = null;
      try {
        parsedPayload = JSON.parse(item.eventPayload || '{}');
      } catch {
        parsedPayload = { raw: item.eventPayload };
      }

      return {
        id: item.id || Math.floor(Math.random() * 1000),
        eventType: item.eventType || item.action || 'AUDIT_LOG',
        eventPayload: item.eventPayload || item.details || '{}',
        timestamp: item.timestamp || new Date().toISOString(),
        originService: this.resolveOriginService(item.eventType),
        severity: this.resolveSeverity(item.eventType),
        parsedPayload
      };
    });
  }

  private resolveOriginService(eventType: string = ''): string {
    const t = eventType.toUpperCase();
    if (t.includes('ORDER') || t.includes('CHECKOUT')) return 'ms-smartrent-checkout';
    if (t.includes('RENTAL') || t.includes('DISPATCH')) return 'ms-smartrent-rentals';
    if (t.includes('MACHINE') || t.includes('INVENTORY')) return 'ms-smartrent-catalog';
    if (t.includes('CART')) return 'ms-smartrent-cart';
    return 'ms-smartrent-bff';
  }

  private resolveSeverity(eventType: string = ''): 'INFO' | 'WARN' | 'ERROR' | 'AUDIT' {
    const t = eventType.toUpperCase();
    if (t.includes('CANCEL') || t.includes('FAIL')) return 'ERROR';
    if (t.includes('MAINTENANCE') || t.includes('OVERDUE')) return 'WARN';
    if (t.includes('ORDER') || t.includes('STATUS')) return 'AUDIT';
    return 'INFO';
  }

  get filteredEvents(): AuditTimeline[] {
    let list = [...this.events];

    if (this.selectedFilter !== 'TODOS') {
      list = list.filter(e => e.eventType === this.selectedFilter);
    }

    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase().trim();
      list = list.filter(e =>
        e.eventType.toLowerCase().includes(q) ||
        e.eventPayload.toLowerCase().includes(q) ||
        (e.originService && e.originService.toLowerCase().includes(q))
      );
    }

    return list;
  }

  toggleExpand(id: number): void {
    this.expandedEventId = this.expandedEventId === id ? null : id;
  }

  getExpandedPayload(): string {
    const found = this.events.find(e => e.id === this.expandedEventId);
    return found ? found.eventPayload : '';
  }

  private getMockAuditTimeline(): AuditTimeline[] {
    return [
      {
        id: 9801,
        eventType: 'ORDER_CREATED',
        timestamp: '2026-10-15T03:30:12Z',
        originService: 'ms-smartrent-checkout',
        severity: 'AUDIT',
        eventPayload: JSON.stringify({
          orderId: 1045,
          userId: 'usr_corp_04',
          rut: '76.840.120-K',
          name: 'SalfaCorp Infraestructura',
          customerType: 'COMPANY',
          orderType: 'EXPRESS',
          deliveryType: 'DISPATCH',
          deliveryAddress: 'Túnel El Melón II, Km 128',
          items: [
            { machineId: 9, machineName: 'Generador Diésel Cummins 50kVA Insonorizado', dailyPrice: 60000, days: 7 }
          ],
          totalEstimatedClp: 420000
        }, null, 2)
      },
      {
        id: 9802,
        eventType: 'RENTAL_STATUS_CHANGED',
        timestamp: '2026-10-15T02:15:44Z',
        originService: 'ms-smartrent-rentals',
        severity: 'INFO',
        eventPayload: JSON.stringify({
          rentalId: 1044,
          machineId: 7,
          previousStatus: 'APROBADO',
          newStatus: 'EN_PREPARACION',
          depotId: 'STGO_CENTRAL',
          operatorUser: 'jefe.bodega@smartrent.cl',
          inspectionChecklistId: 'CHK-4401-OK'
        }, null, 2)
      },
      {
        id: 9803,
        eventType: 'MACHINE_DISPATCHED',
        timestamp: '2026-10-14T20:45:00Z',
        originService: 'ms-smartrent-rentals',
        severity: 'INFO',
        eventPayload: JSON.stringify({
          rentalId: 1042,
          machineId: 1,
          machineSerialNumber: 'CAT-320-001',
          transportType: 'CAMA_BAJA_35T',
          carrierRut: '79.230.100-4',
          driverName: 'Hernán Carrasco M.',
          destinationWorkSite: 'Autopista AVO II Tramo B'
        }, null, 2)
      },
      {
        id: 9804,
        eventType: 'INVENTORY_MAINTENANCE',
        timestamp: '2026-10-14T14:10:33Z',
        originService: 'ms-smartrent-catalog',
        severity: 'WARN',
        eventPayload: JSON.stringify({
          machineId: 6,
          serialNumber: 'DW-SC-700',
          maintenanceType: 'PREVENTIVO_250_HORAS',
          technician: 'patio.taller@smartrent.cl',
          partsReplaced: ['DISCO_CORTE_CARBURO_184MM', 'ESCOBILLAS_CARBON'],
          isAvailable: false
        }, null, 2)
      },
      {
        id: 9805,
        eventType: 'CART_CHECKOUT_INITIATED',
        timestamp: '2026-10-14T11:05:19Z',
        originService: 'ms-smartrent-cart',
        severity: 'INFO',
        eventPayload: JSON.stringify({
          userId: 'usr_corp_02',
          type: 'quote',
          itemsCount: 2,
          totalDepositRequired: 1200000
        }, null, 2)
      }
    ];
  }
}
