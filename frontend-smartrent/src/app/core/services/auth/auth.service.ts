import { Injectable, computed, inject, signal } from '@angular/core';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { AccountInfo, InteractionStatus } from '@azure/msal-browser';

export type UserRole = 'Administrador' | 'JefeBodega' | 'Arrendatario' | 'Auditor' | 'None';

/**
 * Función pura para extraer el rol del usuario desde sus tokens y claims de MSAL.
 * Se puede invocar directamente en guards o servicios sin instanciar dependencias circulares.
 */
export function getUserRoleFromAccount(currentAccount: AccountInfo | null): UserRole {
  if (!currentAccount) {
    return 'None';
  }

  const claims = (currentAccount.idTokenClaims ?? {}) as any;
  const rawRoles = claims.roles ?? claims.role ?? claims.extension_Role ?? claims.groups ?? [];
  const rolesList: string[] = Array.isArray(rawRoles) ? rawRoles : [rawRoles];

  const hasRole = (name: string) =>
    rolesList.some(r => typeof r === 'string' && r.toLowerCase() === name.toLowerCase());

  const username = (currentAccount.username ?? '').toLowerCase();
  const displayName = (currentAccount.name ?? '').toLowerCase();

  if (
    hasRole('ROLE_ADMIN') ||
    hasRole('Administrador') ||
    hasRole('Admin') ||
    username.includes('admin') ||
    displayName.includes('admin')
  ) {
    return 'Administrador';
  }
  if (
    hasRole('ROLE_AUDITOR') ||
    hasRole('Auditor') ||
    username.includes('auditor') ||
    displayName.includes('auditor')
  ) {
    return 'Auditor';
  }
  if (
    hasRole('ROLE_JEFEBODEGA') ||
    hasRole('JefeBodega') ||
    hasRole('Operador') ||
    hasRole('Bodega') ||
    username.includes('bodega') ||
    displayName.includes('bodega')
  ) {
    return 'JefeBodega';
  }
  if (hasRole('ROLE_ARRENDATARIO') || hasRole('Arrendatario') || hasRole('Cliente')) {
    return 'Arrendatario';
  }

  return 'None';
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly msalService = inject(MsalService);
  private readonly broadcastService = inject(MsalBroadcastService);

  private readonly accountSignal = signal<AccountInfo | null>(this.getActiveAccount());
  public readonly isAuthBusy = signal<boolean>(false);

  constructor() {
    this.broadcastService.inProgress$.subscribe((status: InteractionStatus) => {
      this.isAuthBusy.set(status !== InteractionStatus.None);
      if (status === InteractionStatus.None) {
        this.accountSignal.set(this.getActiveAccount());
      }
    });
  }

  public getActiveAccount(): AccountInfo | null {
    const accounts = this.msalService.instance.getAllAccounts();
    return accounts.length > 0 ? accounts[0] : null;
  }

  public readonly account = computed(() => this.accountSignal());

  // Computed signal to determine the user's role from JWT claims
  public readonly userRole = computed<UserRole>(() => {
    return getUserRoleFromAccount(this.account());
  });

  public login() {
    this.msalService.loginRedirect();
  }

  public logout() {
    this.msalService.logoutRedirect();
  }
}
