import { Routes } from '@angular/router';
import { Home } from './home/home';
import { Dashboard } from './dashboard/dashboard';
import { MsalGuard } from '@azure/msal-angular';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'dashboard', component: Dashboard, canActivate: [MsalGuard] },
  { path: '**', redirectTo: '' }
];

