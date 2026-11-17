import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { Cart } from './cart';
import { AuthService } from '../../core/services/auth/auth.service';
import { CartService } from '../../core/services/cart/cart.service';
import { CartItem } from '../../core/models';

describe('Cart', () => {
  let component: Cart;
  let fixture: ComponentFixture<Cart>;
  let router: Router;

  const accountSig = signal<any>(null);
  const mockAuthService = {
    account: accountSig,
    userRole: signal('Arrendatario'),
    isAuthBusy: signal(false),
    login: vi.fn(),
    logout: vi.fn()
  };

  const mockCartService = {
    expressItems: signal<CartItem[]>([
      { machineId: 1, machineName: 'Excavadora Cat 320', type: 'express', dailyPrice: 150000, days: 2 }
    ]),
    quoteItems: signal<CartItem[]>([
      { machineId: 2, machineName: 'Grúa JLG 450', type: 'quote', dailyPrice: 95000, days: 5 }
    ]),
    totalItemsCount: signal(2),
    summary: signal({
      expressCount: 1,
      quoteCount: 1,
      totalItems: 2,
      estimatedNetClp: 775000,
      totalDepositClp: 500000,
      estimatedIvaClp: 147250,
      estimatedTotalClp: 922250
    }),
    updateRentalDays: vi.fn(),
    removeItem: vi.fn(),
    clear: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    accountSig.set(null);

    await TestBed.configureTestingModule({
      imports: [Cart],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
        { provide: CartService, useValue: mockCartService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(Cart);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should expose cart getters, render cart items and empty state, and delegate actions', () => {
    expect(component.expressItems.length).toBe(1);
    expect(component.quoteItems.length).toBe(1);
    expect(component.totalItemsCount).toBe(2);
    expect(component.summary.totalItems).toBe(2);

    // Render empty state too
    mockCartService.totalItemsCount.set(0);
    mockCartService.expressItems.set([]);
    mockCartService.quoteItems.set([]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ir al Catálogo');

    // Restore populated state
    mockCartService.totalItemsCount.set(2);
    mockCartService.expressItems.set([{ machineId: 1, machineName: 'Excavadora Cat 320', type: 'express', dailyPrice: 150000, days: 2 }]);
    mockCartService.quoteItems.set([{ machineId: 2, machineName: 'Grúa JLG 450', type: 'quote', dailyPrice: 95000, days: 5 }]);
    fixture.detectChanges();

    component.updateDays(1, 2, 2);
    expect(mockCartService.updateRentalDays).toHaveBeenCalledWith(1, 4);

    component.updateDays(1, -5, 1);
    expect(mockCartService.updateRentalDays).toHaveBeenCalledWith(1, 1);

    component.removeItem(1);
    expect(mockCartService.removeItem).toHaveBeenCalledWith(1);
  });

  it('should resolve images for all brands and categories', () => {
    expect(component.getImageUrl({ machineId: 1, machineName: 'X', type: 'express', dailyPrice: 1, machine: { id: 1, name: 'X', dailyPrice: 1, isAvailable: true, imageUrl: '/custom.jpg' } as any })).toBe('/custom.jpg');
    expect(component.getImageUrl({ machineId: 1, machineName: 'Cat 320', type: 'express', dailyPrice: 1 })).toBe('/images/cat-320.jpg');
    expect(component.getImageUrl({ machineId: 2, machineName: 'John Deere 310L', type: 'express', dailyPrice: 1 })).toBe('/images/jd-310l.jpg');
    expect(component.getImageUrl({ machineId: 3, machineName: 'Bomag BW 211', type: 'express', dailyPrice: 1 })).toBe('/images/bomag-bw211.jpg');
    expect(component.getImageUrl({ machineId: 4, machineName: 'Makita', type: 'express', dailyPrice: 1 })).toBe('/images/makita-18v.jpg');
    expect(component.getImageUrl({ machineId: 5, machineName: 'Bosch GSH', type: 'express', dailyPrice: 1 })).toBe('/images/bosch-gsh11e.jpg');
    expect(component.getImageUrl({ machineId: 6, machineName: 'DeWalt Circular', type: 'express', dailyPrice: 1 })).toBe('/images/dewalt-dwe575.jpg');
    expect(component.getImageUrl({ machineId: 7, machineName: 'JLG 450', type: 'express', dailyPrice: 1 })).toBe('/images/jlg-450aj.jpg');
    expect(component.getImageUrl({ machineId: 8, machineName: 'Montacargas Yale', type: 'express', dailyPrice: 1 })).toBe('/images/yale-3t.jpg');
    expect(component.getImageUrl({ machineId: 9, machineName: 'Cummins 50', type: 'express', dailyPrice: 1 })).toBe('/images/cummins-50kva.jpg');
    expect(component.getImageUrl({ machineId: 10, machineName: 'Inverter EU22', type: 'express', dailyPrice: 1 })).toBe('/images/honda-eu2200i.jpg');
    expect(component.getImageUrl({ machineId: 11, machineName: 'Sullair 185', type: 'express', dailyPrice: 1 })).toBe('/images/sullair-185.jpg');
    expect(component.getImageUrl({ machineId: 12, machineName: 'Motosierra Husqvarna', type: 'express', dailyPrice: 1 })).toBe('/images/husqvarna-450.jpg');
    expect(component.getImageUrl({ machineId: 13, machineName: 'Cortadora Césped HRN', type: 'express', dailyPrice: 1 })).toBe('/images/honda-hrn216.jpg');
    expect(component.getImageUrl({ machineId: 14, machineName: 'Gen', type: 'express', dailyPrice: 1, machine: { id: 14, name: 'Gen', dailyPrice: 1, isAvailable: true, category: 'Elevación' } as any })).toBe('/images/jlg-450aj.jpg');
    expect(component.getImageUrl({ machineId: 15, machineName: 'Gen', type: 'express', dailyPrice: 1, machine: { id: 15, name: 'Gen', dailyPrice: 1, isAvailable: true, category: { id: 1, name: 'Generador' } } as any })).toBe('/images/cummins-50kva.jpg');
    expect(component.getImageUrl({ machineId: 16, machineName: 'Gen', type: 'express', dailyPrice: 1, machine: { id: 16, name: 'Gen', dailyPrice: 1, isAvailable: true, category: 'Herramienta' } as any })).toBe('/images/makita-18v.jpg');
    expect(component.getImageUrl({ machineId: 17, machineName: 'Gen', type: 'express', dailyPrice: 1, machine: { id: 17, name: 'Gen', dailyPrice: 1, isAvailable: true, category: 'Jardín' } as any })).toBe('/images/husqvarna-450.jpg');
    expect(component.getImageUrl({ machineId: 18, machineName: 'Otra', type: 'express', dailyPrice: 1 })).toBe('/images/cat-320.jpg');
  });

  it('should handle checkout login gate and clearCart confirmation', () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.proceedToCheckout('express');
    expect(alertSpy).toHaveBeenCalled();
    expect(mockAuthService.login).toHaveBeenCalled();

    accountSig.set({ username: 'client@smartrent.cl', name: 'Cliente' });
    component.proceedToCheckout('quote');
    expect(navSpy).toHaveBeenCalledWith(['/checkout'], { queryParams: { type: 'quote' } });

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.clearCart();
    expect(mockCartService.clear).toHaveBeenCalled();
  });
});
