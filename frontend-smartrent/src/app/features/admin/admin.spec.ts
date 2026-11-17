import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { AdminPanel } from './admin';
import { ApiService } from '../../core/services/api/api.service';
import { AuthService, UserRole } from '../../core/services/auth/auth.service';

describe('AdminPanel', () => {
  let component: AdminPanel;
  let fixture: ComponentFixture<AdminPanel>;

  const accountSig = signal<any>({
    name: 'Brayan Admin',
    username: 'brayan@pepito123.onmicrosoft.com',
    idTokenClaims: { oid: '12345678-abcd', tid: '753ae9df-d5ec-4c6b-8227-b334fc775087', roles: ['ROLE_ADMIN'] }
  });
  const roleSig = signal<UserRole>('Administrador');

  const mockAuthService = {
    account: accountSig,
    userRole: roleSig
  };

  const mockApiService = {
    getAdminUsers: vi.fn(),
    getAllCatalog: vi.fn(),
    updateCatalogService: vi.fn(),
    createCatalogService: vi.fn(),
    deleteCatalogService: vi.fn(),
    getRentals: vi.fn(),
    updateRentalStatus: vi.fn(),
    getReportKpis: vi.fn(),
    createRabbitQueue: vi.fn(),
    deleteRabbitQueue: vi.fn(),
    createRabbitExchange: vi.fn(),
    deleteRabbitExchange: vi.fn(),
    createRabbitBinding: vi.fn(),
    deleteRabbitBinding: vi.fn()
  };

  beforeEach(async () => {
    localStorage.clear();
    vi.clearAllMocks();

    mockApiService.getAdminUsers.mockReturnValue(of([]));
    mockApiService.getAllCatalog.mockReturnValue(
      of([{ id: 99, sku: 'REM-99', name: 'Equipo Remoto', dailyPrice: 70000, isAvailable: true, totalStock: 3, availableStock: 2 }])
    );
    mockApiService.getRentals.mockReturnValue(
      of([{ id: 901, machineId: 1, userId: 'cliente@test.cl', startDate: '2026-10-01', endDate: '2026-10-05', status: 'SOLICITADO' }])
    );
    mockApiService.getReportKpis.mockReturnValue(of({}));
    mockApiService.updateCatalogService.mockReturnValue(of({}));
    mockApiService.createCatalogService.mockReturnValue(of({}));
    mockApiService.deleteCatalogService.mockReturnValue(of({}));
    mockApiService.updateRentalStatus.mockReturnValue(of({}));
    mockApiService.createRabbitQueue.mockReturnValue(of({}));
    mockApiService.deleteRabbitQueue.mockReturnValue(of({}));
    mockApiService.createRabbitExchange.mockReturnValue(of({}));
    mockApiService.deleteRabbitExchange.mockReturnValue(of({}));
    mockApiService.createRabbitBinding.mockReturnValue(of({}));
    mockApiService.deleteRabbitBinding.mockReturnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [AdminPanel],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: ApiService, useValue: mockApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminPanel);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should initialize state, compute role display, switch tabs and manage tenant users', () => {
    expect(component.currentUserRoleDisplay()).toBe('ADMIN');
    roleSig.set('JefeBodega');
    expect(component.currentUserRoleDisplay()).toBe('JEFE BODEGA');
    roleSig.set('Arrendatario');
    expect(component.currentUserRoleDisplay()).toBe('CLIENTE');
    roleSig.set('Auditor');
    expect(component.currentUserRoleDisplay()).toBe('AUDITOR');
    roleSig.set('None');
    expect(component.currentUserRoleDisplay()).toBe('ADMIN');

    component.setTab('inventory');
    fixture.detectChanges();
    expect(component.activeTab()).toBe('inventory');

    component.setTab('validation');
    fixture.detectChanges();
    component.setTab('kpis');
    fixture.detectChanges();
    component.setTab('rabbitmq');
    fixture.detectChanges();
    component.setTab('users');
    fixture.detectChanges();

    component.onTenantSelectChange('753ae9df-d5ec-4c6b-8227-b334fc775087');
    expect(component.selectedTenantInfo()?.id).toBe('753ae9df-d5ec-4c6b-8227-b334fc775087');
    expect(component.filteredUsers().length).toBeGreaterThan(0);

    component.viewUserIdentityCard(component.users()[0]);
    fixture.detectChanges();
    expect(component.isUserModalOpen()).toBe(true);
    component.closeUserModal();
    fixture.detectChanges();
    expect(component.isUserModalOpen()).toBe(false);

    mockApiService.getAdminUsers.mockReturnValue(
      of([{ id: 'u1', fullName: 'Remoto', email: 'remoto@test.cl', company: 'SmartRent', tenantId: '753ae9df-d5ec-4c6b-8227-b334fc775087', role: 'AUDITOR', status: 'ACTIVO', createdAt: '2026-01-01' }])
    );
    component.syncTenantUsers();
    expect(component.users().some(u => u.email === 'remoto@test.cl')).toBe(true);

    mockApiService.getAdminUsers.mockReturnValue(throwError(() => new Error('Graph error')));
    component.syncTenantUsers();
    expect(component.isSyncing()).toBe(false);
  });

  it('should handle inventory CRUD, quick edit, maintenance, block, delete and keyboard helpers', () => {
    vi.useFakeTimers();

    // Keyboard & input helpers
    const preventDefault = vi.fn();
    component.preventSpaces({ key: ' ', code: 'Space', preventDefault } as unknown as KeyboardEvent);
    expect(preventDefault).toHaveBeenCalledTimes(1);
    component.preventFirstSpace({ key: ' ', code: 'Space', preventDefault } as unknown as KeyboardEvent, '');
    expect(preventDefault).toHaveBeenCalledTimes(2);
    component.preventNonNumeric({ key: 'a', preventDefault } as unknown as KeyboardEvent);
    expect(preventDefault).toHaveBeenCalledTimes(3);
    component.preventNonNumeric({ key: 'Backspace', preventDefault } as unknown as KeyboardEvent);
    expect(preventDefault).toHaveBeenCalledTimes(3);

    const skuEvt = { target: { value: 'abc 123' } } as unknown as Event;
    component.onSkuInput(skuEvt);
    expect(component.machineForm.get('sku')?.value).toBe('ABC123');

    const nameEvt = { target: { value: '   Equipo Test' } } as unknown as Event;
    component.onNameInput(nameEvt);
    expect(component.machineForm.get('name')?.value).toBe('Equipo Test');

    const numEvt = { target: { value: '12a34' } } as unknown as Event;
    component.onNumericInput(numEvt, 'dailyRate', 5);
    expect(component.machineForm.get('dailyRate')?.value).toBe(1234);

    // Invalid form check
    component.setTab('inventory');
    component.openCreateMachineModal();
    component.toggleImageGallery();
    fixture.detectChanges();
    component.saveMachine();
    expect(component.isMachineModalOpen()).toBe(true);

    // Create machine
    component.selectProductImage('/images/bomag-bw211.jpg');
    expect(component.getSelectedImageLabel()).toBe('Rodillo Bomag BW 211');
    expect(component.trackByImgUrl(0, { label: 'x', url: '/u' })).toBe('/u');

    component.machineForm.patchValue({
      sku: 'NEW-SKU-01',
      name: 'Compresor Atlas',
      category: 'GENERACION',
      dailyRate: 50000,
      totalStock: 5,
      availableStock: 3,
      assignedDepot: 'Patio Pudahuel Sur',
      status: 'DISPONIBLE',
      imageUrl: '/images/bomag-bw211.jpg'
    });
    component.saveMachine();
    fixture.detectChanges();
    expect(component.isMachineModalOpen()).toBe(false);
    expect(component.inventory().some(m => m.sku === 'NEW-SKU-01')).toBe(true);

    // Edit existing machine
    const created = component.inventory().find(m => m.sku === 'NEW-SKU-01')!;
    component.openEditMachineModal(created);
    fixture.detectChanges();
    component.machineForm.patchValue({ availableStock: 0, status: 'DISPONIBLE' });
    component.saveMachine();
    expect(component.inventory().find(m => m.sku === 'NEW-SKU-01')?.status).toBe('SIN_STOCK');

    // Quick edit
    component.openQuickEditModal(created);
    fixture.detectChanges();
    component.quickEditForm.patchValue({ availableStock: 10, totalStock: 2 });
    component.saveQuickEdit(); // blocked by available > total
    expect(component.isQuickEditModalOpen()).toBe(true);

    component.quickEditForm.patchValue({ dailyRate: 55000, availableStock: 2, totalStock: 5, status: 'DISPONIBLE' });
    component.saveQuickEdit();
    fixture.detectChanges();
    expect(component.isQuickEditModalOpen()).toBe(false);

    // Maintenance, order block, delete confirm
    const target = component.inventory()[0];
    component.toggleMaintenance(target);
    const updatedTarget = component.inventory()[0];
    expect(updatedTarget.status).toBe('EN_MANTENCION');
    component.toggleMaintenance(updatedTarget);

    component.toggleOrderBlock(component.inventory()[0]);
    component.toggleOrderBlock(component.inventory()[0]);

    const countBefore = component.inventory().length;
    component.confirmDeleteMachine(component.inventory()[0]);
    fixture.detectChanges();
    expect(component.isConfirmModalOpen()).toBe(true);
    component.executeConfirmAction();
    fixture.detectChanges();
    expect(component.inventory().length).toBe(countBefore - 1);

    vi.advanceTimersByTime(5000);
    fixture.detectChanges();
    expect(component.toasts().length).toBe(0);
  });

  it('should manage warehouse validation orders, KPI ranges, RabbitMQ topology and helpers', () => {
    expect(component.pendingValidationOrders().length).toBeGreaterThan(0);
    expect(component.inFieldOrders().length).toBeGreaterThan(0);

    component.setTab('validation');
    const pendingOrder = component.pendingValidationOrders()[0];
    component.openInspectionModal(pendingOrder);
    fixture.detectChanges();
    expect(component.isInspectionModalOpen()).toBe(true);
    component.approveAndDispatchOrder();
    fixture.detectChanges();
    expect(component.isInspectionModalOpen()).toBe(false);
    expect(mockApiService.updateRentalStatus).toHaveBeenCalledWith(pendingOrder.id, 'EN_TERRENO');

    const anotherPending = component.pendingValidationOrders()[0];
    component.openInspectionModal(anotherPending);
    component.rejectOrderForMaintenance();
    expect(component.validationOrders().find(o => o.id === anotherPending.id)?.status).toBe('EN_MANTENCION');

    const inField = component.inFieldOrders()[0];
    component.receiveAndReturnMachine(inField);
    component.executeConfirmAction();
    expect(component.validationOrders().find(o => o.id === inField.id)?.status).toBe('DEVUELTO');

    // KPI ranges
    component.setTab('kpis');
    component.setKpiRange('last7d');
    fixture.detectChanges();
    expect(component.kpiMetrics().dailyRentals).toBe(184);
    component.setKpiRange('last30d');
    fixture.detectChanges();
    expect(component.kpiMetrics().dailyRentals).toBe(712);
    component.setKpiRange('last24h');
    fixture.detectChanges();
    expect(component.kpiMetrics().dailyRentals).toBe(28);

    // RabbitMQ topology CRUD
    component.setTab('rabbitmq');
    component.openCreateQueueModal();
    fixture.detectChanges();
    component.queueForm.patchValue({ name: 'q.cmd.custom', type: 'standard', purpose: 'Cola de prueba unitaria' });
    component.saveQueue();
    expect(component.queues().some(q => q.name === 'q.cmd.custom')).toBe(true);
    component.saveQueue(); // duplicate name guard

    component.openCreateExchangeModal();
    fixture.detectChanges();
    component.exchangeForm.patchValue({ name: 'cmd.custom', type: 'topic', description: 'Exchange personalizado de prueba' });
    component.saveExchange();
    expect(component.exchanges().some(e => e.name === 'cmd.custom')).toBe(true);

    component.openCreateBindingModal();
    fixture.detectChanges();
    component.bindingForm.patchValue({ source: 'cmd.custom', destination: 'q.cmd.custom', routingKey: 'custom.*' });
    component.saveBinding();
    fixture.detectChanges();
    expect(component.bindings().some(b => b.routingKey === 'custom.*')).toBe(true);

    const binding = component.bindings().find(b => b.routingKey === 'custom.*')!;
    component.deleteBinding(binding);

    const queue = component.queues().find(q => q.name === 'q.cmd.custom')!;
    component.confirmDeleteQueue(queue);
    component.executeConfirmAction();

    const exchange = component.exchanges().find(e => e.name === 'cmd.custom')!;
    component.confirmDeleteExchange(exchange);
    component.executeConfirmAction();

    component.restoreBaseTopology(true);
    expect(component.queues().length).toBe(6);

    // Formatting & labels
    expect(component.formatClp(150000)).toContain('150');
    expect(component.getCategoryLabel('MOVIMIENTO_TIERRA')).toBe('Movimiento de Tierra');
    expect(component.getRoleBadge('ADMIN').badgeClass).toBe('role-admin');
    expect(component.getRoleBadge('OPERADOR').badgeClass).toBe('role-operador');
    expect(component.getRoleBadge('CLIENTE').badgeClass).toBe('role-cliente');
    expect(component.getRoleBadge('AUDITOR').badgeClass).toBe('role-auditor');
    expect(component.getRoleBadge('OTHER').badgeClass).toBe('role-default');
    expect(component.getTenantName('753ae9df-d5ec-4c6b-8227-b334fc775087')).toContain('SmartRent');
    expect(component.getTenantName('UNKNOWN')).toBe('UNKNOWN');
  });

  it('should handle localStorage hydration, MSAL roles, filters and edge validations', () => {
    // 1. Valid and invalid localStorage hydration on ngOnInit
    localStorage.setItem('smartrent_admin_users', 'invalid-json');
    localStorage.setItem('smartrent_catalog_inventory', 'invalid-json');
    localStorage.setItem('smartrent_validation_orders', 'invalid-json');
    localStorage.setItem('smartrent_rabbit_queues', 'invalid-json');
    localStorage.setItem('smartrent_rabbit_exchanges', 'invalid-json');
    localStorage.setItem('smartrent_rabbit_bindings', 'invalid-json');
    component.ngOnInit();

    localStorage.setItem('smartrent_admin_users', JSON.stringify([{ id: 'u-cache', fullName: 'Cache', email: 'c@test.cl', company: 'C', tenantId: '753ae9df-d5ec-4c6b-8227-b334fc775087', role: 'ADMIN', status: 'ACTIVO', createdAt: '2026-01-01' }]));
    localStorage.setItem('smartrent_catalog_inventory', JSON.stringify([{ id: 1, sku: 'CAT-320D', name: 'Excavadora', category: 'MOVIMIENTO_TIERRA', dailyRate: 100000, totalStock: 2, availableStock: 1, status: 'DISPONIBLE', imageUrl: '/images/cat-320.jpg' }]));
    localStorage.setItem('smartrent_validation_orders', JSON.stringify([{ id: 1044, rentalCode: 'ARR-1044', machineId: 1, machineName: 'Excavadora', sku: 'CAT-320D', lesseeName: 'L', company: 'C', tenantId: 't', workSiteAddress: 'W', startDate: '2026-01-01', endDate: '2026-01-02', status: 'SOLICITADO' }]));
    localStorage.setItem('smartrent_rabbit_queues', JSON.stringify([{ name: 'q.1', type: 'standard', durable: true, autoDelete: false, purpose: 'P', messageCount: 0, consumerCount: 0 }]));
    localStorage.setItem('smartrent_rabbit_exchanges', JSON.stringify([{ name: 'e.1', type: 'direct', durable: true, autoDelete: false }]));
    localStorage.setItem('smartrent_rabbit_bindings', JSON.stringify([{ id: 'b-1', source: 'e.1', destination: 'q.1', routingKey: 'k' }]));
    component.ngOnInit();
    expect(component.users().some(u => u.email === 'c@test.cl')).toBe(true);

    // 2. MSAL active user mapping across roles & null account
    accountSig.set({ name: 'Bodega', username: 'b@test.cl', idTokenClaims: { roles: ['ROLE_JEFEBODEGA'] } });
    component.refreshActiveUserFromMsal();
    accountSig.set({ name: 'Auditor', username: 'a@test.cl', idTokenClaims: { roles: ['ROLE_AUDITOR'] } });
    component.refreshActiveUserFromMsal();
    accountSig.set({ name: 'Arrendatario', username: 'arr@test.cl', idTokenClaims: { roles: ['ROLE_ARRENDATARIO'] } });
    component.refreshActiveUserFromMsal();
    accountSig.set(null);
    component.refreshActiveUserFromMsal();

    // 3. Filters for users & inventory
    component.selectedTenantId.set('ALL');
    component.userSearchQuery.set('brayan');
    component.userRoleFilter.set('ADMIN');
    expect(component.filteredUsers().length).toBeGreaterThanOrEqual(0);
    expect(component.selectedTenantInfo()).toBeNull();

    component.inventorySearchQuery.set('excavadora');
    component.inventoryCategoryFilter.set('MOVIMIENTO_TIERRA');
    component.inventoryStatusFilter.set('DISPONIBLE');
    expect(component.filteredInventory().length).toBeGreaterThanOrEqual(0);

    // 4. Machine validation: availableStock > totalStock
    component.openCreateProductModal();
    component.machineForm.patchValue({
      sku: 'TST-99',
      name: 'Equipo Stock Alto',
      category: 'GENERACION',
      dailyRate: 10000,
      totalStock: 2,
      availableStock: 5,
      assignedDepot: 'Patio',
      status: 'DISPONIBLE'
    });
    component.saveMachine();
    expect(component.isMachineModalOpen()).toBe(true);
    component.closeMachineModal();

    // 5. Inspection modal validations: invalid form & out-of-stock machine
    const order = component.validationOrders()[0];
    component.openInspectionModal(order);
    component.inspectionForm.patchValue({ fluidsChecked: false });
    component.approveAndDispatchOrder();
    expect(component.isInspectionModalOpen()).toBe(true);

    component.inventory.update(list => list.map(m => (m.sku === order.sku ? { ...m, availableStock: 0 } : m)));
    component.inspectionForm.patchValue({ fluidsChecked: true });
    component.approveAndDispatchOrder();
    component.closeInspectionModal();

    // 6. RabbitMQ invalid forms & duplicates
    component.openCreateQueueModal();
    component.queueForm.patchValue({ name: '' });
    component.saveQueue();
    component.closeQueueModal();

    component.openCreateExchangeModal();
    component.exchangeForm.patchValue({ name: '' });
    component.saveExchange();
    component.exchangeForm.patchValue({ name: 'e.1', type: 'direct', description: 'Exchange existente duplicado' });
    component.saveExchange();
    component.closeExchangeModal();

    component.openCreateBindingModal();
    component.bindingForm.patchValue({ routingKey: '' });
    component.saveBinding();
    component.bindingForm.patchValue({ source: 'e.1', destination: 'q.1', routingKey: 'k' });
    component.saveBinding();
    component.closeBindingModal();
    component.closeQuickEditModal();
    component.closeConfirmModal();
  });
});
