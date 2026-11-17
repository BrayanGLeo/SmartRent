import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { Audit } from './audit';
import { ApiService } from '../../core/services/api/api.service';

describe('Audit', () => {
  let component: Audit;
  let fixture: ComponentFixture<Audit>;

  const mockApiService = {
    getAuditTimeline: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    mockApiService.getAuditTimeline.mockReturnValue(
      of([
        { id: 1, eventType: 'ORDER_CREATED', eventPayload: '{"orderId":10}', timestamp: '2026-10-15T10:00:00Z' },
        { id: 2, eventType: 'RENTAL_STATUS_CHANGED', eventPayload: 'not-json', timestamp: '2026-10-15T11:00:00Z' },
        { id: 3, eventType: 'INVENTORY_MAINTENANCE', eventPayload: '{"machineId":5}', timestamp: '2026-10-15T12:00:00Z' },
        { id: 4, eventType: 'CART_CANCELLED', eventPayload: '{"cart":1}', timestamp: '2026-10-15T13:00:00Z' },
        { id: 5, eventType: 'SYSTEM_OTHER', eventPayload: '{}', timestamp: '2026-10-15T14:00:00Z' }
      ])
    );

    await TestBed.configureTestingModule({
      imports: [Audit],
      providers: [
        provideHttpClient(),
        { provide: ApiService, useValue: mockApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Audit);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should load, parse, filter and expand events, and fallback on empty/error', () => {
    expect(component.events.length).toBe(5);
    expect(component.events[0].originService).toBe('ms-smartrent-checkout');
    expect(component.events[0].severity).toBe('AUDIT');
    expect(component.events[1].originService).toBe('ms-smartrent-rentals');
    expect(component.events[2].originService).toBe('ms-smartrent-catalog');
    expect(component.events[2].severity).toBe('WARN');
    expect(component.events[3].originService).toBe('ms-smartrent-cart');
    expect(component.events[3].severity).toBe('ERROR');
    expect(component.events[4].originService).toBe('ms-smartrent-bff');

    component.selectedFilter = 'ORDER_CREATED';
    expect(component.filteredEvents.length).toBe(1);

    component.selectedFilter = 'TODOS';
    component.searchTerm = 'rentals';
    expect(component.filteredEvents.length).toBe(1);

    component.toggleExpand(1);
    fixture.detectChanges();
    expect(component.expandedEventId).toBe(1);
    expect(component.getExpandedPayload()).toContain('orderId');

    component.viewMode = 'table';
    fixture.detectChanges();
    component.viewMode = 'timeline';

    component.toggleExpand(1);
    fixture.detectChanges();
    expect(component.expandedEventId).toBeNull();
    expect(component.getExpandedPayload()).toBe('');

    mockApiService.getAuditTimeline.mockReturnValue(of([]));
    component.loadTimeline();
    expect(component.events.length).toBe(5);

    mockApiService.getAuditTimeline.mockReturnValue(throwError(() => new Error('Audit API down')));
    component.loadTimeline();
    expect(component.events.length).toBe(5);
  });
});
