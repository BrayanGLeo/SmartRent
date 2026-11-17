import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { Catalog } from './catalog';
import { AuthService, UserRole } from '../../core/services/auth/auth.service';
import { ApiService, Machine } from '../../core/services/api/api.service';
import { CartService } from '../../core/services/cart/cart.service';
import { AccessDeniedService } from '../../core/services/modal/access-denied.service';

describe('Catalog', () => {
  let component: Catalog;
  let fixture: ComponentFixture<Catalog>;
  let router: Router;
  let queryParams$: BehaviorSubject<Record<string, string>>;

  const userRoleSig = signal<UserRole>('Arrendatario');
  const mockAuthService = {
    account: signal(null),
    userRole: userRoleSig,
    isAuthBusy: signal(false)
  };

  const sampleMachines: Machine[] = [
    { id: 1, name: 'Excavadora Cat 320', serialNumber: 'CAT-320', sku: 'EQ-CAT-320', category: 'Maquinaria Pesada', dailyPrice: 150000, isAvailable: true, status: 'DISPONIBLE', availableStock: 2, brand: 'Caterpillar' },
    { id: 2, name: 'Taladro Makita', serialNumber: 'MAK-1', sku: 'EQ-MAK', category: { id: 2, name: 'Herramientas Eléctricas', description: 'Herramientas' }, dailyPrice: 20000, isAvailable: false, status: 'SIN_STOCK', availableStock: 0, brand: 'Makita' },
    { id: 3, name: 'Bomag BW 211', serialNumber: 'BMG-211', category: 'Compactación', dailyPrice: 85000, isAvailable: true, status: 'DISPONIBLE', availableStock: 1 }
  ];

  const mockApiService = {
    getCatalog: vi.fn(),
    getMockCatalog: vi.fn(() => sampleMachines)
  };

  const mockCartService = {
    addItem: vi.fn()
  };

  const mockAccessDenied = {
    show: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    userRoleSig.set('Arrendatario');
    queryParams$ = new BehaviorSubject<Record<string, string>>({});
    mockApiService.getCatalog.mockReturnValue(of(sampleMachines));

    await TestBed.configureTestingModule({
      imports: [Catalog],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: ActivatedRoute, useValue: { queryParams: queryParams$.asObservable() } },
        { provide: AuthService, useValue: mockAuthService },
        { provide: ApiService, useValue: mockApiService },
        { provide: CartService, useValue: mockCartService },
        { provide: AccessDeniedService, useValue: mockAccessDenied }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(Catalog);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should filter, sort, compute rental days and resolve images', () => {
    expect(component.machines.length).toBe(3);

    component.rentalStartDate = '2026-10-01';
    component.rentalEndDate = '2026-10-06';
    expect(component.rentalDays).toBe(5);
    component.rentalStartDate = '';
    expect(component.rentalDays).toBe(1);

    component.selectedCategory = 'Maquinaria Pesada';
    expect(component.filteredMachines.length).toBe(1);

    component.selectedCategory = 'Todas';
    component.onlyAvailable = true;
    expect(component.filteredMachines.length).toBe(2);

    component.searchTerm = 'makita';
    component.onlyAvailable = false;
    expect(component.filteredMachines.length).toBe(1);

    component.searchTerm = '';
    component.sortBy = 'price_asc';
    expect(component.filteredMachines[0].dailyPrice).toBe(20000);

    component.sortBy = 'price_desc';
    expect(component.filteredMachines[0].dailyPrice).toBe(150000);

    component.sortBy = 'name';
    expect(component.filteredMachines[0].name).toBe('Bomag BW 211');

    expect(component.isAdmin).toBe(false);
    expect(component.isHeavyMachinery('Maquinaria Pesada')).toBe(true);
    expect(component.isHeavyMachinery({ name: 'Herramientas' })).toBe(false);
    expect(component.getImageUrl(sampleMachines[0])).toBe('/images/cat-320.jpg');
    expect(component.getImageUrl({ id: 9, name: 'Otro', category: 'General', status: 'DISPONIBLE', dailyPrice: 1000, isAvailable: true, imageUrl: '/img.png' })).toBe('/img.png');
    expect(component.getImageUrl({ id: 10, name: 'Desconocido', category: 'General', status: 'DISPONIBLE', dailyPrice: 1000, isAvailable: true })).toBe('/images/cat-320.jpg');
  });

  it('should add items to cart or show out-of-stock toast and handle API fallback', () => {
    vi.useFakeTimers();

    // Available machine -> quote and express
    component.requestQuote(sampleMachines[0]);
    expect(mockCartService.addItem).toHaveBeenCalled();
    expect(component.showNotification).toBe(true);

    component.rentDirect(sampleMachines[0]);
    expect(mockCartService.addItem).toHaveBeenCalledTimes(2);

    // Out-of-stock machine
    component.requestQuote(sampleMachines[1]);
    component.rentDirect(sampleMachines[1]);
    expect(mockCartService.addItem).toHaveBeenCalledTimes(2);

    vi.advanceTimersByTime(4500);
    expect(component.showNotification).toBe(false);

    component.showNotification = true;
    component.dismissToast();
    expect(component.showNotification).toBe(false);

    // API empty & error fallback
    mockApiService.getCatalog.mockReturnValue(of([]));
    component.loadCatalog();
    expect(component.machines.length).toBe(3);

    mockApiService.getCatalog.mockReturnValue(throwError(() => new Error('Network')));
    component.loadCatalog();
    expect(mockApiService.getMockCatalog).toHaveBeenCalled();
  });

  it('should block internal roles and apply queryParams', () => {
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    queryParams$.next({ category: 'Compactación', startDate: '2026-11-01', endDate: '2026-11-05' });
    component.ngOnInit();
    expect(component.selectedCategory).toBe('Compactación');

    userRoleSig.set('Administrador');
    component.ngOnInit();
    expect(mockAccessDenied.show).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/']);
  });
});
