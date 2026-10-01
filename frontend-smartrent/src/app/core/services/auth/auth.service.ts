import { Injectable, computed, inject, signal } from '@angular/core';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { AccountInfo, InteractionStatus } from '@azure/msal-browser';
import { filter } from 'rxjs/operators';

export type UserRole = 'Admin' | 'JefeBodega' | 'Arrendatario' | 'Auditor' | 'None';

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

    if (roles.includes('Admin')) return 'Admin';
    if (roles.includes('Auditor')) return 'Auditor';
    if (roles.includes('JefeBodega')) return 'JefeBodega';
    if (roles.includes('Arrendatario')) return 'Arrendatario';

    return 'None';
  });

  public login() {
    this.msalService.loginRedirect();
  }

  public logout() {
    this.msalService.logoutRedirect();
  }
}
