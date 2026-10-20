import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { concatMap, map } from 'rxjs/operators';
import { getUserRoleFromAccount } from '../services/auth/auth.service';
import { AccessDeniedService } from '../services/modal/access-denied.service';

export const adminGuard: CanActivateFn = () => {
  const msalService = inject(MsalService);
  const accessDeniedService = inject(AccessDeniedService);
  const router = inject(Router);

  return msalService.initialize().pipe(
    concatMap(() => msalService.handleRedirectObservable()),
    map(() => {
      const accounts = msalService.instance.getAllAccounts();
      const role = getUserRoleFromAccount(accounts.length > 0 ? accounts[0] : null);

      if (role === 'Administrador') {
        return true;
      }

      setTimeout(() => {
        accessDeniedService.show('No tiene permiso para acceder a esta página.', '/');
      }, 100);

      return router.createUrlTree(['/']);
    })
  );
};
