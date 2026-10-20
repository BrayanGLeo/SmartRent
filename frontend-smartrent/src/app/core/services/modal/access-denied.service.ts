import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AccessDeniedService {
  private readonly router = inject(Router);

  public readonly isOpen = signal<boolean>(false);
  public readonly title = signal<string>('Acceso No Autorizado');
  public readonly message = signal<string>('No tiene permiso para acceder a esta página.');
  public readonly redirectUrl = signal<string>('/');
  public readonly secondsRemaining = signal<number>(4);

  private timerId: any = null;

  /**
   * Muestra el modal de acceso denegado y programa la redirección automática al home
   */
  public show(
    message: string = 'No tiene permiso para acceder a esta página.',
    redirectUrl: string = '/',
    title: string = 'Acceso No Autorizado'
  ): void {
    this.clearTimer();

    this.title.set(title);
    this.message.set(message);
    this.redirectUrl.set(redirectUrl);
    this.secondsRemaining.set(4);
    this.isOpen.set(true);

    this.timerId = setInterval(() => {
      const current = this.secondsRemaining();
      if (current <= 1) {
        this.confirmAndRedirect();
      } else {
        this.secondsRemaining.set(current - 1);
      }
    }, 1000);
  }

  /**
   * Cierra el modal y redirige de inmediato a la ruta establecida
   */
  public confirmAndRedirect(): void {
    this.clearTimer();
    this.isOpen.set(false);
    const target = this.redirectUrl() || '/';
    void this.router.navigate([target]);
  }

  private clearTimer(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
