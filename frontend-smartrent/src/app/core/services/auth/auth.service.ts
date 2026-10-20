import { Injectable, computed, inject, signal } from '@angular/core';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { AccountInfo, InteractionStatus } from '@azure/msal-browser';
import { filter } from 'rxjs/operators';

export type UserRole = 'Administrador' | 'JefeBodega' | 'Arrendatario' | 'Auditor' | 'None';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private msalService = inject(MsalService);
  private broadcastService = inject(MsalBroadcastService);

  private accountSignal = signal<AccountInfo | null>(this.getActiveAccount());
  public isAuthBusy = signal<boolean>(false);

  constructor() {
    this.broadcastService.inProgress$.subscribe((status: InteractionStatus) => {
      this.isAuthBusy.set(status !== InteractionStatus.None);
      if (status === InteractionStatus.None) {
        this.accountSignal.set(this.getActiveAccount());
      }
    });
  }

  private getActiveAccount(): AccountInfo | null {
    const accounts = this.msalService.instance.getAllAccounts();
    return accounts.length > 0 ? accounts[0] : null;
  }

  public account = computed(() => this.accountSignal());

  // Computed signal to determine the user's role from JWT claims
  public userRole = computed<UserRole>(() => {
    const currentAccount = this.account();
    if (!currentAccount || !currentAccount.idTokenClaims) {
      return 'None';
    }

    const claims = currentAccount.idTokenClaims as any;
    const roles: string[] = claims.roles || [];

    const hasRole = (name: string) =>
      roles.some(r => typeof r === 'string' && r.toLowerCase() === name.toLowerCase());

    if (hasRole('ROLE_ADMIN') || hasRole('Administrador') || hasRole('Admin')) return 'Administrador';
    if (hasRole('ROLE_AUDITOR') || hasRole('Auditor')) return 'Auditor';
    if (hasRole('ROLE_JEFEBODEGA') || hasRole('JefeBodega') || hasRole('Operador') || hasRole('Bodega')) return 'JefeBodega';
    if (hasRole('ROLE_ARRENDATARIO') || hasRole('Arrendatario') || hasRole('Cliente')) return 'Arrendatario';

    return 'None';
  });

  public login() {
    this.msalService.loginRedirect();
  }

  public logout() {
    this.msalService.logoutRedirect();
  }
}
