import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { Dashboard } from './dashboard';
import { AuthService } from '../core/services/auth/auth.service';
import { ApiService } from '../core/services/api/api.service';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  const mockAuthService = {
    account: signal({ name: 'Cliente Test', username: 'c@test.cl' }),
    userRole: signal('Arrendatario'),
    isAuthBusy: signal(false)
  };

  const mockApiService = {
    getRentals: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    mockApiService.getRentals.mockReturnValue(
      of([
        { id: 1, machineId: 1, userId: 'u1', startDate: '2026-10-01', endDate: '2026-10-05', status: 'SOLICITADO' }
      ])
    );

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ApiService, useValue: mockApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should load rentals and fallback to mock data on empty or error', () => {
    expect(component.recentRentals.length).toBe(1);

    mockApiService.getRentals.mockReturnValue(of([]));
    component.ngOnInit();
    expect(component.recentRentals.length).toBe(3);

    mockApiService.getRentals.mockReturnValue(throwError(() => new Error('API down')));
    component.ngOnInit();
    expect(component.recentRentals.length).toBe(3);
  });
});
