import { Routes } from '@angular/router';
import { Home } from './home/home';
import { Dashboard } from './dashboard/dashboard';
import { MsalGuard } from '@azure/msal-angular';

export const routes: Routes = [
  { path: '', redirectTo: 'rentals', pathMatch: 'full' },
  { path: 'rentals', loadComponent: () => import('./features/rentals/rentals').then(m => m.Rentals), canActivate: [MsalGuard] },
  { path: 'catalog', loadComponent: () => import('./features/catalog/catalog').then(m => m.Catalog), canActivate: [MsalGuard] },
  { path: 'reports', loadComponent: () => import('./features/reports/reports').then(m => m.Reports), canActivate: [MsalGuard] },
  { path: 'audit', loadComponent: () => import('./features/audit/audit').then(m => m.Audit), canActivate: [MsalGuard] },
  { path: '**', redirectTo: 'rentals' }
];

