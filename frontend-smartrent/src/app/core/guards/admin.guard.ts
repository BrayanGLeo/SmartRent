import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.userRole() === 'Administrador') {
    return true;
  }

  // Si no es administrador, redirigir al home o a una página de acceso denegado
  return router.createUrlTree(['/']);
};
