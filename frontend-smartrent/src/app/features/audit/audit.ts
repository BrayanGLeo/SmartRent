import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, AuditEvent } from '../../core/services/api/api.service';

@Component({
  imports: [CommonModule],
  selector: 'app-audit',
  styleUrl: './audit.css',
  templateUrl: './audit.html',
})
export class Audit implements OnInit {
  apiService = inject(ApiService);
  events: AuditEvent[] = [];

  ngOnInit() {
    this.apiService.getAuditTimeline().subscribe(data => {
      this.events = data;
    });
  }
}
