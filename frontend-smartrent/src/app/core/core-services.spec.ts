import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { AccountInfo, InteractionStatus } from '@azure/msal-browser';
import { BehaviorSubject, Observable, isObservable, of } from 'rxjs';
import { LoadingService } from './services/loading/loading.service';
import { AccessDeniedService } from './services/modal/access-denied.service';
import { AccessDeniedModalComponent } from './components/access-denied-modal/access-denied-modal';
import { AuthService, getUserRoleFromAccount } from './services/auth/auth.service';
import { CartService } from './services/cart/cart.service';
import { ApiService } from './services/api/api.service';
import { adminGuard } from './guards/admin.guard';
import { catalogGuard } from './guards/catalog.guard';
import { roleGuard } from './guards/role.guard';
import { loadingInterceptor } from './interceptors/loading.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';
import { NavbarComponent } from './components/navbar/navbar';

function makeAccount(overrides: Partial<AccountInfo> & { idTokenClaims?: Record<string, unknown> } = {}): AccountInfo {
  return {
    homeAccountId: 'home-1',
    environment: 'ciamlogin.com',
    tenantId: 'tenant-1',
    username: 'user@smartrent.cl',
    localAccountId: 'oid-1',
    name: 'Usuario Demo',
    ...overrides
  };
}

describe('Core Services, Guards, Interceptors & Components', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  describe('LoadingService', () => {
    it('should toggle loading state with show and hide', () => {
      TestBed.configureTestingModule({});
      const service = TestBed.inject(LoadingService);
      expect(service.isLoading()).toBe(false);
      service.show();
      expect(service.isLoading()).toBe(true);
      service.hide();
      expect(service.isLoading()).toBe(false);
    });
  });

  describe('AccessDeniedService & AccessDeniedModalComponent', () => {
    it('should open modal, count down and redirect', () => {
      vi.useFakeTimers();
      TestBed.configureTestingModule({
        imports: [AccessDeniedModalComponent],
        providers: [provideRouter([])]
      });
      const router = TestBed.inject(Router);
      const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      const service = TestBed.inject(AccessDeniedService);

      const fixture = TestBed.createComponent(AccessDeniedModalComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      service.show('Sin acceso', '/home', 'Denegado');
      fixture.detectChanges();
      expect(service.isOpen()).toBe(true);
      expect(service.secondsRemaining()).toBe(4);
      expect((fixture.nativeElement as HTMLElement).textContent).toContain('Sin acceso');

      vi.advanceTimersByTime(1000);
      expect(service.secondsRemaining()).toBe(3);

      // Re-show clears previous timer
      service.show('Reintento', '/');
      expect(service.secondsRemaining()).toBe(4);

      vi.advanceTimersByTime(4000);
      expect(service.isOpen()).toBe(false);
      expect(navSpy).toHaveBeenCalledWith(['/']);

      // Test component actions: escape & backdrop
      service.show('Escape test', '/catalog');
      component.onEscape();
      expect(service.isOpen()).toBe(false);

      service.show('Backdrop test', '/');
      const div = document.createElement('div');
      component.onBackdropClick({ target: div, currentTarget: div } as unknown as MouseEvent);
      expect(service.isOpen()).toBe(false);

      service.show('Other target', '/');
      component.onBackdropClick({ target: document.createElement('span'), currentTarget: div } as unknown as MouseEvent);
      expect(service.isOpen()).toBe(true);
      component.onConfirm();
      expect(service.isOpen()).toBe(false);
    });
  });

  describe('AuthService & getUserRoleFromAccount', () => {
    it('should extract explicit roles and fallbacks', () => {
      expect(getUserRoleFromAccount(null)).toBe('None');
      expect(getUserRoleFromAccount(makeAccount({ idTokenClaims: { roles: ['ROLE_ADMIN'] } }))).toBe('Administrador');
      expect(getUserRoleFromAccount(makeAccount({ idTokenClaims: { role: 'Auditor' } }))).toBe('Auditor');
      expect(getUserRoleFromAccount(makeAccount({ idTokenClaims: { extension_Role: 'JefeBodega' } }))).toBe('JefeBodega');
      expect(getUserRoleFromAccount(makeAccount({ idTokenClaims: { groups: ['Arrendatario'] } }))).toBe('Arrendatario');
      expect(getUserRoleFromAccount(makeAccount({ idTokenClaims: { roles: ['UnknownRole'] } }))).toBe('None');

      // Fallbacks without explicit roles claim
      expect(getUserRoleFromAccount(makeAccount({ username: 'admin@test.cl', name: 'User' }))).toBe('Administrador');
      expect(getUserRoleFromAccount(makeAccount({ username: 'u@test.cl', name: 'Juan Auditor' }))).toBe('Auditor');
      expect(getUserRoleFromAccount(makeAccount({ username: 'bodega@test.cl', name: 'Operador' }))).toBe('JefeBodega');
      expect(getUserRoleFromAccount(makeAccount({ username: 'cliente@test.cl', name: 'Cliente' }))).toBe('None');
    });

    it('should track account signals and delegate login/logout', () => {
      const inProgress$ = new BehaviorSubject<InteractionStatus>(InteractionStatus.Startup);
      const account = makeAccount({ idTokenClaims: { roles: ['Administrador'] } });
      const msalMock = {
        instance: { getAllAccounts: vi.fn().mockReturnValue([account]) },
        loginRedirect: vi.fn(),
        logoutRedirect: vi.fn()
      };

      TestBed.configureTestingModule({
        providers: [
          { provide: MsalService, useValue: msalMock },
          { provide: MsalBroadcastService, useValue: { inProgress$ } }
        ]
      });

      const authService = TestBed.inject(AuthService);
      expect(authService.isAuthBusy()).toBe(true);
      inProgress$.next(InteractionStatus.None);
      expect(authService.isAuthBusy()).toBe(false);
      expect(authService.account()).toEqual(account);
      expect(authService.userRole()).toBe('Administrador');

      authService.login();
      authService.logout();
      expect(msalMock.loginRedirect).toHaveBeenCalled();
      expect(msalMock.logoutRedirect).toHaveBeenCalled();
    });
  });

  describe('CartService & NavbarComponent', () => {
    it('should load legacy and invalid storage, manage items and calculate summary', () => {
      localStorage.setItem(
        'smartrent_cart',
        JSON.stringify([{ machine: { id: 10, name: 'Excavadora', dailyPrice: 100000 }, type: 'express', days: 2 }])
      );

      TestBed.configureTestingModule({
        imports: [NavbarComponent],
        providers: [
          provideRouter([]),
          provideHttpClient(),
          {
            provide: AuthService,
            useValue: { account: vi.fn(() => null), userRole: vi.fn(() => 'None'), isAuthBusy: vi.fn(() => false), login: vi.fn(), logout: vi.fn() }
          }
        ]
      });

      const cartService = TestBed.inject(CartService);
      expect(cartService.totalItemsCount()).toBe(1);
      expect(cartService.expressItems().length).toBe(1);

      const navFixture = TestBed.createComponent(NavbarComponent);
      expect(navFixture.componentInstance.cartCount).toBe(1);

      cartService.addItem({ machineId: 10, machineName: 'Excavadora Updated', type: 'express', dailyPrice: 120000, days: 3 });
      cartService.addItem({ machineId: 20, machineName: 'Grúa', type: 'quote', dailyPrice: 200000, days: 2 });
      expect(cartService.quoteItems().length).toBe(1);
      expect(cartService.summary().estimatedNetClp).toBe(120000 * 3 + 200000 * 2);

      cartService.updateRentalDays(20, 5, '2026-10-01', '2026-10-06');
      expect(cartService.quoteItems()[0].days).toBe(5);
      cartService.updateRentalDays(20, 4);
      expect(cartService.quoteItems()[0].days).toBe(4);

      const setItemSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementationOnce(() => {
        throw new Error('QuotaExceeded');
      });
      cartService.removeItem(10);
      setItemSpy.mockRestore();
      expect(cartService.totalItemsCount()).toBe(1);

      cartService.clear();
      expect(cartService.totalItemsCount()).toBe(0);
    });

    it('should recover cleanly from corrupted, non-array or minimal legacy localStorage cart data', () => {
      localStorage.setItem('smartrent_cart', 'invalid-json');
      TestBed.configureTestingModule({
        providers: [provideHttpClient()]
      });
      const cartService = TestBed.inject(CartService);
      expect(cartService.items()).toEqual([]);

      TestBed.resetTestingModule();
      localStorage.setItem('smartrent_cart', '{"not":"an-array"}');
      TestBed.configureTestingModule({ providers: [provideHttpClient()] });
      expect(TestBed.inject(CartService).items()).toEqual([]);

      TestBed.resetTestingModule();
      localStorage.setItem('smartrent_cart', JSON.stringify([{ machine: { id: 9, name: 'M', pricePerDay: 2000 } }]));
      TestBed.configureTestingModule({ providers: [provideHttpClient()] });
      const legacyCart = TestBed.inject(CartService);
      expect(legacyCart.items()[0].type).toBe('quote');
      expect(legacyCart.items()[0].days).toBe(3);
      expect(legacyCart.items()[0].dailyPrice).toBe(2000);
    });
  });

  describe('ApiService', () => {
    let apiService: ApiService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting()]
      });
      apiService = TestBed.inject(ApiService);
      httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      httpMock.verify();
    });

    it('should build mock catalog with and without localStorage inventory overrides', () => {
      localStorage.setItem(
        'smartrent_catalog_inventory',
        JSON.stringify([
          { sku: 'EQ-CAT-320', availableStock: 0, totalStock: 5, status: 'EN_MANTENCION', dailyRate: 175000 },
          { id: 2, name: 'Retroexcavadora John Deere 310L', availableStock: 4, totalStock: 6, status: 'DISPONIBLE' }
        ])
      );
      const list = apiService.getMockCatalog();
      expect(list.length).toBeGreaterThan(5);
      expect(list[0].status).toBe('EN_MANTENCION');
      expect(list[0].dailyPrice).toBe(175000);
      expect(list[1].availableStock).toBe(4);
    });

    it('should fetch catalog with serial/name/unknown variants and fallback on error', () => {
      let result: any[] = [];
      apiService.getCatalog().subscribe(res => (result = res));
      const req = httpMock.expectOne(`${apiService.baseUrl}/catalog/services`);
      req.flush([
        { id: 1, name: 'Excavadora Oruga Cat 320', dailyPrice: 150000, isAvailable: true },
        { serialNumber: 'CAT-320-001', dailyPrice: 140000, isAvailable: false, category: { name: 'Pesada' }, description: 'Propia' },
        { name: 'Retroexcavadora John Deere 310L', isAvailable: true },
        {}
      ]);
      expect(result.length).toBe(4);
      expect(result[0].name).toBe('Excavadora Oruga Cat 320');
      expect(result[1].description).toBe('Propia');
      expect(result[1].category).toBe('Pesada');
      expect(result[3].category).toBe('General');
      expect(result[3].description).toBe('Sin descripción');

      let fallback: any[] = [];
      apiService.getAllCatalog().subscribe(res => (fallback = res));
      const reqAll = httpMock.expectOne(`${apiService.baseUrl}/catalog/services/all`);
      reqAll.error(new ProgressEvent('error'));
      expect(fallback.length).toBeGreaterThan(5);
    });

    it('should call all rentals, catalog CRUD, users, reports, rabbitmq, audit, cart and checkout endpoints', () => {
      vi.useFakeTimers();

      let addedMachine: any;
      apiService.addMachine({ name: 'Nueva' }).subscribe(m => (addedMachine = m));
      vi.advanceTimersByTime(800);
      expect(addedMachine.name).toBe('Nueva');

      apiService.getRentals().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/rentals`).flush([]);

      apiService.requestRental({ machineId: 1 }).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/rentals`).flush({});

      apiService.updateRentalStatus(1, 'APROBADO').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/rentals/1/status`).flush({});

      apiService.updateCatalogService(1, { dailyPrice: 1000 }).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/catalog/services/1`).flush({});

      apiService.createCatalogService({ name: 'Eq' }).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/catalog/services`).flush({});

      apiService.deleteCatalogService(1).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/catalog/services/1`).flush({});

      apiService.getAdminUsers().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/users`).flush([]);

      apiService.createAdminUser({}).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/users`).flush({});

      apiService.updateAdminUser(1, {}).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/users/1`).flush({});

      apiService.deleteAdminUser(1).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/users/1`).flush({});

      let kpi: any;
      apiService.getKpis().subscribe(res => (kpi = res));
      httpMock.expectOne(`${apiService.baseUrl}/reports/kpis`).flush({ dailyRentals: 9 });
      expect(kpi.rentalsPerDay[0]).toBe(9);

      apiService.getKpis().subscribe(res => (kpi = res));
      httpMock.expectOne(`${apiService.baseUrl}/reports/kpis`).flush({ rentalsPerDay: [1, 2], labels: ['L', 'M'] });
      expect(kpi.labels).toEqual(['L', 'M']);

      apiService.getKpis().subscribe(res => (kpi = res));
      httpMock.expectOne(`${apiService.baseUrl}/reports/kpis`).flush({});
      expect(kpi.labels.length).toBe(7);

      apiService.getReportKpis().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/reports/kpis?range=last24h`).flush({});
      apiService.getReportKpis('last7d').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/reports/kpis?range=last7d`).flush({});

      apiService.getTopServices().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/reports/top-services?range=last7d`).flush([]);
      apiService.getTopServices('last30d').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/reports/top-services?range=last30d`).flush([]);

      apiService.getRabbitQueues().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/queues`).flush([]);

      apiService.createRabbitQueue({ name: 'q.1' }).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/queues`).flush({});

      apiService.deleteRabbitQueue('q.1').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/queues/q.1`).flush({});

      apiService.getRabbitExchanges().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/exchanges`).flush([]);

      apiService.createRabbitExchange({ name: 'ex.1' }).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/exchanges`).flush({});

      apiService.deleteRabbitExchange('ex.1').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/exchanges/ex.1`).flush({});

      apiService.getRabbitBindings().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/bindings`).flush([]);

      apiService.createRabbitBinding({}).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/bindings`).flush({});

      apiService.deleteRabbitBinding({}).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/bindings`).flush({});

      apiService.getKafkaCluster().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/cluster`).flush({});

      apiService.getKafkaTopics().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/topics?includeInternal=false`).flush([]);

      apiService.getKafkaTopicDetail('rentals.events').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/topics/rentals.events`).flush({});

      apiService.createKafkaTopic({ name: 'test.topic' }).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/topics`).flush({});

      apiService.updateKafkaTopicConfig('test.topic', { partitions: 3 }).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/topics/test.topic/config`).flush({});

      apiService.deleteKafkaTopic('test.topic').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/topics/test.topic`).flush({});

      apiService.getKafkaConsumerGroups().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/consumer-groups`).flush([]);

      apiService.getKafkaConsumerGroupDetail('audit-group').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/consumer-groups/audit-group`).flush({});

      apiService.deleteKafkaConsumerGroup('audit-group').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/kafka/consumer-groups/audit-group`).flush({});

      apiService.getCatalogReportActivity().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/reports/catalog-activity`).flush({});

      apiService.getAuditTimeline().subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/audit/events`).flush([]);

      apiService.syncCart([]).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/cart/sync`).flush({});

      apiService.checkoutCart('express').subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/cart/checkout/express`).flush({});

      apiService.processCheckout({ type: 'EXPRESS' }).subscribe();
      httpMock.expectOne(`${apiService.baseUrl}/checkout/process`).flush({});
    });
  });

  describe('Route Guards (adminGuard, catalogGuard, roleGuard)', () => {
    const dummyRoute = {} as ActivatedRouteSnapshot;
    const dummyState = {} as RouterStateSnapshot;

    function resolveGuardResult(obs: any): any {
      let value: any;
      if (isObservable(obs)) {
        (obs as Observable<any>).subscribe(v => (value = v));
      }
      return value;
    }

    it('adminGuard should allow Administrador and block others', () => {
      vi.useFakeTimers();
      const accounts: AccountInfo[] = [];
      const msalMock = {
        initialize: () => of(void 0),
        handleRedirectObservable: () => of(null),
        instance: { getAllAccounts: () => accounts }
      };

      TestBed.configureTestingModule({
        providers: [provideRouter([]), { provide: MsalService, useValue: msalMock }]
      });

      const accessDenied = TestBed.inject(AccessDeniedService);
      const showSpy = vi.spyOn(accessDenied, 'show');

      // Deny when unauthenticated
      const deniedResult = TestBed.runInInjectionContext(() => resolveGuardResult(adminGuard(dummyRoute, dummyState)));
      expect(deniedResult instanceof UrlTree).toBe(true);
      vi.advanceTimersByTime(150);
      expect(showSpy).toHaveBeenCalled();

      // Allow Administrador
      accounts.push(makeAccount({ idTokenClaims: { roles: ['Administrador'] } }));
      const allowedResult = TestBed.runInInjectionContext(() => resolveGuardResult(adminGuard(dummyRoute, dummyState)));
      expect(allowedResult).toBe(true);
    });

    it('catalogGuard should allow visitors & Arrendatario, and block internal roles', () => {
      const accounts: AccountInfo[] = [];
      const msalMock = {
        initialize: () => of(void 0),
        handleRedirectObservable: () => of(null),
        instance: { getAllAccounts: () => accounts }
      };

      TestBed.configureTestingModule({
        providers: [provideRouter([]), { provide: MsalService, useValue: msalMock }]
      });

      const accessDenied = TestBed.inject(AccessDeniedService);
      const showSpy = vi.spyOn(accessDenied, 'show');

      expect(TestBed.runInInjectionContext(() => resolveGuardResult(catalogGuard(dummyRoute, dummyState)))).toBe(true);

      accounts.push(makeAccount({ idTokenClaims: { roles: ['Arrendatario'] } }));
      expect(TestBed.runInInjectionContext(() => resolveGuardResult(catalogGuard(dummyRoute, dummyState)))).toBe(true);

      accounts[0] = makeAccount({ idTokenClaims: { roles: ['Administrador'] } });
      const blocked = TestBed.runInInjectionContext(() => resolveGuardResult(catalogGuard(dummyRoute, dummyState)));
      expect(blocked instanceof UrlTree).toBe(true);
      expect(showSpy).toHaveBeenCalled();
    });

    it('roleGuard should allow listed roles and deny unlisted roles', () => {
      vi.useFakeTimers();
      const accounts: AccountInfo[] = [makeAccount({ idTokenClaims: { roles: ['Auditor'] } })];
      const msalMock = {
        initialize: () => of(void 0),
        handleRedirectObservable: () => of(null),
        instance: { getAllAccounts: () => accounts }
      };

      TestBed.configureTestingModule({
        providers: [provideRouter([]), { provide: MsalService, useValue: msalMock }]
      });

      const guard = roleGuard('Administrador', 'Auditor');
      expect(TestBed.runInInjectionContext(() => resolveGuardResult(guard(dummyRoute, dummyState)))).toBe(true);

      accounts.length = 0;
      const denied = TestBed.runInInjectionContext(() => resolveGuardResult(guard(dummyRoute, dummyState)));
      expect(denied instanceof UrlTree).toBe(true);
      vi.advanceTimersByTime(150);
    });
  });

  describe('HTTP Interceptors (loadingInterceptor & errorInterceptor)', () => {
    it('should track loading state and handle HTTP error status codes', () => {
      const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(withInterceptors([errorInterceptor, loadingInterceptor])),
          provideHttpClientTesting()
        ]
      });

      const http = TestBed.inject(HttpClient);
      const httpMock = TestBed.inject(HttpTestingController);
      const loadingService = TestBed.inject(LoadingService);

      // Success clears loading
      http.get('/api/rentals').subscribe();
      expect(loadingService.isLoading()).toBe(true);
      httpMock.expectOne('/api/rentals').flush([]);
      expect(loadingService.isLoading()).toBe(false);

      // 401 silent
      let err401: Error | undefined;
      http.get('/api/other').subscribe({ error: e => (err401 = e) });
      httpMock.expectOne('/api/other').flush({}, { status: 401, statusText: 'Unauthorized' });
      expect(err401?.message).toBe('No autorizado');

      // 403 on whitelisted path (no alert)
      let err403: HttpErrorResponse | undefined;
      http.get('/api/rentals').subscribe({ error: e => (err403 = e) });
      httpMock.expectOne('/api/rentals').flush({}, { status: 403, statusText: 'Forbidden' });
      expect(err403?.status).toBe(403);
      expect(alertSpy).not.toHaveBeenCalled();

      // Status 0 / network error on /kafka whitelisted path (no alert)
      let errKafka: HttpErrorResponse | undefined;
      http.get('/v2/api/kafka/cluster').subscribe({ error: e => (errKafka = e) });
      httpMock.expectOne('/v2/api/kafka/cluster').error(new ProgressEvent('error'));
      expect(errKafka?.status).toBe(0);
      expect(alertSpy).not.toHaveBeenCalled();

      // 500 on non-whitelisted path (triggers alert)
      let err500: Error | undefined;
      http.get('/api/external').subscribe({ error: e => (err500 = e) });
      httpMock.expectOne('/api/external').flush({}, { status: 500, statusText: 'Server Error' });
      expect(err500?.message).toContain('500');
      expect(alertSpy).toHaveBeenCalled();

      // Other status on non-whitelisted path + ErrorEvent
      http.get('/api/external').subscribe({ error: () => {} });
      httpMock.expectOne('/api/external').flush({}, { status: 404, statusText: 'Not Found' });
      http.get('/api/external').subscribe({ error: () => {} });
      httpMock.expectOne('/api/external').error(new ErrorEvent('Network', { message: 'Offline' }) as any);

      httpMock.verify();
    });
  });
});
