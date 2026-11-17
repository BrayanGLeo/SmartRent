import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MsalService, MsalBroadcastService, MSAL_GUARD_CONFIG } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { App } from './app';
import { AuthService } from './core/services/auth/auth.service';

describe('App', () => {
  let mockMsalService: any;

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
    mockMsalService = {
      handleRedirectObservable: vi.fn(() => of(null)),
      instance: {
        getAllAccounts: vi.fn(() => [{ username: 'test@smartrent.cl' }])
      },
      loginRedirect: vi.fn(),
      logoutRedirect: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: MsalService, useValue: mockMsalService },
        { provide: MsalBroadcastService, useValue: mockBroadcastService },
        { provide: MSAL_GUARD_CONFIG, useValue: { authRequest: { scopes: ['Arriendos.Leer'] }, interactionType: 'redirect' } },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create the app and render brand name in navbar', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(fixture.componentInstance).toBeTruthy();
    expect(compiled.querySelector('.brand-name')?.textContent).toContain('SMARTRENT');
    expect(fixture.componentInstance.loginDisplay).toBe(true);
  });

  it('should handle login, logout and redirect error cleanup', async () => {
    const replaceSpy = vi.spyOn(window.history, 'replaceState').mockImplementation(() => {});
    const errSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    app.login();
    expect(mockMsalService.loginRedirect).toHaveBeenCalledWith({ scopes: ['Arriendos.Leer'] });

    app.logout();
    expect(mockMsalService.logoutRedirect).toHaveBeenCalled();

    // Cover else branch of login and error branch of handleRedirectObservable
    (app as any).msalGuardConfig = {};
    app.login();
    expect(mockMsalService.loginRedirect).toHaveBeenCalledTimes(2);

    mockMsalService.handleRedirectObservable.mockReturnValue(throwError(() => new Error('state_mismatch')));
    app.ngOnInit();
    expect(errSpy).toHaveBeenCalled();
    expect(replaceSpy).toHaveBeenCalled();

    app.ngOnDestroy();
  });
});
