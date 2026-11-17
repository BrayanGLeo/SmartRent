import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { Home } from './home';
import { AuthService, UserRole } from '../core/services/auth/auth.service';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let router: Router;

  const userRoleSig = signal<UserRole>('None');
  const mockAuthService = {
    account: signal(null),
    userRole: userRoleSig,
    isAuthBusy: signal(false)
  };

  beforeEach(async () => {
    userRoleSig.set('None');
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should route onQuickSearch and filterByCategory based on user role', () => {
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    // Public / Arrendatario -> /catalog
    component.searchForm.category = 'Maquinaria Pesada';
    component.onQuickSearch();
    expect(navSpy).toHaveBeenCalledWith(['/catalog'], expect.objectContaining({ queryParams: expect.objectContaining({ category: 'Maquinaria Pesada' }) }));

    component.filterByCategory('Equipos de Elevación');
    expect(navSpy).toHaveBeenCalledWith(['/catalog'], { queryParams: { category: 'Equipos de Elevación' } });

    // Administrador -> /admin
    userRoleSig.set('Administrador');
    component.onQuickSearch();
    component.filterByCategory('Test');
    expect(navSpy).toHaveBeenCalledWith(['/admin']);

    // JefeBodega -> /rentals
    userRoleSig.set('JefeBodega');
    component.onQuickSearch();
    component.filterByCategory('Test');
    expect(navSpy).toHaveBeenCalledWith(['/rentals']);

    // Auditor -> /audit
    userRoleSig.set('Auditor');
    component.onQuickSearch();
    component.filterByCategory('Test');
    expect(navSpy).toHaveBeenCalledWith(['/audit']);
  });
});
