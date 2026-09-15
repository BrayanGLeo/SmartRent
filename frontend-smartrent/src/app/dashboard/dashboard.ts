import { Component, OnInit } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class Dashboard implements OnInit {
  userName: string = '';
  userEmail: string = '';
  userRoles: string[] = [];
  apiResponse: string = '';

  constructor(private authService: MsalService, private http: HttpClient) {}

  ngOnInit(): void {
    const activeAccount = this.authService.instance.getAllAccounts()[0];
    if (activeAccount) {
      this.userName = activeAccount.name || 'Usuario';
      this.userEmail = activeAccount.username || '';
      
      const claims: any = activeAccount.idTokenClaims;
      if (claims && claims.roles) {
        this.userRoles = claims.roles;
      }
    }
  }

  fetchRentals() {
    this.apiResponse = 'Cargando...';
    this.http.get(environment.apiBaseUrl + '/rentals').subscribe({
      next: (data) => {
        this.apiResponse = JSON.stringify(data, null, 2);
      },
      error: (error) => {
        console.error('Error fetching rentals', error);
        this.apiResponse = 'Error: ' + error.message + ' (Revisa la consola para más detalles y verifica la pestaña Network para ver el token).';
      }
    });
  }
}
