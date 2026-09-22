import { Injectable, computed, inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';

export type UserRole = 'Admin' | 'JefeBodega' | 'Arrendatario' | 'Auditor' | 'None';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private msalService = inject(MsalService);

  // Computed signal to get the active account
  public account = computed<AccountInfo | null>(() => {
    const accounts = this.msalService.instance.getAllAccounts();
    return accounts.length > 0 ? accounts[0] : null;
  });

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
