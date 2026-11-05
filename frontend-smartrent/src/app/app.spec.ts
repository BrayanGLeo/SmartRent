import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { MsalService, MsalBroadcastService, MSAL_GUARD_CONFIG } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { App } from './app';
import { AuthService } from './core/services/auth/auth.service';

describe('App', () => {
  const mockMsalService = {
    handleRedirectObservable: () => of(null),
    instance: {
      getAllAccounts: () => []
    },
    loginRedirect: () => {},
    logoutRedirect: () => {}
  };

  const mockBroadcastService = {
    inProgress$: of(InteractionStatus.None)
  };

  const mockAuthService = {
    account: signal(null),
    userRole: signal('None'),
    isAuthBusy: signal(false),
    login: () => {},
    logout: () => {}
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: MsalService, useValue: mockMsalService },
        { provide: MsalBroadcastService, useValue: mockBroadcastService },
        { provide: MSAL_GUARD_CONFIG, useValue: { interactionType: 'redirect' } },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render brand name in navbar', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand-name')?.textContent).toContain('SMARTRENT');
  });
});
