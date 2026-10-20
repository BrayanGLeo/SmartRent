import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';

/**
 * Guard para la ruta /catalog:
 * Permite acceso exclusivo a:
 * 1. Personas no autenticadas (público general / invitados)
 * 2. Arrendatarios (clientes)
 *
 * Bloquea al Administrador (redirigiéndolo a su propio panel en /admin)
 * y a otros roles de gestión interna.
 */
export const catalogGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Si no ha iniciado sesión, puede navegar libremente por el catálogo
  if (!authService.account()) {
    return true;
  }

  const role = authService.userRole();

  // El Administrador no debe ver el catálogo de clientes; tiene su panel de gestión
  if (role === 'Administrador') {
    return router.createUrlTree(['/admin']);
  }

  // Si es Arrendatario o usuario estándar, se permite
  if (role === 'Arrendatario' || role === 'None') {
    return true;
  }

  // Otros roles internos (JefeBodega, Auditor) se redirigen al home
  return router.createUrlTree(['/']);
};
