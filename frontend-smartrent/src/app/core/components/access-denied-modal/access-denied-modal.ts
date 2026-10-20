import { Component, HostListener, inject } from '@angular/core';
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
  modalService = inject(AccessDeniedService);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.modalService.isOpen()) {
      this.modalService.confirmAndRedirect();
    }
  }

  onConfirm(): void {
    this.modalService.confirmAndRedirect();
  }
}
