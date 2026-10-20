import { Routes } from '@angular/router';
import { Home } from './home/home';

import { MsalGuard } from '@azure/msal-angular';

import { roleGuard } from './core/guards/role.guard';
import { catalogGuard } from './core/guards/catalog.guard';

export const routes: Routes = [
  { path: '', component: Home, pathMatch: 'full' },
  { path: 'about', loadComponent: () => import('./features/about/about').then(m => m.About) },
  { path: 'catalog', loadComponent: () => import('./features/catalog/catalog').then(m => m.Catalog), canActivate: [catalogGuard] },
  { path: 'rentals', loadComponent: () => import('./features/rentals/rentals').then(m => m.Rentals), canActivate: [MsalGuard, roleGuard('Arrendatario', 'JefeBodega')] },
  { path: 'cart', loadComponent: () => import('./features/cart/cart').then(m => m.Cart), canActivate: [MsalGuard, roleGuard('Arrendatario')] },
  { path: 'checkout', loadComponent: () => import('./features/checkout/checkout').then(m => m.CheckoutComponent), canActivate: [MsalGuard, roleGuard('Arrendatario')] },
  { path: 'reports', loadComponent: () => import('./features/reports/reports').then(m => m.Reports), canActivate: [MsalGuard, roleGuard('Administrador')] },
  { path: 'audit', loadComponent: () => import('./features/audit/audit').then(m => m.Audit), canActivate: [MsalGuard, roleGuard('Auditor')] },
  { path: 'admin', loadComponent: () => import('./features/admin/admin').then(m => m.AdminPanel), canActivate: [MsalGuard, roleGuard('Administrador')] },
  { path: '**', redirectTo: '' }
];
