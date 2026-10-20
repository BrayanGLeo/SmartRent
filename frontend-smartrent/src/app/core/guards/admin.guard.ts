import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';
import { AccessDeniedService } from '../services/modal/access-denied.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const accessDeniedService = inject(AccessDeniedService);
  const router = inject(Router);

  if (authService.userRole() === 'Administrador') {
    return true;
  }

  accessDeniedService.show('No tiene permiso para acceder a esta página.', '/');
  return router.createUrlTree(['/']);
};
