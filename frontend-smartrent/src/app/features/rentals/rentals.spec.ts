import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { Rentals } from './rentals';
import { AuthService, UserRole } from '../../core/services/auth/auth.service';
import { ApiService, Machine, Rental } from '../../core/services/api/api.service';

describe('Rentals', () => {
  let component: Rentals;
  let fixture: ComponentFixture<Rentals>;

  const userRoleSig = signal<UserRole>('JefeBodega');
  const mockAuthService = {
    account: signal({ localAccountId: 'oid-1', name: 'Operador Bodega' }),
    userRole: userRoleSig,
    isAuthBusy: signal(false)
  };

  const sampleMachines: Machine[] = [
    { id: 1, name: 'Excavadora Cat 320', category: 'Maquinaria Pesada', status: 'DISPONIBLE', dailyPrice: 150000, isAvailable: true, availableStock: 2 },
    { id: 2, name: 'Taladro Sin Stock', category: 'Herramientas', status: 'SIN_STOCK', dailyPrice: 20000, isAvailable: false, availableStock: 0 }
  ];

  const sampleRentals: Rental[] = [
    { id: 101, machineId: 1, userId: 'oid-1', startDate: '2026-10-10', endDate: '2026-10-15', status: 'SOLICITADO' }
  ];

  const mockApiService = {
    getRentals: vi.fn(),
    getAllCatalog: vi.fn(),
    getCatalog: vi.fn(),
    getMockCatalog: vi.fn(() => sampleMachines),
    requestRental: vi.fn(),
    updateRentalStatus: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    userRoleSig.set('JefeBodega');
    mockApiService.getRentals.mockReturnValue(of(sampleRentals));
    mockApiService.getAllCatalog.mockReturnValue(of(sampleMachines));
    mockApiService.getCatalog.mockReturnValue(of(sampleMachines));

    await TestBed.configureTestingModule({
      imports: [Rentals],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ApiService, useValue: mockApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Rentals);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should load rentals and machines, filter, and check role getters', () => {
    expect(component.rentals.length).toBe(1);
    expect(component.rentals[0].machineName).toBe('Excavadora Cat 320');
    expect(component.isJefeBodega).toBe(true);
    expect(component.isArrendatario).toBe(false);

    userRoleSig.set('Arrendatario');
    component.loadMachines();
    expect(mockApiService.getCatalog).toHaveBeenCalled();

    component.selectedFilter = 'SOLICITADO';
    expect(component.filteredRentals.length).toBe(1);
    component.searchTerm = 'excavadora';
    expect(component.filteredRentals.length).toBe(1);
    component.searchTerm = '101';
    expect(component.filteredRentals.length).toBe(1);
    component.searchTerm = 'Faena Central';
    expect(component.filteredRentals.length).toBe(1);
    component.selectedFilter = 'APROBADO';
    expect(component.filteredRentals.length).toBe(0);

    expect(component.getStatusConfig('DESCONOCIDO').label).toBe('DESCONOCIDO');

    // Empty arrays fallback
    mockApiService.getRentals.mockReturnValue(of([]));
    mockApiService.getCatalog.mockReturnValue(of([]));
    component.loadRentals();
    component.loadMachines();
    expect(component.rentals.length).toBeGreaterThan(1);
    expect(component.machines.length).toBe(2);
  });

  it('should handle API errors with mocks and submit rental / status updates', () => {
    vi.useFakeTimers();
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

    // Empty form validation
    component.newRental.machineId = '';
    component.requestRental();
    expect(alertSpy).toHaveBeenCalled();

    // Unavailable machine validation
    component.newRental.machineId = '2';
    component.requestRental();
    expect(alertSpy).toHaveBeenCalledTimes(2);

    // Valid request -> success & error
    component.showNewModal = true;
    fixture.detectChanges();
    component.newRental.machineId = '1';
    mockApiService.requestRental.mockReturnValue(of({ id: 555 }));
    component.requestRental();
    fixture.detectChanges();
    expect(component.rentals[0].id).toBe(555);
    expect(component.showToast).toBe(true);

    mockApiService.requestRental.mockReturnValue(throwError(() => new Error('fail')));
    component.requestRental();
    expect(component.showToast).toBe(true);

    // Update status -> success & error
    mockApiService.updateRentalStatus.mockReturnValue(of({}));
    component.updateStatus(555, { target: { value: 'APROBADO' } } as unknown as Event);
    expect(component.rentals.find(r => r.id === 555)?.status).toBe('APROBADO');

    mockApiService.updateRentalStatus.mockReturnValue(throwError(() => new Error('offline')));
    component.updateStatus(555, { target: { value: 'EN_TERRENO' } } as unknown as Event);
    expect(component.rentals.find(r => r.id === 555)?.status).toBe('EN_TERRENO');

    vi.advanceTimersByTime(4000);
    expect(component.showToast).toBe(false);

    // Error fallbacks in loadRentals and loadMachines
    mockApiService.getRentals.mockReturnValue(throwError(() => new Error('down')));
    mockApiService.getAllCatalog.mockReturnValue(throwError(() => new Error('down')));
    userRoleSig.set('Administrador');
    component.loadRentals();
    component.loadMachines();
    expect(component.rentals.length).toBeGreaterThan(1);
  });
});
