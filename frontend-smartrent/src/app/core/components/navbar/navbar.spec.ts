import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { NavbarComponent } from './navbar';
import { AuthService, UserRole } from '../../services/auth/auth.service';
import { CartService } from '../../services/cart/cart.service';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;

  const accountSig = signal<any>(null);
  const roleSig = signal<UserRole>('None');
  const busySig = signal(false);

  const mockAuthService = {
    account: accountSig,
    userRole: roleSig,
    isAuthBusy: busySig,
    login: vi.fn(),
    logout: vi.fn()
  };

  const mockCartService = {
    totalItemsCount: signal(3)
  };

  beforeEach(async () => {
    accountSig.set(null);
    roleSig.set('None');
    busySig.set(false);

    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: CartService, useValue: mockCartService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render navigation links across all user roles', () => {
    expect(component).toBeTruthy();
    expect(component.cartCount).toBe(3);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Iniciar Sesión');

    accountSig.set({ name: 'Arrendatario', username: 'arr@test.cl' });
    roleSig.set('Arrendatario');
    fixture.detectChanges();
    expect(el.textContent).toContain('Mis Arriendos');
    expect(el.textContent).toContain('Cotización');

    roleSig.set('JefeBodega');
    fixture.detectChanges();
    expect(el.textContent).toContain('Control de Patio');

    roleSig.set('Auditor');
    fixture.detectChanges();
    expect(el.textContent).toContain('Auditoría');

    roleSig.set('Administrador');
    fixture.detectChanges();
    expect(el.textContent).toContain('Panel Admin');
    expect(el.textContent).toContain('Reportes');

    busySig.set(true);
    fixture.detectChanges();
    expect(el.querySelector('.nav-spinner')).toBeTruthy();
  });
});
