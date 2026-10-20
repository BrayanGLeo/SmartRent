import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';
import { AccessDeniedService } from '../services/modal/access-denied.service';

/**
 * Guard para la ruta /catalog:
 * Permite acceso exclusivo a:
 * 1. Personas no autenticadas (público general / invitados)
 * 2. Arrendatarios (clientes)
 *
 * Bloquea al Administrador (ya que tiene su propio panel de administración)
 * y a otros roles internos, mostrando un modal elegante y redirigiendo al home.
 */
export const catalogGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const accessDeniedService = inject(AccessDeniedService);
  const router = inject(Router);

  // Si no ha iniciado sesión, puede navegar libremente por el catálogo
  if (!authService.account()) {
    return true;
  }

  const role = authService.userRole();

  // El Administrador no debe acceder al catálogo de clientes
  if (role === 'Administrador') {
    accessDeniedService.show('No tiene permiso para acceder a esta página.', '/');
    return router.createUrlTree(['/']);
  }

  // Si es Arrendatario o usuario estándar, se permite
  if (role === 'Arrendatario' || role === 'None') {
    return true;
  }

  // Otros roles internos (JefeBodega, Auditor) se redirigen al home con mensaje
  accessDeniedService.show('No tiene permiso para acceder a esta página.', '/');
  return router.createUrlTree(['/']);
};
