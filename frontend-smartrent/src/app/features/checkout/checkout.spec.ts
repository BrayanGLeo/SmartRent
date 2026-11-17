import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { CheckoutComponent } from './checkout';
import { ApiService } from '../../core/services/api/api.service';
import { CartService } from '../../core/services/cart/cart.service';
import { AuthService } from '../../core/services/auth/auth.service';
import { CartItem } from '../../core/models';

describe('CheckoutComponent', () => {
  let component: CheckoutComponent;
  let fixture: ComponentFixture<CheckoutComponent>;
  let router: Router;
  let queryParams$: BehaviorSubject<Record<string, string>>;

  const itemsSignal = signal<CartItem[]>([
    { machineId: 1, machineName: 'Excavadora Cat 320', type: 'express', dailyPrice: 100000, days: 3, warrantyDeposit: 150000 },
    { machineId: 2, machineName: 'Grúa JLG 450', type: 'quote', dailyPrice: 80000, days: 2 }
  ]);

  const mockCartService = {
    items: itemsSignal,
    removeItem: vi.fn()
  };

  const mockApiService = {
    processCheckout: vi.fn()
  };

  const mockAuthService = {
    account: signal({ name: 'Juan Pérez', username: 'juan@empresa.cl' }),
    userRole: signal('Arrendatario')
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    queryParams$ = new BehaviorSubject<Record<string, string>>({ type: 'express' });
    itemsSignal.set([
      { machineId: 1, machineName: 'Excavadora Cat 320', type: 'express', dailyPrice: 100000, days: 3, warrantyDeposit: 150000 },
      { machineId: 2, machineName: 'Grúa JLG 450', type: 'quote', dailyPrice: 80000, days: 2 }
    ]);

    await TestBed.configureTestingModule({
      imports: [CheckoutComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParams: queryParams$.asObservable() } },
        { provide: ApiService, useValue: mockApiService },
        { provide: CartService, useValue: mockCartService },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(CheckoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should compute totals and handle logistics toggle', () => {
    expect(component.checkoutType).toBe('EXPRESS');
    expect(component.cartItems.length).toBe(1);
    expect(component.subtotalNeto).toBe(300000);
    expect(component.costoLogistico).toBe(45000);
    expect(component.totalGarantia).toBe(150000);
    expect(component.totalIva).toBe(Math.round((300000 + 45000) * 0.19));
    expect(component.totalGeneral).toBe(300000 + 45000 + component.totalIva);

    component.logisticsForm.get('deliveryType')?.setValue('PICKUP');
    expect(component.costoLogistico).toBe(0);
    component.logisticsForm.get('deliveryType')?.setValue('DISPATCH');
    expect(component.costoLogistico).toBe(45000);
  });

  it('should validate steps and submit EXPRESS order successfully and with offline fallback', () => {
    component.nextStep();
    expect(component.step).toBe(1); // invalid customerForm

    component.customerForm.patchValue({
      customerType: 'COMPANY',
      rut: '12345678-9',
      name: 'Juan Pérez',
      phone: '+56912345678',
      email: 'juan@empresa.cl',
      workSiteName: 'Faena Norte'
    });
    component.nextStep();
    expect(component.step).toBe(2);
    fixture.detectChanges();

    // Invalid logisticsForm prevents submit
    component.submitOrder();
    expect(mockApiService.processCheckout).not.toHaveBeenCalled();

    component.logisticsForm.patchValue({
      deliveryType: 'DISPATCH',
      deliveryAddress: 'Av. Industrial 100',
      paymentMethod: 'TRANSFER'
    });
    mockApiService.processCheckout.mockReturnValue(of({ orderId: 777 }));

    component.submitOrder();
    fixture.detectChanges();
    expect(component.orderCompleted).toBe(true);
    expect(component.createdOrderId).toBe(777);
    expect(mockCartService.removeItem).toHaveBeenCalledWith(1);

    component.orderCompleted = false;
    component.prevStep();
    fixture.detectChanges();
    expect(component.step).toBe(1);

    // Offline fallback on error
    mockApiService.processCheckout.mockReturnValue(throwError(() => new Error('Network')));
    component.submitOrder();
    expect(component.orderCompleted).toBe(true);
    expect(component.createdOrderId).toBeTruthy();
  });

  it('should submit QUOTE directly from step 1 and redirect when cart is empty', () => {
    mockApiService.processCheckout.mockReturnValue(of({ orderId: 888 }));
    queryParams$.next({ type: 'quote' });
    expect(component.checkoutType).toBe('QUOTE');
    expect(component.cartItems.length).toBe(1);

    component.customerForm.patchValue({
      customerType: 'PERSON',
      rut: '11111111-1',
      name: 'Ana Gómez',
      phone: '+56987654321',
      email: 'ana@smartrent.cl',
      workSiteName: 'Proyecto Sur'
    });
    component.nextStep();
    expect(component.createdOrderId).toBe(888);

    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    itemsSignal.set([]);
    component.loadCart();
    expect(alertSpy).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/cart']);
  });

  it('should resolve images across brands and categories', () => {
    expect(component.getImageUrl({ machineId: 1, machineName: 'Cat 320', type: 'express', dailyPrice: 1 })).toBe('/images/cat-320.jpg');
    expect(component.getImageUrl({ machineId: 2, machineName: 'John Deere 310', type: 'express', dailyPrice: 1 })).toBe('/images/jd-310l.jpg');
    expect(component.getImageUrl({ machineId: 3, machineName: 'Bomag 211', type: 'express', dailyPrice: 1 })).toBe('/images/bomag-bw211.jpg');
    expect(component.getImageUrl({ machineId: 4, machineName: 'Makita', type: 'express', dailyPrice: 1 })).toBe('/images/makita-18v.jpg');
    expect(component.getImageUrl({ machineId: 5, machineName: 'Bosch GSH', type: 'express', dailyPrice: 1 })).toBe('/images/bosch-gsh11e.jpg');
    expect(component.getImageUrl({ machineId: 6, machineName: 'DeWalt Circular', type: 'express', dailyPrice: 1 })).toBe('/images/dewalt-dwe575.jpg');
    expect(component.getImageUrl({ machineId: 7, machineName: 'JLG 450', type: 'express', dailyPrice: 1 })).toBe('/images/jlg-450aj.jpg');
    expect(component.getImageUrl({ machineId: 8, machineName: 'Montacargas Yale', type: 'express', dailyPrice: 1 })).toBe('/images/yale-3t.jpg');
    expect(component.getImageUrl({ machineId: 9, machineName: 'Cummins', type: 'express', dailyPrice: 1 })).toBe('/images/cummins-50kva.jpg');
    expect(component.getImageUrl({ machineId: 10, machineName: 'EU22 Inverter', type: 'express', dailyPrice: 1 })).toBe('/images/honda-eu2200i.jpg');
    expect(component.getImageUrl({ machineId: 11, machineName: 'Sullair 185', type: 'express', dailyPrice: 1 })).toBe('/images/sullair-185.jpg');
    expect(component.getImageUrl({ machineId: 12, machineName: 'Motosierra Husqvarna', type: 'express', dailyPrice: 1 })).toBe('/images/husqvarna-450.jpg');
    expect(component.getImageUrl({ machineId: 13, machineName: 'Césped HRN', type: 'express', dailyPrice: 1 })).toBe('/images/honda-hrn216.jpg');
    expect(component.getImageUrl({ machineId: 14, machineName: 'Gen', type: 'express', dailyPrice: 1, machine: { id: 14, name: 'Gen', dailyPrice: 1, isAvailable: true, category: 'Elevación' } as any })).toBe('/images/jlg-450aj.jpg');
  });
});
