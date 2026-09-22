import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { ApiService, KpiData } from '../../core/services/api/api.service';

@Component({
  imports: [CommonModule, BaseChartDirective],
  selector: 'app-reports',
  styleUrl: './reports.css',
  templateUrl: './reports.html',
})
export class Reports implements OnInit {
  apiService = inject(ApiService);

  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      { data: [], label: 'Arriendos por Día', backgroundColor: '#3498db' }
    ]
  };

  public barChartOptions: ChartOptions<'bar'> = {
    responsive: true,
  };

  ngOnInit() {
    this.apiService.getKpis().subscribe((data: KpiData) => {
      this.barChartData = {
        labels: data.labels,
        datasets: [
          { data: data.rentalsPerDay, label: 'Arriendos por Día', backgroundColor: '#3498db' }
        ]
      };
    });
  }
}
