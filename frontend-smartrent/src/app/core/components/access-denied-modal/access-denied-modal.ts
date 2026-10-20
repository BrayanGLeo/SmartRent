import { Component, HostListener, inject, ChangeDetectorRef, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccessDeniedService } from '../../services/modal/access-denied.service';

@Component({
  standalone: true,
  selector: 'app-access-denied-modal',
  imports: [CommonModule],
  templateUrl: './access-denied-modal.html',
  styleUrl: './access-denied-modal.css'
})
export class AccessDeniedModalComponent {
  readonly modalService = inject(AccessDeniedService);
  private readonly cdr = inject(ChangeDetectorRef);

  constructor() {
    effect(() => {
      this.modalService.isOpen();
      this.cdr.markForCheck();
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.modalService.isOpen()) {
      this.modalService.confirmAndRedirect();
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.onConfirm();
    }
  }

  onConfirm(): void {
    this.modalService.confirmAndRedirect();
  }
}
