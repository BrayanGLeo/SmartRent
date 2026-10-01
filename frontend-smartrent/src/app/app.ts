import { Component, Inject, OnInit, OnDestroy, signal } from '@angular/core';
import { RouterOutlet, RouterModule } from '@angular/router';
import { MsalService, MsalBroadcastService, MSAL_GUARD_CONFIG, MsalGuardConfiguration } from '@azure/msal-angular';
import { InteractionStatus, RedirectRequest } from '@azure/msal-browser';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { CommonModule } from '@angular/common';import { NavbarComponent } from './core/components/navbar/navbar';
import { LoadingService } from './core/services/loading/loading.service';

@Component({
  imports: [RouterOutlet, RouterModule, CommonModule, NavbarComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit, OnDestroy {
  title = 'frontend-smartrent';
  isIframe = false;
  loginDisplay = false;
  private readonly _destroying$ = new Subject<void>();

  constructor(
    @Inject(MSAL_GUARD_CONFIG) private msalGuardConfig: MsalGuardConfiguration,
    private authService: MsalService,
    private broadcastService: MsalBroadcastService,
    public loadingService: LoadingService
  ) {}

  ngOnInit(): void {
    this.isIframe = window !== window.parent && !window.opener;

    this.authService.handleRedirectObservable().subscribe({
      next: () => {
        // Limpia la basura de la URL (como el ?state=) que deja el logout
        if (window.location.search.includes('state=')) {
          window.history.replaceState({}, document.title, window.location.pathname);
        }
      },
      error: (err) => {
        console.error('Error de MSAL:', err);
        // Si hay error de state_mismatch, limpiamos la URL para poder volver a intentar
        window.history.replaceState({}, document.title, window.location.pathname);
      }
    });

    this.broadcastService.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None),
        takeUntil(this._destroying$)
      )
      .subscribe(() => {
        this.setLoginDisplay();
      });
  }

  setLoginDisplay() {
    this.loginDisplay = this.authService.instance.getAllAccounts().length > 0;
  }

  login() {
    if (this.msalGuardConfig.authRequest){
      this.authService.loginRedirect({...this.msalGuardConfig.authRequest} as RedirectRequest);
    } else {
      this.authService.loginRedirect();
    }
  }

  logout() {
    this.authService.logoutRedirect({
      postLogoutRedirectUri: window.location.origin
    });
  }

  ngOnDestroy(): void {
    this._destroying$.next(undefined);
    this._destroying$.complete();
  }
}
