import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api/api.service';
import { AuthService } from '../../core/services/auth/auth.service';

export type AdminTab = 'users' | 'inventory' | 'validation' | 'kpis' | 'rabbitmq';

export interface TenantInfo {
  id: string;
  name: string;
  rut: string;
  headquarters: string;
  activeUsersCount: number;
  contactEmail: string;
  depotLocation: string;
  status: 'ACTIVO' | 'EN_MIGRACION';
}

export interface AdminUser {
  id: string | number;
  fullName: string;
  email: string;
  company: string;
  tenantId: string;
  role: 'ADMIN' | 'OPERADOR' | 'CLIENTE' | 'AUDITOR';
  status: 'ACTIVO' | 'INACTIVO';
  createdAt: string;
}

export type MachineCategory =
  | 'MOVIMIENTO_TIERRA'
  | 'COMPACTACION'
  | 'GENERACION'
  | 'ELEVACION'
  | 'HERRAMIENTAS'
  | 'ACCESORIOS';

export interface MachineItem {
  id: number | string;
  sku: string;
  name: string;
  category: MachineCategory;
  dailyRate: number;
  totalStock: number;
  availableStock: number;
  status: 'DISPONIBLE' | 'EN_MANTENCION' | 'SIN_STOCK';
  imageUrl: string;
  serialNumber?: string;
  location?: string;
  assignedDepot?: string;
  isOrderBlocked?: boolean;
}

export interface MachineValidationOrder {
  id: number | string;
  rentalCode: string;
  machineId: number | string;
  machineName: string;
  sku: string;
  lesseeName: string;
  company: string;
  tenantId: string;
  workSiteAddress: string;
  startDate: string;
  endDate: string;
  status: 'SOLICITADO' | 'EN_PREPARACION' | 'EN_TERRENO' | 'DEVUELTO' | 'EN_MANTENCION';
  horometerOut?: number;
  horometerIn?: number;
  inspectedBy?: string;
  inspectionDate?: string;
  notes?: string;
  fluidsChecked?: boolean;
  structureChecked?: boolean;
  securityKitChecked?: boolean;
  brakeEngineChecked?: boolean;
  checklistPassed?: boolean;
}

export interface RabbitExchange {
  name: string;
  type: 'direct' | 'topic' | 'fanout' | 'headers';
  durable: boolean;
  autoDelete: boolean;
  description?: string;
  isOfficial?: boolean;
}

export interface RabbitQueue {
  name: string;
  type: 'standard' | 'quorum' | 'dlq';
  durable: boolean;
  autoDelete: boolean;
  dlqAssociated?: string;
  purpose: string;
  messageCount: number;
  consumerCount: number;
  isOfficial?: boolean;
}

export interface RabbitBinding {
  id: string;
  source: string;
  destination: string;
  routingKey: string;
}

export interface TopServiceMetric {
  sku: string;
  name: string;
  category: string;
  rentalsCount: number;
  occupancyPercent: number;
  revenueClp: number;
  imageUrl: string;
}

export interface RentalStatusMetric {
  status: string;
  label: string;
  count: number;
  percentage: number;
  color: string;
  description: string;
}

export interface ToastNotification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
}

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './admin.html',
  styleUrl: './admin.css'
})
export class AdminPanel implements OnInit {
  public readonly apiService = inject(ApiService);
  public readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  // Active Tab
  public activeTab = signal<AdminTab>('users');

  // Loading & Feedback
  public isSyncing = signal<boolean>(false);
  public toasts = signal<ToastNotification[]>([]);

  // ==========================================
  // TAB 1: USERS & TENANTS STATE
  // ==========================================
  public readonly tenants: TenantInfo[] = [
    {
      id: 'TNT-CENTRAL-01',
      name: 'SmartRent Matriz Central',
      rut: '76.842.100-K',
      headquarters: 'Av. El Bosque Norte 500, Las Condes, RM',
      depotLocation: 'Patio Central Pudahuel',
      activeUsersCount: 3,
      contactEmail: 'admin@smartrent.cl',
      status: 'ACTIVO'
    },
    {
      id: 'TNT-PUDAHUEL-02',
      name: 'Maquinarias Pudahuel Sur SpA',
      rut: '77.201.340-8',
      headquarters: 'Parque Industrial Lo Boza, Pudahuel, RM',
      depotLocation: 'Patio Pudahuel Sur',
      activeUsersCount: 2,
      contactEmail: 'operaciones@pudahuel-rent.cl',
      status: 'ACTIVO'
    },
    {
      id: 'TNT-BESALCO-03',
      name: 'Constructora Besalco S.A.',
      rut: '96.550.210-4',
      headquarters: 'Av. Vitacura 2939, Las Condes, RM',
      depotLocation: 'Faena Lo Espejo / AVO II',
      activeUsersCount: 2,
      contactEmail: 'adquisiciones@besalco.cl',
      status: 'ACTIVO'
    },
    {
      id: 'TNT-ANTOF-04',
      name: 'Equipos Mineros Antofagasta Ltda',
      rut: '78.910.420-1',
      headquarters: 'Sector Industrial La Negra Módulo 14, Antofagasta',
      depotLocation: 'Patio La Negra Antofagasta',
      activeUsersCount: 2,
      contactEmail: 'patio@antofagasta-equipos.cl',
      status: 'ACTIVO'
    },
    {
      id: 'TNT-BIOBIO-05',
      name: 'Vial Rent Biobío SpA',
      rut: '79.430.880-3',
      headquarters: 'Ruta 160 Km 12, San Pedro de la Paz, Biobío',
      depotLocation: 'Patio San Pedro Biobío',
      activeUsersCount: 1,
      contactEmail: 'bodega@vial-biobio.cl',
      status: 'ACTIVO'
    },
    {
      id: 'TNT-KPMG-06',
      name: 'KPMG Auditores Consultores',
      rut: '81.320.100-2',
      headquarters: 'Av. Andrés Bello 2711, Providencia, RM',
      depotLocation: 'Oficina Central RM',
      activeUsersCount: 1,
      contactEmail: 'auditoria@kpmg-audit.cl',
      status: 'ACTIVO'
    },
    {
      id: 'TNT-ECHEVERRIA-07',
      name: 'Echeverría Izquierdo Ingeniería',
      rut: '96.720.500-9',
      headquarters: 'Av. El Valle 554, Huechuraba, RM',
      depotLocation: 'Faena Américo Vespucio Norte',
      activeUsersCount: 1,
      contactEmail: 'logistica@echeverria-izquierdo.cl',
      status: 'ACTIVO'
    }
  ];

  public selectedTenantId = signal<string>('ALL');
  public users = signal<AdminUser[]>([]);
  public userSearchQuery = signal<string>('');
  public userRoleFilter = signal<string>('ALL');
  public isUserModalOpen = signal<boolean>(false);
  public editingUser = signal<AdminUser | null>(null);

  public userForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    tenantId: ['TNT-CENTRAL-01', [Validators.required]],
    company: ['', [Validators.required, Validators.minLength(2)]],
    role: ['CLIENTE', [Validators.required]],
    status: ['ACTIVO', [Validators.required]]
  });

  // Selected Tenant Object computed
  public selectedTenantInfo = computed<TenantInfo | null>(() => {
    const tid = this.selectedTenantId();
    if (tid === 'ALL') return null;
    return this.tenants.find(t => t.id === tid) || null;
  });

  // Filtered Users computed signal by Tenant, Search and Role
  public filteredUsers = computed(() => {
    const query = this.userSearchQuery().toLowerCase().trim();
    const role = this.userRoleFilter();
    const tid = this.selectedTenantId();

    return this.users().filter(u => {
      const matchesTenant = tid === 'ALL' || u.tenantId === tid;
      const matchesSearch =
        !query ||
        u.fullName.toLowerCase().includes(query) ||
        u.email.toLowerCase().includes(query) ||
        u.company.toLowerCase().includes(query) ||
        u.tenantId.toLowerCase().includes(query);
      const matchesRole = role === 'ALL' || u.role === role;
      return matchesTenant && matchesSearch && matchesRole;
    });
  });

  // ==========================================
  // TAB 2: INVENTORY & STOCK MANAGEMENT
  // ==========================================
  public inventory = signal<MachineItem[]>([]);
  public inventorySearchQuery = signal<string>('');
  public inventoryCategoryFilter = signal<string>('ALL');
  public inventoryStatusFilter = signal<string>('ALL');
  public isMachineModalOpen = signal<boolean>(false);
  public editingMachine = signal<MachineItem | null>(null);
  public isQuickEditModalOpen = signal<boolean>(false);
  public quickEditingMachine = signal<MachineItem | null>(null);

  public readonly localCatalogImages: { label: string; url: string }[] = [
    { label: 'Excavadora Cat 320', url: '/images/cat-320.jpg' },
    { label: 'Rodillo Bomag BW 211', url: '/images/bomag-bw211.jpg' },
    { label: 'Generador Cummins 50kVA', url: '/images/cummins-50kva.jpg' },
    { label: 'Demoledor Bosch GSH 11E', url: '/images/bosch-gsh11e.jpg' },
    { label: 'Sierra DeWalt DWE575', url: '/images/dewalt-dwe575.jpg' },
    { label: 'Miniexcavadora Terreno', url: '/images/excavator.jpg' },
    { label: 'Grúa Móvil Grove', url: '/images/crane.jpg' },
    { label: 'Grupo Generador Industrial', url: '/images/generator.jpg' }
  ];

  public machineForm: FormGroup = this.fb.group({
    sku: ['', [Validators.required, Validators.pattern(/^[A-Z0-9-]+$/)]],
    name: ['', [Validators.required, Validators.minLength(3)]],
    category: ['MOVIMIENTO_TIERRA', [Validators.required]],
    dailyRate: [50000, [Validators.required, Validators.min(1000)]],
    totalStock: [5, [Validators.required, Validators.min(1)]],
    availableStock: [5, [Validators.required, Validators.min(0)]],
    assignedDepot: ['Patio Pudahuel Sur', [Validators.required]],
    status: ['DISPONIBLE', [Validators.required]],
    imageUrl: ['/images/cat-320.jpg', [Validators.required]]
  });

  public quickEditForm: FormGroup = this.fb.group({
    dailyRate: [0, [Validators.required, Validators.min(1000)]],
    availableStock: [0, [Validators.required, Validators.min(0)]],
    totalStock: [0, [Validators.required, Validators.min(1)]],
    status: ['DISPONIBLE', [Validators.required]],
    assignedDepot: ['']
  });

  // Filtered Inventory computed
  public filteredInventory = computed(() => {
    const query = this.inventorySearchQuery().toLowerCase().trim();
    const cat = this.inventoryCategoryFilter();
    const st = this.inventoryStatusFilter();

    return this.inventory().filter(m => {
      const matchesSearch =
        !query ||
        m.name.toLowerCase().includes(query) ||
        m.sku.toLowerCase().includes(query) ||
        (m.assignedDepot && m.assignedDepot.toLowerCase().includes(query));
      const matchesCat = cat === 'ALL' || m.category === cat;
      const matchesStatus = st === 'ALL' || m.status === st;
      return matchesSearch && matchesCat && matchesStatus;
    });
  });

  // Fleet Stats computed
  public fleetStats = computed(() => {
    const list = this.inventory();
    const total = list.reduce((acc, curr) => acc + (curr.totalStock || 0), 0);
    const available = list.reduce((acc, curr) => acc + (curr.availableStock || 0), 0);
    const inMaintenance = list.filter(m => m.status === 'EN_MANTENCION').reduce((acc, curr) => acc + (curr.totalStock - curr.availableStock), 0);
    const inUse = Math.max(0, total - available - inMaintenance);
    const occupancyRate = total > 0 ? Math.round(((total - available) / total) * 100) : 0;
    return { total, available, inUse, inMaintenance, occupancyRate };
  });

  // ==========================================
  // TAB 3: VALIDACIÓN DE BODEGA (JEFE DE BODEGA)
  // ==========================================
  public validationOrders = signal<MachineValidationOrder[]>([]);
  public isInspectionModalOpen = signal<boolean>(false);
  public inspectingOrder = signal<MachineValidationOrder | null>(null);

  public inspectionForm: FormGroup = this.fb.group({
    operatorName: ['Carlos Mendoza (Jefe de Bodega)', [Validators.required]],
    horometerOut: [1420, [Validators.required, Validators.min(0)]],
    fluidsChecked: [true, [Validators.requiredTrue]],
    structureChecked: [true, [Validators.requiredTrue]],
    securityKitChecked: [true, [Validators.requiredTrue]],
    brakeEngineChecked: [true, [Validators.requiredTrue]],
    notes: ['Equipo verificado en patio. Presión hidráulica 210 bar y combustible al 100%. Aprobado para despacho.']
  });

  // Pending vs In-Field orders computed
  public pendingValidationOrders = computed(() =>
    this.validationOrders().filter(o => o.status === 'SOLICITADO' || o.status === 'EN_PREPARACION')
  );

  public inFieldOrders = computed(() =>
    this.validationOrders().filter(o => o.status === 'EN_TERRENO')
  );

  // ==========================================
  // TAB 4: KPIS & NETWORK METRICS
  // ==========================================
  public kpiRange = signal<'last24h' | 'last7d' | 'last30d'>('last24h');
  public kpiMetrics = signal({
    dailyRentals: 28,
    dailyRentalsComparison: '+14% vs día anterior',
    avgCycleHours: 3.4,
    avgCycleDesc: 'Solicitado -> Aprobado -> Terreno',
    activeMachines: 142,
    totalFleet: 180,
    occupancyPercent: 78.9,
    activeDailyRevenueClp: 14850000,
    activeDailyRevenueDesc: 'Facturación neta diaria vigente'
  });

  public topServices = signal<TopServiceMetric[]>([]);
  public rentalStatusDistribution = signal<RentalStatusMetric[]>([]);

  // ==========================================
  // TAB 5: RABBITMQ TOPOLOGY STATE & FORMS
  // ==========================================
  public exchanges = signal<RabbitExchange[]>([]);
  public queues = signal<RabbitQueue[]>([]);
  public bindings = signal<RabbitBinding[]>([]);

  public isQueueModalOpen = signal<boolean>(false);
  public isExchangeModalOpen = signal<boolean>(false);
  public isBindingModalOpen = signal<boolean>(false);
  public isConfirmModalOpen = signal<boolean>(false);
  public confirmActionData = signal<{ title: string; message: string; onConfirm: () => void } | null>(null);

  public queueForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]+$/), Validators.minLength(3)]],
    type: ['standard', [Validators.required]],
    durable: [true],
    autoDelete: [false],
    dlqAssociated: [''],
    purpose: ['', [Validators.required, Validators.minLength(5)]]
  });

  public exchangeForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]+$/), Validators.minLength(3)]],
    type: ['direct', [Validators.required]],
    durable: [true],
    autoDelete: [false],
    description: ['', [Validators.required, Validators.minLength(5)]]
  });

  public bindingForm: FormGroup = this.fb.group({
    source: ['', [Validators.required]],
    destination: ['', [Validators.required]],
    routingKey: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9.*#_-]+$/), Validators.minLength(2)]]
  });

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.initializeUsers();
    this.initializeInventory();
    this.initializeValidationOrders();
    this.initializeKpis();
    this.initializeRabbitTopology();
  }

  public setTab(tab: AdminTab): void {
    this.activeTab.set(tab);
  }

  // ==========================================
  // USERS LOGIC (CRUD & MULTI-TENANT)
  // ==========================================
  private initializeUsers(): void {
    const cached = localStorage.getItem('smartrent_admin_users');
    if (cached) {
      try {
        const parsed = JSON.parse(cached);
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.users.set(parsed);
          return;
        }
      } catch (e) {
        console.warn('Error reading cached users');
      }
    }

    const initialUsers: AdminUser[] = [
      {
        id: 'USR-101',
        fullName: 'Brayan González Leo',
        email: 'b.gonzalez@smartrent.cl',
        company: 'SmartRent Red Matriz (Central)',
        tenantId: 'TNT-CENTRAL-01',
        role: 'ADMIN',
        status: 'ACTIVO',
        createdAt: '2024-01-15'
      },
      {
        id: 'USR-102',
        fullName: 'Carlos Mendoza Silva',
        email: 'c.mendoza@pudahuel-rent.cl',
        company: 'Maquinarias Pudahuel Sur SpA',
        tenantId: 'TNT-PUDAHUEL-02',
        role: 'OPERADOR',
        status: 'ACTIVO',
        createdAt: '2024-02-01'
      },
      {
        id: 'USR-103',
        fullName: 'Constanza Valenzuela',
        email: 'c.valenzuela@besalco.cl',
        company: 'Constructora Besalco S.A.',
        tenantId: 'TNT-BESALCO-03',
        role: 'CLIENTE',
        status: 'ACTIVO',
        createdAt: '2024-02-18'
      },
      {
        id: 'USR-104',
        fullName: 'Rodrigo Araya Peña',
        email: 'r.araya@kpmg-audit.cl',
        company: 'KPMG Auditores & Consultores',
        tenantId: 'TNT-KPMG-06',
        role: 'AUDITOR',
        status: 'ACTIVO',
        createdAt: '2024-01-20'
      },
      {
        id: 'USR-105',
        fullName: 'Felipe Morales Tapia',
        email: 'f.morales@antofagasta-equipos.cl',
        company: 'Equipos Mineros Antofagasta Ltda',
        tenantId: 'TNT-ANTOF-04',
        role: 'OPERADOR',
        status: 'ACTIVO',
        createdAt: '2024-03-12'
      },
      {
        id: 'USR-106',
        fullName: 'Daniela Soto Navarrete',
        email: 'd.soto@echeverria-izquierdo.cl',
        company: 'Echeverría Izquierdo Ingeniería',
        tenantId: 'TNT-ECHEVERRIA-07',
        role: 'CLIENTE',
        status: 'ACTIVO',
        createdAt: '2024-04-05'
      },
      {
        id: 'USR-107',
        fullName: 'Héctor Paredes Rubio',
        email: 'h.paredes@vial-biobio.cl',
        company: 'Vial Rent Concepción SpA',
        tenantId: 'TNT-BIOBIO-05',
        role: 'OPERADOR',
        status: 'INACTIVO',
        createdAt: '2024-05-10'
      }
    ];

    this.users.set(initialUsers);
    this.saveUsersToStorage();

    this.apiService.getAdminUsers().subscribe({
      next: (remoteUsers: any[]) => {
        if (Array.isArray(remoteUsers) && remoteUsers.length > 0) {
          this.users.set(remoteUsers);
          this.saveUsersToStorage();
        }
      },
      error: () => {}
    });
  }

  private saveUsersToStorage(): void {
    localStorage.setItem('smartrent_admin_users', JSON.stringify(this.users()));
  }

  public onTenantSelectChange(tenantId: string): void {
    this.selectedTenantId.set(tenantId);
    const tenant = this.tenants.find(t => t.id === tenantId);
    if (tenant) {
      this.showToast('info', 'Filtro Tenant Aplicado', `Visualizando usuarios del tenant: ${tenant.name}`);
    }
  }

  public openCreateUserModal(): void {
    this.editingUser.set(null);
    const activeTenant = this.selectedTenantId() !== 'ALL' ? this.selectedTenantId() : 'TNT-CENTRAL-01';
    const foundTenant = this.tenants.find(t => t.id === activeTenant);

    this.userForm.reset({
      fullName: '',
      email: '',
      tenantId: activeTenant,
      company: foundTenant ? foundTenant.name : 'SmartRent Red Central',
      role: 'CLIENTE',
      status: 'ACTIVO'
    });
    this.isUserModalOpen.set(true);
  }

  public openEditUserModal(user: AdminUser): void {
    this.editingUser.set(user);
    this.userForm.patchValue({
      fullName: user.fullName,
      email: user.email,
      tenantId: user.tenantId,
      company: user.company,
      role: user.role,
      status: user.status
    });
    this.isUserModalOpen.set(true);
  }

  public onUserModalTenantChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const tid = select.value;
    const found = this.tenants.find(t => t.id === tid);
    if (found) {
      this.userForm.patchValue({ company: found.name });
    }
  }

  public closeUserModal(): void {
    this.isUserModalOpen.set(false);
    this.editingUser.set(null);
  }

  public saveUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      this.showToast('warning', 'Campos requeridos', 'Verifica el formulario antes de guardar el usuario.');
      return;
    }

    const formVal = this.userForm.value;
    const current = this.editingUser();

    if (current) {
      const updated: AdminUser = {
        ...current,
        fullName: formVal.fullName,
        email: formVal.email,
        tenantId: formVal.tenantId,
        company: formVal.company,
        role: formVal.role,
        status: formVal.status
      };

      this.users.update(list => list.map(u => (u.id === current.id ? updated : u)));
      this.saveUsersToStorage();
      this.closeUserModal();
      this.showToast('success', 'Usuario actualizado', `Datos actualizados para ${updated.fullName} (${updated.tenantId}).`);

      this.apiService.updateAdminUser(current.id, updated).subscribe({ error: () => {} });
    } else {
      const newUser: AdminUser = {
        id: `USR-${Math.floor(100 + Math.random() * 900)}`,
        fullName: formVal.fullName,
        email: formVal.email,
        tenantId: formVal.tenantId,
        company: formVal.company,
        role: formVal.role,
        status: formVal.status,
        createdAt: new Date().toISOString().split('T')[0]
      };

      this.users.update(list => [newUser, ...list]);
      this.saveUsersToStorage();
      this.closeUserModal();
      this.showToast('success', 'Usuario registrado', `Se dio de alta a ${newUser.fullName} en ${newUser.company}.`);

      this.apiService.createAdminUser(newUser).subscribe({ error: () => {} });
    }
  }

  public toggleUserStatus(user: AdminUser): void {
    const nextStatus: 'ACTIVO' | 'INACTIVO' = user.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    const updated: AdminUser = { ...user, status: nextStatus };

    this.users.update(list => list.map(u => (u.id === user.id ? updated : u)));
    this.saveUsersToStorage();
    this.showToast('info', 'Estado modificado', `El usuario ${user.fullName} quedó ${nextStatus}.`);

    this.apiService.updateAdminUser(user.id, updated).subscribe({ error: () => {} });
  }

  public confirmDeleteUser(user: AdminUser): void {
    this.openConfirmModal(
      'Eliminar Usuario',
      `¿Confirmas la eliminación del usuario "${user.fullName}" (${user.email}) del tenant ${user.tenantId}? Esta acción revocará sus credenciales corporativas.`,
      () => {
        this.users.update(list => list.filter(u => u.id !== user.id));
        this.saveUsersToStorage();
        this.showToast('success', 'Usuario eliminado', `Se eliminó al usuario ${user.fullName}.`);
        this.apiService.deleteAdminUser(user.id).subscribe({ error: () => {} });
      }
    );
  }

  // ==========================================
  // INVENTORY & STOCK MANAGEMENT LOGIC
  // ==========================================
  private initializeInventory(): void {
    const cached = localStorage.getItem('smartrent_catalog_inventory');
    if (cached) {
      try {
        const parsed = JSON.parse(cached);
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.inventory.set(parsed);
          return;
        }
      } catch (e) {
        console.warn('Error loading cached inventory');
      }
    }

    const initialMachines: MachineItem[] = [
      {
        id: 1,
        sku: 'CAT-320D',
        name: 'Excavadora Oruga Cat 320',
        category: 'MOVIMIENTO_TIERRA',
        dailyRate: 150000,
        totalStock: 6,
        availableStock: 2,
        status: 'DISPONIBLE',
        imageUrl: '/images/cat-320.jpg',
        serialNumber: 'CAT-320-001',
        assignedDepot: 'Patio Pudahuel Sur'
      },
      {
        id: 2,
        sku: 'BOM-BW211',
        name: 'Rodillo Compactador Bomag BW 211',
        category: 'COMPACTACION',
        dailyRate: 85000,
        totalStock: 5,
        availableStock: 3,
        status: 'DISPONIBLE',
        imageUrl: '/images/bomag-bw211.jpg',
        serialNumber: 'BMG-RC-909',
        assignedDepot: 'Patio La Negra Antofagasta'
      },
      {
        id: 3,
        sku: 'CUM-50KVA',
        name: 'Generador Diésel Cummins 50kVA Insonorizado',
        category: 'GENERACION',
        dailyRate: 60000,
        totalStock: 8,
        availableStock: 4,
        status: 'DISPONIBLE',
        imageUrl: '/images/cummins-50kva.jpg',
        serialNumber: 'CUM-50-GEN',
        assignedDepot: 'Patio Pudahuel Sur'
      },
      {
        id: 4,
        sku: 'BOS-GSH11E',
        name: 'Demoledor Bosch GSH 11 E Professional',
        category: 'HERRAMIENTAS',
        dailyRate: 25000,
        totalStock: 14,
        availableStock: 9,
        status: 'DISPONIBLE',
        imageUrl: '/images/bosch-gsh11e.jpg',
        serialNumber: 'BOS-GSH-11E',
        assignedDepot: 'Patio San Pedro Biobío'
      },
      {
        id: 5,
        sku: 'DEW-DWE575',
        name: 'Sierra Circular DeWalt DWE575K 7-1/4"',
        category: 'HERRAMIENTAS',
        dailyRate: 12000,
        totalStock: 20,
        availableStock: 12,
        status: 'DISPONIBLE',
        imageUrl: '/images/dewalt-dwe575.jpg',
        serialNumber: 'DEW-SC-575',
        assignedDepot: 'Patio Pudahuel Sur'
      },
      {
        id: 6,
        sku: 'EXC-MINI35',
        name: 'Miniexcavadora Terreno Komatsu PC35',
        category: 'MOVIMIENTO_TIERRA',
        dailyRate: 95000,
        totalStock: 4,
        availableStock: 1,
        status: 'DISPONIBLE',
        imageUrl: '/images/excavator.jpg',
        serialNumber: 'KOM-PC35-08',
        assignedDepot: 'Patio Central Pudahuel'
      },
      {
        id: 7,
        sku: 'CRN-RT550',
        name: 'Grúa Todo Terreno Grove RT550E 45T',
        category: 'ELEVACION',
        dailyRate: 320000,
        totalStock: 2,
        availableStock: 0,
        status: 'SIN_STOCK',
        imageUrl: '/images/crane.jpg',
        serialNumber: 'GRV-RT550-02',
        assignedDepot: 'Patio La Negra Antofagasta'
      },
      {
        id: 8,
        sku: 'GEN-IND100',
        name: 'Grupo Electrógeno Atlas Copco QAS 100',
        category: 'GENERACION',
        dailyRate: 90000,
        totalStock: 3,
        availableStock: 1,
        status: 'DISPONIBLE',
        imageUrl: '/images/generator.jpg',
        serialNumber: 'AC-QAS-100',
        assignedDepot: 'Patio San Pedro Biobío'
      }
    ];

    this.inventory.set(initialMachines);
    this.saveInventoryToStorage();
  }

  private saveInventoryToStorage(): void {
    localStorage.setItem('smartrent_catalog_inventory', JSON.stringify(this.inventory()));
  }

  public openCreateMachineModal(): void {
    this.editingMachine.set(null);
    this.machineForm.reset({
      sku: '',
      name: '',
      category: 'MOVIMIENTO_TIERRA',
      dailyRate: 65000,
      totalStock: 4,
      availableStock: 4,
      assignedDepot: 'Patio Pudahuel Sur',
      status: 'DISPONIBLE',
      imageUrl: '/images/cat-320.jpg'
    });
    this.isMachineModalOpen.set(true);
  }

  public openEditMachineModal(item: MachineItem): void {
    this.editingMachine.set(item);
    this.machineForm.patchValue({
      sku: item.sku,
      name: item.name,
      category: item.category,
      dailyRate: item.dailyRate,
      totalStock: item.totalStock,
      availableStock: item.availableStock,
      assignedDepot: item.assignedDepot || 'Patio Pudahuel Sur',
      status: item.status,
      imageUrl: item.imageUrl
    });
    this.isMachineModalOpen.set(true);
  }

  public closeMachineModal(): void {
    this.isMachineModalOpen.set(false);
    this.editingMachine.set(null);
  }

  public saveMachine(): void {
    if (this.machineForm.invalid) {
      this.machineForm.markAllAsTouched();
      this.showToast('warning', 'Campos incompletos', 'Completa los datos del equipo.');
      return;
    }

    const formVal = this.machineForm.value;
    const current = this.editingMachine();

    if (formVal.availableStock > formVal.totalStock) {
      this.showToast('warning', 'Stock inconsistente', 'El stock disponible no puede superar la flota total.');
      return;
    }

    let status: 'DISPONIBLE' | 'EN_MANTENCION' | 'SIN_STOCK' = formVal.status;
    if (formVal.availableStock === 0 && status === 'DISPONIBLE') {
      status = 'SIN_STOCK';
    }

    if (current) {
      const updated: MachineItem = {
        ...current,
        sku: formVal.sku,
        name: formVal.name,
        category: formVal.category,
        dailyRate: Number(formVal.dailyRate),
        totalStock: Number(formVal.totalStock),
        availableStock: Number(formVal.availableStock),
        assignedDepot: formVal.assignedDepot,
        status,
        imageUrl: formVal.imageUrl
      };

      this.inventory.update(list => list.map(m => (m.id === current.id ? updated : m)));
      this.saveInventoryToStorage();
      this.closeMachineModal();
      this.showToast('success', 'Equipo modificado', `Inventario actualizado para ${updated.name}.`);

      this.apiService.updateCatalogService(current.id, updated).subscribe({ error: () => {} });
    } else {
      const newMachine: MachineItem = {
        id: Date.now(),
        sku: formVal.sku,
        name: formVal.name,
        category: formVal.category,
        dailyRate: Number(formVal.dailyRate),
        totalStock: Number(formVal.totalStock),
        availableStock: Number(formVal.availableStock),
        assignedDepot: formVal.assignedDepot,
        status,
        imageUrl: formVal.imageUrl
      };

      this.inventory.update(list => [newMachine, ...list]);
      this.saveInventoryToStorage();
      this.closeMachineModal();
      this.showToast('success', 'Equipo creado', `Se incorporó ${newMachine.name} al catálogo.`);

      this.apiService.createCatalogService(newMachine).subscribe({ error: () => {} });
    }
  }

  // Quick stock edit
  public openQuickEditModal(item: MachineItem): void {
    this.quickEditingMachine.set(item);
    this.quickEditForm.patchValue({
      dailyRate: item.dailyRate,
      availableStock: item.availableStock,
      totalStock: item.totalStock,
      status: item.status,
      assignedDepot: item.assignedDepot || 'Patio Pudahuel Sur'
    });
    this.isQuickEditModalOpen.set(true);
  }

  public closeQuickEditModal(): void {
    this.isQuickEditModalOpen.set(false);
    this.quickEditingMachine.set(null);
  }

  public saveQuickEdit(): void {
    const item = this.quickEditingMachine();
    if (!item || this.quickEditForm.invalid) {
      this.showToast('warning', 'Valores inválidos', 'Revisa la tarifa y stock ingresados.');
      return;
    }

    const { dailyRate, availableStock, totalStock, status, assignedDepot } = this.quickEditForm.value;

    if (availableStock > totalStock) {
      this.showToast('warning', 'Stock excedido', 'El stock disponible no puede superar la flota total.');
      return;
    }

    const resolvedStatus: 'DISPONIBLE' | 'EN_MANTENCION' | 'SIN_STOCK' =
      availableStock === 0 && status === 'DISPONIBLE' ? 'SIN_STOCK' : status;

    const updated: MachineItem = {
      ...item,
      dailyRate: Number(dailyRate),
      availableStock: Number(availableStock),
      totalStock: Number(totalStock),
      assignedDepot,
      status: resolvedStatus
    };

    this.inventory.update(list => list.map(m => (m.id === item.id ? updated : m)));
    this.saveInventoryToStorage();
    this.closeQuickEditModal();
    this.showToast(
      'success',
      'Stock y Tarifa Sincronizados',
      `${item.sku}: Tarifa ${this.formatClp(dailyRate)} / Disp: ${availableStock} de ${totalStock}.`
    );

    this.apiService.updateCatalogService(item.id, {
      dailyPrice: updated.dailyRate,
      availableStock: updated.availableStock,
      totalStock: updated.totalStock,
      isAvailable: updated.availableStock > 0
    }).subscribe({ error: () => {} });
  }

  // Send to maintenance or return to service
  public toggleMaintenance(item: MachineItem): void {
    if (item.status === 'EN_MANTENCION') {
      // Reintegrar a disponible
      const newAvailable = Math.min(item.totalStock, item.availableStock + 1);
      const updated: MachineItem = {
        ...item,
        status: newAvailable > 0 ? 'DISPONIBLE' : 'SIN_STOCK',
        availableStock: newAvailable
      };
      this.inventory.update(list => list.map(m => (m.id === item.id ? updated : m)));
      this.saveInventoryToStorage();
      this.showToast('success', 'Equipo Reintegrado', `${item.name} volvió a servicio en patio (Stock: ${newAvailable}).`);
    } else {
      // Enviar a mantención
      const newAvailable = Math.max(0, item.availableStock - 1);
      const updated: MachineItem = {
        ...item,
        status: 'EN_MANTENCION',
        availableStock: newAvailable
      };
      this.inventory.update(list => list.map(m => (m.id === item.id ? updated : m)));
      this.saveInventoryToStorage();
      this.showToast('warning', 'Equipo a Taller Técnico', `${item.name} enviado a mantención técnica preventiva.`);
    }
  }

  // Toggle Order Block
  public toggleOrderBlock(item: MachineItem): void {
    const nextBlocked = !item.isOrderBlocked;
    const updated: MachineItem = {
      ...item,
      isOrderBlocked: nextBlocked,
      status: nextBlocked ? 'SIN_STOCK' : (item.availableStock > 0 ? 'DISPONIBLE' : 'SIN_STOCK')
    };

    this.inventory.update(list => list.map(m => (m.id === item.id ? updated : m)));
    this.saveInventoryToStorage();
    this.showToast(
      nextBlocked ? 'warning' : 'success',
      nextBlocked ? 'Pedidos Bloqueados' : 'Pedidos Desbloqueados',
      nextBlocked
        ? `Se bloquearon nuevos arriendos para ${item.sku}.`
        : `Se reactivó la recepción de pedidos para ${item.sku}.`
    );
  }

  public confirmDeleteMachine(item: MachineItem): void {
    this.openConfirmModal(
      'Dar de Baja Equipo',
      `¿Deseas dar de baja del catálogo el equipo "${item.name}" (SKU: ${item.sku})? Se revocarán futuras reservas.`,
      () => {
        this.inventory.update(list => list.filter(m => m.id !== item.id));
        this.saveInventoryToStorage();
        this.showToast('success', 'Equipo dado de baja', `El equipo ${item.sku} fue removido del catálogo.`);
        this.apiService.deleteCatalogService(item.id).subscribe({ error: () => {} });
      }
    );
  }

  // ==========================================
  // TAB 3: VALIDACIÓN DE BODEGA LOGIC
  // ==========================================
  private initializeValidationOrders(): void {
    const cached = localStorage.getItem('smartrent_validation_orders');
    if (cached) {
      try {
        const parsed = JSON.parse(cached);
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.validationOrders.set(parsed);
          return;
        }
      } catch (e) {
        console.warn('Error reading cached validation orders');
      }
    }

    const defaultOrders: MachineValidationOrder[] = [
      {
        id: 1044,
        rentalCode: 'ARR-2026-1044',
        machineId: 1,
        machineName: 'Excavadora Oruga Cat 320',
        sku: 'CAT-320D',
        lesseeName: 'Constanza Valenzuela',
        company: 'Constructora Besalco S.A.',
        tenantId: 'TNT-BESALCO-03',
        workSiteAddress: 'Faena Autopista Vespucio Oriente Tramo II',
        startDate: '2026-10-20',
        endDate: '2026-10-27',
        status: 'EN_PREPARACION',
        horometerOut: 1420,
        notes: 'En preparación en taller de patio Pudahuel.'
      },
      {
        id: 1045,
        rentalCode: 'ARR-2026-1045',
        machineId: 2,
        machineName: 'Rodillo Compactador Bomag BW 211',
        sku: 'BOM-BW211',
        lesseeName: 'Daniela Soto Navarrete',
        company: 'Echeverría Izquierdo Ingeniería',
        tenantId: 'TNT-ECHEVERRIA-07',
        workSiteAddress: 'Faena Lo Boza Módulo B, Pudahuel',
        startDate: '2026-10-21',
        endDate: '2026-10-28',
        status: 'SOLICITADO',
        horometerOut: 890,
        notes: 'Solicitud aprobada comercialmente, pendiente inspección técnica de salida.'
      },
      {
        id: 1046,
        rentalCode: 'ARR-2026-1046',
        machineId: 3,
        machineName: 'Generador Diésel Cummins 50kVA Insonorizado',
        sku: 'CUM-50KVA',
        lesseeName: 'Carlos Mendoza Silva',
        company: 'Maquinarias Pudahuel Sur SpA',
        tenantId: 'TNT-PUDAHUEL-02',
        workSiteAddress: 'Faena Costanera Sur Módulo C',
        startDate: '2026-10-19',
        endDate: '2026-10-26',
        status: 'SOLICITADO',
        horometerOut: 650,
        notes: 'Generador solicitado para turno continuo.'
      },
      {
        id: 1042,
        rentalCode: 'ARR-2026-1042',
        machineId: 6,
        machineName: 'Miniexcavadora Terreno Komatsu PC35',
        sku: 'EXC-MINI35',
        lesseeName: 'Constanza Valenzuela',
        company: 'Constructora Besalco S.A.',
        tenantId: 'TNT-BESALCO-03',
        workSiteAddress: 'Faena Metro Línea 7 Estación Baquedano',
        startDate: '2026-10-10',
        endDate: '2026-10-22',
        status: 'EN_TERRENO',
        horometerOut: 1100,
        inspectedBy: 'Carlos Mendoza (Jefe de Bodega)',
        inspectionDate: '2026-10-10',
        checklistPassed: true,
        notes: 'Operando en faena de cliente.'
      },
      {
        id: 1043,
        rentalCode: 'ARR-2026-1043',
        machineId: 5,
        machineName: 'Sierra Circular DeWalt DWE575K 7-1/4"',
        sku: 'DEW-DWE575',
        lesseeName: 'Felipe Morales Tapia',
        company: 'Equipos Mineros Antofagasta Ltda',
        tenantId: 'TNT-ANTOF-04',
        workSiteAddress: 'Faena Minera Escondida Módulo 8',
        startDate: '2026-10-12',
        endDate: '2026-10-25',
        status: 'EN_TERRENO',
        horometerOut: 320,
        inspectedBy: 'Felipe Morales (Jefe de Bodega)',
        inspectionDate: '2026-10-12',
        checklistPassed: true,
        notes: 'Herramienta entregada con disco de tungsteno nuevo.'
      }
    ];

    this.validationOrders.set(defaultOrders);
    this.saveValidationOrdersToStorage();
  }

  private saveValidationOrdersToStorage(): void {
    localStorage.setItem('smartrent_validation_orders', JSON.stringify(this.validationOrders()));
  }

  public openInspectionModal(order: MachineValidationOrder): void {
    this.inspectingOrder.set(order);
    this.inspectionForm.reset({
      operatorName: 'Carlos Mendoza (Jefe de Bodega)',
      horometerOut: order.horometerOut || 1200,
      fluidsChecked: true,
      structureChecked: true,
      securityKitChecked: true,
      brakeEngineChecked: true,
      notes: `Inspección de pre-entrega para ${order.machineName} (${order.sku}). Niveles certificados y baliza de faena montada.`
    });
    this.isInspectionModalOpen.set(true);
  }

  public closeInspectionModal(): void {
    this.isInspectionModalOpen.set(false);
    this.inspectingOrder.set(null);
  }

  public approveAndDispatchOrder(): void {
    const order = this.inspectingOrder();
    if (!order) return;

    if (this.inspectionForm.invalid) {
      this.inspectionForm.markAllAsTouched();
      this.showToast('warning', 'Checklist Obligatorio', 'Debes validar los 4 puntos del checklist técnico preoperacional.');
      return;
    }

    // 1. Check stock of machine
    const mach = this.inventory().find(m => m.sku === order.sku || m.id === order.machineId);
    if (mach && mach.availableStock <= 0) {
      this.showToast('error', 'Stock Agotado', `El equipo ${mach.name} ya no tiene unidades disponibles en patio para despachar.`);
      return;
    }

    // 2. Decrement stock
    if (mach) {
      const nextStock = Math.max(0, mach.availableStock - 1);
      const updatedMach: MachineItem = {
        ...mach,
        availableStock: nextStock,
        status: nextStock === 0 ? 'SIN_STOCK' : mach.status
      };
      this.inventory.update(list => list.map(m => (m.id === mach.id ? updatedMach : m)));
      this.saveInventoryToStorage();
    }

    // 3. Update order to EN_TERRENO
    const formVal = this.inspectionForm.value;
    const updatedOrder: MachineValidationOrder = {
      ...order,
      status: 'EN_TERRENO',
      horometerOut: Number(formVal.horometerOut),
      inspectedBy: formVal.operatorName,
      inspectionDate: new Date().toISOString().split('T')[0],
      checklistPassed: true,
      notes: formVal.notes
    };

    this.validationOrders.update(list => list.map(o => (o.id === order.id ? updatedOrder : o)));
    this.saveValidationOrdersToStorage();
    this.closeInspectionModal();

    this.showToast(
      'success',
      'Maquinaria Validada y Despachada',
      `El Jefe de Bodega validó y despachó a faena ${order.machineName} (Orden ${order.rentalCode}). Stock en patio: ${mach ? mach.availableStock - 1 : 0} un.`
    );

    // Call update rental status
    this.apiService.updateRentalStatus(order.id, 'EN_TERRENO').subscribe({ error: () => {} });
  }

  public rejectOrderForMaintenance(): void {
    const order = this.inspectingOrder();
    if (!order) return;

    // Send machine to maintenance
    const mach = this.inventory().find(m => m.sku === order.sku || m.id === order.machineId);
    if (mach) {
      const updatedMach: MachineItem = {
        ...mach,
        status: 'EN_MANTENCION',
        availableStock: Math.max(0, mach.availableStock - 1)
      };
      this.inventory.update(list => list.map(m => (m.id === mach.id ? updatedMach : m)));
      this.saveInventoryToStorage();
    }

    const updatedOrder: MachineValidationOrder = {
      ...order,
      status: 'EN_MANTENCION',
      notes: 'Rechazado por Jefe de Bodega en inspección: fallas mecánicas detectadas. Requiere reasignación de equipo.'
    };

    this.validationOrders.update(list => list.map(o => (o.id === order.id ? updatedOrder : o)));
    this.saveValidationOrdersToStorage();
    this.closeInspectionModal();

    this.showToast(
      'warning',
      'Despacho Rechazado - A Taller',
      `El equipo ${order.machineName} no superó la inspección técnica de bodega y fue derivado a mantenimiento.`
    );
  }

  public receiveAndReturnMachine(order: MachineValidationOrder): void {
    this.openConfirmModal(
      'Recepción y Devolución de Faena',
      `¿Confirmas la recepción en patio del equipo "${order.machineName}" (${order.sku}) devuelto de la faena "${order.workSiteAddress}"? Se reincorporará 1 unidad al stock disponible.`,
      () => {
        // 1. Increment machine availableStock
        const mach = this.inventory().find(m => m.sku === order.sku || m.id === order.machineId);
        if (mach) {
          const nextStock = Math.min(mach.totalStock, mach.availableStock + 1);
          const updatedMach: MachineItem = {
            ...mach,
            availableStock: nextStock,
            status: nextStock > 0 ? 'DISPONIBLE' : mach.status
          };
          this.inventory.update(list => list.map(m => (m.id === mach.id ? updatedMach : m)));
          this.saveInventoryToStorage();
        }

        // 2. Update order to DEVUELTO
        const updatedOrder: MachineValidationOrder = {
          ...order,
          status: 'DEVUELTO',
          notes: 'Equipo retornado de faena, inspeccionado sin daños estructurales y reingresado al stock de patio.'
        };
        this.validationOrders.update(list => list.map(o => (o.id === order.id ? updatedOrder : o)));
        this.saveValidationOrdersToStorage();

        this.showToast(
          'success',
          'Equipo Devuelto a Stock',
          `Se recepcionó ${order.machineName}. Stock disponible incrementado a ${mach ? mach.availableStock + 1 : 1} unidades.`
        );

        this.apiService.updateRentalStatus(order.id, 'DEVUELTO').subscribe({ error: () => {} });
      }
    );
  }

  // ==========================================
  // KPIS & REPORT LOGIC
  // ==========================================
  private initializeKpis(): void {
    this.loadKpisForRange(this.kpiRange());
  }

  public setKpiRange(range: 'last24h' | 'last7d' | 'last30d'): void {
    this.kpiRange.set(range);
    this.loadKpisForRange(range);
  }

  private loadKpisForRange(range: 'last24h' | 'last7d' | 'last30d'): void {
    if (range === 'last24h') {
      this.kpiMetrics.set({
        dailyRentals: 28,
        dailyRentalsComparison: '+14% vs día anterior',
        avgCycleHours: 3.4,
        avgCycleDesc: 'Solicitado -> Aprobado -> Terreno',
        activeMachines: 142,
        totalFleet: 180,
        occupancyPercent: 78.9,
        activeDailyRevenueClp: 14850000,
        activeDailyRevenueDesc: 'Facturación neta diaria vigente'
      });
    } else if (range === 'last7d') {
      this.kpiMetrics.set({
        dailyRentals: 184,
        dailyRentalsComparison: '+9.3% vs semana anterior',
        avgCycleHours: 3.8,
        avgCycleDesc: 'Promedio acumulado de 7 días',
        activeMachines: 149,
        totalFleet: 180,
        occupancyPercent: 82.8,
        activeDailyRevenueClp: 98450000,
        activeDailyRevenueDesc: 'Facturación semanal acumulada'
      });
    } else {
      this.kpiMetrics.set({
        dailyRentals: 712,
        dailyRentalsComparison: '+16.5% vs mes anterior',
        avgCycleHours: 4.1,
        avgCycleDesc: 'Promedio mensual de ciclo',
        activeMachines: 156,
        totalFleet: 180,
        occupancyPercent: 86.7,
        activeDailyRevenueClp: 382400000,
        activeDailyRevenueDesc: 'Facturación mensual consolidada'
      });
    }

    this.topServices.set([
      {
        sku: 'CAT-320D',
        name: 'Excavadora Oruga Cat 320',
        category: 'Movimiento de Tierra',
        rentalsCount: range === 'last24h' ? 8 : range === 'last7d' ? 36 : 142,
        occupancyPercent: 92,
        revenueClp: range === 'last24h' ? 1200000 : range === 'last7d' ? 5400000 : 21300000,
        imageUrl: '/images/cat-320.jpg'
      },
      {
        sku: 'BOM-BW211',
        name: 'Rodillo Compactador Bomag BW 211',
        category: 'Compactación',
        rentalsCount: range === 'last24h' ? 6 : range === 'last7d' ? 28 : 110,
        occupancyPercent: 85,
        revenueClp: range === 'last24h' ? 510000 : range === 'last7d' ? 2380000 : 9350000,
        imageUrl: '/images/bomag-bw211.jpg'
      },
      {
        sku: 'CUM-50KVA',
        name: 'Generador Diésel Cummins 50kVA',
        category: 'Generación',
        rentalsCount: range === 'last24h' ? 5 : range === 'last7d' ? 25 : 98,
        occupancyPercent: 88,
        revenueClp: range === 'last24h' ? 300000 : range === 'last7d' ? 1500000 : 5880000,
        imageUrl: '/images/cummins-50kva.jpg'
      },
      {
        sku: 'BOS-GSH11E',
        name: 'Demoledor Bosch GSH 11 E',
        category: 'Herramientas',
        rentalsCount: range === 'last24h' ? 4 : range === 'last7d' ? 19 : 78,
        occupancyPercent: 76,
        revenueClp: range === 'last24h' ? 100000 : range === 'last7d' ? 475000 : 1950000,
        imageUrl: '/images/bosch-gsh11e.jpg'
      },
      {
        sku: 'DEW-DWE575',
        name: 'Sierra Circular DeWalt DWE575',
        category: 'Herramientas',
        rentalsCount: range === 'last24h' ? 3 : range === 'last7d' ? 15 : 62,
        occupancyPercent: 68,
        revenueClp: range === 'last24h' ? 36000 : range === 'last7d' ? 180000 : 744000,
        imageUrl: '/images/dewalt-dwe575.jpg'
      }
    ]);

    const totalOrders = range === 'last24h' ? 28 : range === 'last7d' ? 184 : 712;
    this.rentalStatusDistribution.set([
      {
        status: 'SOLICITADO',
        label: 'Solicitado',
        count: Math.round(totalOrders * 0.14),
        percentage: 14,
        color: '#64748b',
        description: 'Esperando validación comercial y garantía'
      },
      {
        status: 'APROBADO',
        label: 'Aprobado',
        count: Math.round(totalOrders * 0.18),
        percentage: 18,
        color: '#0284c7',
        description: 'Aprobación técnica y crediticia confirmada'
      },
      {
        status: 'EN_PREPARACION',
        label: 'En Preparación',
        count: Math.round(totalOrders * 0.11),
        percentage: 11,
        color: '#d97706',
        description: 'Alistamiento e inspección de bodega'
      },
      {
        status: 'EN_TERRENO',
        label: 'En Terreno',
        count: Math.round(totalOrders * 0.39),
        percentage: 39,
        color: '#16a34a',
        description: 'Operando activamente en faena de obra'
      },
      {
        status: 'DEVUELTO',
        label: 'Devuelto',
        count: Math.round(totalOrders * 0.15),
        percentage: 15,
        color: '#475569',
        description: 'Retornado a patio e inspeccionado'
      },
      {
        status: 'CANCELADO',
        label: 'Cancelado',
        count: Math.round(totalOrders * 0.03),
        percentage: 3,
        color: '#dc2626',
        description: 'Cancelado por el arrendatario o contingencia'
      }
    ]);

    this.apiService.getReportKpis(range).subscribe({
      next: () => {},
      error: () => {}
    });
  }

  // ==========================================
  // TAB 5: RABBITMQ TOPOLOGY LOGIC
  // ==========================================
  private initializeRabbitTopology(): void {
    const cachedQueues = localStorage.getItem('smartrent_rabbit_queues');
    const cachedExchanges = localStorage.getItem('smartrent_rabbit_exchanges');
    const cachedBindings = localStorage.getItem('smartrent_rabbit_bindings');

    if (cachedQueues && cachedExchanges && cachedBindings) {
      try {
        this.queues.set(JSON.parse(cachedQueues));
        this.exchanges.set(JSON.parse(cachedExchanges));
        this.bindings.set(JSON.parse(cachedBindings));
        return;
      } catch (e) {
        console.warn('Error reading cached rabbit topology');
      }
    }

    this.restoreBaseTopology(false);
  }

  private saveRabbitTopologyToStorage(): void {
    localStorage.setItem('smartrent_rabbit_queues', JSON.stringify(this.queues()));
    localStorage.setItem('smartrent_rabbit_exchanges', JSON.stringify(this.exchanges()));
    localStorage.setItem('smartrent_rabbit_bindings', JSON.stringify(this.bindings()));
  }

  public restoreBaseTopology(showNotification: boolean = true): void {
    const baseExchanges: RabbitExchange[] = [
      {
        name: 'cmd.direct',
        type: 'direct',
        durable: true,
        autoDelete: false,
        description: 'Enrutamiento directo y unívoco por comando operacional exacto.',
        isOfficial: true
      },
      {
        name: 'cmd.topic',
        type: 'topic',
        durable: true,
        autoDelete: false,
        description: 'Enrutamiento temático por patrones wildcard (*, #) para eventos y auditoría.',
        isOfficial: true
      },
      {
        name: 'cmd.dead.dlx',
        type: 'direct',
        durable: true,
        autoDelete: false,
        description: 'Dead Letter Exchange (DLX) centralizado para captura de fallos de procesamiento.',
        isOfficial: true
      }
    ];

    const baseQueues: RabbitQueue[] = [
      {
        name: 'q.cmd.email',
        type: 'standard',
        durable: true,
        autoDelete: false,
        dlqAssociated: 'q.cmd.email.dlq',
        purpose: 'Despacho de notificaciones y confirmaciones por correo al arrendatario.',
        messageCount: 0,
        consumerCount: 3,
        isOfficial: true
      },
      {
        name: 'q.cmd.prep',
        type: 'standard',
        durable: true,
        autoDelete: false,
        dlqAssociated: 'q.cmd.prep.dlq',
        purpose: 'Generación de ticket de alistamiento y despacho preoperacional en bodega.',
        messageCount: 2,
        consumerCount: 2,
        isOfficial: true
      },
      {
        name: 'q.cmd.contract',
        type: 'standard',
        durable: true,
        autoDelete: false,
        dlqAssociated: 'q.cmd.contract.dlq',
        purpose: 'Generación y firma digital del PDF de contrato y acta de entrega.',
        messageCount: 1,
        consumerCount: 2,
        isOfficial: true
      },
      {
        name: 'q.cmd.email.dlq',
        type: 'dlq',
        durable: true,
        autoDelete: false,
        purpose: 'Dead Letter Queue para contingencia de correos con rebote o fallo SMTP.',
        messageCount: 0,
        consumerCount: 1,
        isOfficial: true
      },
      {
        name: 'q.cmd.prep.dlq',
        type: 'dlq',
        durable: true,
        autoDelete: false,
        purpose: 'Dead Letter Queue para tickets de bodega no procesados o timeouts.',
        messageCount: 0,
        consumerCount: 1,
        isOfficial: true
      },
      {
        name: 'q.cmd.contract.dlq',
        type: 'dlq',
        durable: true,
        autoDelete: false,
        purpose: 'Dead Letter Queue para contingencia en generación de contratos PDF.',
        messageCount: 0,
        consumerCount: 1,
        isOfficial: true
      }
    ];

    const baseBindings: RabbitBinding[] = [
      { id: 'b-1', source: 'cmd.direct', destination: 'q.cmd.email', routingKey: 'email.send' },
      { id: 'b-2', source: 'cmd.topic', destination: 'q.cmd.email', routingKey: 'email.*' },
      { id: 'b-3', source: 'cmd.direct', destination: 'q.cmd.prep', routingKey: 'prep.ticket' },
      { id: 'b-4', source: 'cmd.topic', destination: 'q.cmd.prep', routingKey: 'prep.#' },
      { id: 'b-5', source: 'cmd.direct', destination: 'q.cmd.contract', routingKey: 'contract.gen' },
      { id: 'b-6', source: 'cmd.topic', destination: 'q.cmd.contract', routingKey: 'contract.*' },
      { id: 'b-7', source: 'cmd.dead.dlx', destination: 'q.cmd.email.dlq', routingKey: 'email.dead' },
      { id: 'b-8', source: 'cmd.dead.dlx', destination: 'q.cmd.prep.dlq', routingKey: 'prep.dead' },
      { id: 'b-9', source: 'cmd.dead.dlx', destination: 'q.cmd.contract.dlq', routingKey: 'contract.dead' }
    ];

    this.exchanges.set(baseExchanges);
    this.queues.set(baseQueues);
    this.bindings.set(baseBindings);
    this.saveRabbitTopologyToStorage();

    if (showNotification) {
      this.showToast(
        'success',
        'Topología Base Restaurada',
        'Se verificaron e inicializaron los 3 Exchanges y 6 Colas oficiales del caso SmartRent.'
      );
    }
  }

  // Rabbit operations
  public openCreateQueueModal(): void {
    this.queueForm.reset({
      name: 'q.cmd.',
      type: 'standard',
      durable: true,
      autoDelete: false,
      dlqAssociated: '',
      purpose: ''
    });
    this.isQueueModalOpen.set(true);
  }

  public closeQueueModal(): void {
    this.isQueueModalOpen.set(false);
  }

  public saveQueue(): void {
    if (this.queueForm.invalid) {
      this.queueForm.markAllAsTouched();
      this.showToast(
        'error',
        'Validación RabbitMQ',
        'El nombre de la cola es obligatorio, sin espacios y solo caracteres alfanuméricos, puntos o guiones.'
      );
      return;
    }

    const { name, type, durable, autoDelete, dlqAssociated, purpose } = this.queueForm.value;
    const cleanName = name.trim();

    if (this.queues().some(q => q.name.toLowerCase() === cleanName.toLowerCase())) {
      this.showToast('error', 'Cola Existente', `Ya existe una cola registrada con el nombre "${cleanName}".`);
      return;
    }

    const newQueue: RabbitQueue = {
      name: cleanName,
      type,
      durable: !!durable,
      autoDelete: !!autoDelete,
      dlqAssociated: dlqAssociated?.trim() || undefined,
      purpose: purpose.trim(),
      messageCount: 0,
      consumerCount: 0,
      isOfficial: false
    };

    this.queues.update(list => [...list, newQueue]);
    this.saveRabbitTopologyToStorage();
    this.closeQueueModal();
    this.showToast('success', 'Cola Creada', `Cola ${cleanName} creada exitosamente en RabbitMQ.`);

    this.apiService.createRabbitQueue(newQueue).subscribe({ error: () => {} });
  }

  public confirmDeleteQueue(queue: RabbitQueue): void {
    this.openConfirmModal(
      'Eliminar Cola de RabbitMQ',
      `¿Deseas eliminar permanentemente la cola "${queue.name}"? Los mensajes no consumidos serán descartados.`,
      () => {
        this.queues.update(list => list.filter(q => q.name !== queue.name));
        this.bindings.update(list => list.filter(b => b.destination !== queue.name));
        this.saveRabbitTopologyToStorage();
        this.showToast('success', 'Cola Eliminada', `Se eliminó la cola ${queue.name}.`);
        this.apiService.deleteRabbitQueue(queue.name).subscribe({ error: () => {} });
      }
    );
  }

  public openCreateExchangeModal(): void {
    this.exchangeForm.reset({
      name: 'cmd.',
      type: 'direct',
      durable: true,
      autoDelete: false,
      description: ''
    });
    this.isExchangeModalOpen.set(true);
  }

  public closeExchangeModal(): void {
    this.isExchangeModalOpen.set(false);
  }

  public saveExchange(): void {
    if (this.exchangeForm.invalid) {
      this.exchangeForm.markAllAsTouched();
      this.showToast(
        'error',
        'Validación Exchange',
        'El nombre del exchange es obligatorio, sin espacios y con sintaxis válida.'
      );
      return;
    }

    const { name, type, durable, autoDelete, description } = this.exchangeForm.value;
    const cleanName = name.trim();

    if (this.exchanges().some(e => e.name.toLowerCase() === cleanName.toLowerCase())) {
      this.showToast('error', 'Exchange Existente', `Ya existe un exchange llamado "${cleanName}".`);
      return;
    }

    const newEx: RabbitExchange = {
      name: cleanName,
      type,
      durable: !!durable,
      autoDelete: !!autoDelete,
      description: description.trim(),
      isOfficial: false
    };

    this.exchanges.update(list => [...list, newEx]);
    this.saveRabbitTopologyToStorage();
    this.closeExchangeModal();
    this.showToast('success', 'Exchange Creado', `Exchange ${cleanName} (${type}) registrado.`);

    this.apiService.createRabbitExchange(newEx).subscribe({ error: () => {} });
  }

  public confirmDeleteExchange(exchange: RabbitExchange): void {
    this.openConfirmModal(
      'Eliminar Exchange',
      `¿Deseas eliminar el exchange "${exchange.name}"? Se desenrutarán los bindings conectados.`,
      () => {
        this.exchanges.update(list => list.filter(e => e.name !== exchange.name));
        this.bindings.update(list => list.filter(b => b.source !== exchange.name));
        this.saveRabbitTopologyToStorage();
        this.showToast('success', 'Exchange Eliminado', `Se eliminó el exchange ${exchange.name}.`);
        this.apiService.deleteRabbitExchange(exchange.name).subscribe({ error: () => {} });
      }
    );
  }

  public openCreateBindingModal(): void {
    const firstEx = this.exchanges()[0]?.name || 'cmd.direct';
    const firstQ = this.queues()[0]?.name || 'q.cmd.email';

    this.bindingForm.reset({
      source: firstEx,
      destination: firstQ,
      routingKey: ''
    });
    this.isBindingModalOpen.set(true);
  }

  public closeBindingModal(): void {
    this.isBindingModalOpen.set(false);
  }

  public saveBinding(): void {
    if (this.bindingForm.invalid) {
      this.bindingForm.markAllAsTouched();
      this.showToast(
        'error',
        'Routing Key Inválida',
        'Ingresa una routingKey válida (ej: "email.send", "prep.#", "contract.*").'
      );
      return;
    }

    const { source, destination, routingKey } = this.bindingForm.value;
    const cleanKey = routingKey.trim();

    const exists = this.bindings().some(
      b => b.source === source && b.destination === destination && b.routingKey === cleanKey
    );

    if (exists) {
      this.showToast('warning', 'Binding duplicado', 'Ya existe un enlace con esta misma routing key.');
      return;
    }

    const newBinding: RabbitBinding = {
      id: `b-${Date.now()}`,
      source,
      destination,
      routingKey: cleanKey
    };

    this.bindings.update(list => [...list, newBinding]);
    this.saveRabbitTopologyToStorage();
    this.closeBindingModal();
    this.showToast('success', 'Binding Creado', `Vinculado [${source}] -> (${cleanKey}) -> [${destination}].`);

    this.apiService.createRabbitBinding(newBinding).subscribe({ error: () => {} });
  }

  public deleteBinding(binding: RabbitBinding): void {
    this.bindings.update(list => list.filter(b => b.id !== binding.id));
    this.saveRabbitTopologyToStorage();
    this.showToast('info', 'Binding eliminado', `Se desvinculó ${binding.source} de ${binding.destination}.`);
    this.apiService.deleteRabbitBinding(binding).subscribe({ error: () => {} });
  }

  // ==========================================
  // CONFIRM MODAL HELPER
  // ==========================================
  public openConfirmModal(title: string, message: string, onConfirm: () => void): void {
    this.confirmActionData.set({ title, message, onConfirm });
    this.isConfirmModalOpen.set(true);
  }

  public closeConfirmModal(): void {
    this.isConfirmModalOpen.set(false);
    this.confirmActionData.set(null);
  }

  public executeConfirmAction(): void {
    const data = this.confirmActionData();
    if (data) {
      data.onConfirm();
    }
    this.closeConfirmModal();
  }

  // ==========================================
  // TOAST NOTIFICATIONS
  // ==========================================
  public showToast(type: 'success' | 'error' | 'warning' | 'info', title: string, message: string): void {
    const id = `toast-${Date.now()}-${Math.random()}`;
    const newToast: ToastNotification = { id, type, title, message };
    this.toasts.update(list => [...list, newToast]);

    setTimeout(() => {
      this.dismissToast(id);
    }, 4500);
  }

  public dismissToast(id: string): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  public formatClp(val: number): string {
    return '$' + (val || 0).toLocaleString('es-CL');
  }

  public getCategoryLabel(cat: MachineCategory | string): string {
    const map: Record<string, string> = {
      MOVIMIENTO_TIERRA: 'Movimiento de Tierra',
      COMPACTACION: 'Compactación',
      GENERACION: 'Generación y Energía',
      ELEVACION: 'Equipos de Elevación',
      HERRAMIENTAS: 'Herramientas Eléctricas',
      ACCESORIOS: 'Accesorios y Acoples'
    };
    return map[cat] || cat;
  }

  public getRoleBadge(role: string): { label: string; badgeClass: string } {
    switch (role) {
      case 'ADMIN':
        return { label: 'Admin', badgeClass: 'role-admin' };
      case 'OPERADOR':
        return { label: 'Jefe de bodega (Operador)', badgeClass: 'role-operador' };
      case 'CLIENTE':
        return { label: 'Arrendatario (Cliente)', badgeClass: 'role-cliente' };
      case 'AUDITOR':
        return { label: 'Auditor', badgeClass: 'role-auditor' };
      default:
        return { label: role, badgeClass: 'role-default' };
    }
  }

  public getTenantName(tenantId: string): string {
    const found = this.tenants.find(t => t.id === tenantId);
    return found ? found.name : tenantId;
  }
}
