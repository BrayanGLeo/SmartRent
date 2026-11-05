import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { Home } from './home';
import { AuthService } from '../core/services/auth/auth.service';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;

  const mockAuthService = {
    account: signal(null),
    userRole: signal('None'),
    isAuthBusy: signal(false)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
