import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.userRole() === 'Admin') {
    return true;
  }

  // Si no es admin, redirigir al home o a una página de acceso denegado
  return router.createUrlTree(['/']);
};
