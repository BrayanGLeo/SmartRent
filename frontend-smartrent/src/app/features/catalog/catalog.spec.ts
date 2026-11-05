import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { Catalog } from './catalog';
import { AuthService } from '../../core/services/auth/auth.service';

describe('Catalog', () => {
  let component: Catalog;
  let fixture: ComponentFixture<Catalog>;

  const mockAuthService = {
    account: signal(null),
    userRole: signal('Arrendatario'),
    isAuthBusy: signal(false)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Catalog],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Catalog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
