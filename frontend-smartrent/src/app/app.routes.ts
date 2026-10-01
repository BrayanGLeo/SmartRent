import { Routes } from '@angular/router';
import { Home } from './home/home';
import { Dashboard } from './dashboard/dashboard';
import { MsalGuard } from '@azure/msal-angular';

import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', component: Home, pathMatch: 'full' },
  { path: 'about', loadComponent: () => import('./features/about/about').then(m => m.About) },
  { path: 'rentals', loadComponent: () => import('./features/rentals/rentals').then(m => m.Rentals), canActivate: [MsalGuard] },
  { path: 'catalog', loadComponent: () => import('./features/catalog/catalog').then(m => m.Catalog) },
  { path: 'reports', loadComponent: () => import('./features/reports/reports').then(m => m.Reports), canActivate: [MsalGuard] },
  { path: 'audit', loadComponent: () => import('./features/audit/audit').then(m => m.Audit), canActivate: [MsalGuard] },
  { path: 'admin', loadComponent: () => import('./features/admin/admin').then(m => m.AdminPanel), canActivate: [MsalGuard, adminGuard] },
  { path: '**', redirectTo: '' }
];

