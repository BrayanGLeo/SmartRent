import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, UserRole } from '../services/auth/auth.service';

/**
 * Fábrica de guards basados en roles.
 * Recibe un array de roles permitidos y retorna un CanActivateFn.
 * Uso: canActivate: [MsalGuard, roleGuard('Administrador', 'JefeBodega')]
 */
export function roleGuard(...allowedRoles: UserRole[]): CanActivateFn {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (allowedRoles.includes(authService.userRole())) {
      return true;
    }

    return router.createUrlTree(['/']);
  };
}
