import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { Rentals } from './rentals';
import { AuthService } from '../../core/services/auth/auth.service';

describe('Rentals', () => {
  let component: Rentals;
  let fixture: ComponentFixture<Rentals>;

  const mockAuthService = {
    account: signal(null),
    userRole: signal('JefeBodega'),
    isAuthBusy: signal(false)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Rentals],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Rentals);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
