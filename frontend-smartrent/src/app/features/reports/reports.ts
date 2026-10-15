import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { ApiService, KpiData } from '../../core/services/api/api.service';
import { FleetUtilizationMetric } from '../../core/models';

@Component({
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  selector: 'app-reports',
  styleUrl: './reports.css',
  templateUrl: './reports.html',
})
export class Reports implements OnInit {
  apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);

  // KPIs Financieros y Operacionales
  kpiSummary = {
    grossRevenueClp: 184500000,
    ivaClp: 35055000,
    netRevenueClp: 149445000,
    activeFleetUnits: 28,
    totalFleetUnits: 36,
    fleetUtilizationRate: 77.8,
    guaranteesHeldClp: 48200000,
    maintenanceUnits: 4
  };

  // Métricas de Utilización por Familia
  fleetUtilization: FleetUtilizationMetric[] = [
    {
      category: 'Maquinaria Pesada (Excavación / Compactación)',
      totalUnits: 12,
      activeUnits: 10,
      utilizationRatePercent: 83.3,
      revenueGeneratedClp: 88500000
    },
    {
      category: 'Equipos de Elevación (JLG / Yale)',
      totalUnits: 8,
      activeUnits: 6,
      utilizationRatePercent: 75.0,
      revenueGeneratedClp: 42000000
    },
    {
      category: 'Generadores y Energía (Cummins / Sullair)',
      totalUnits: 10,
      activeUnits: 8,
      utilizationRatePercent: 80.0,
      revenueGeneratedClp: 36000000
    },
    {
      category: 'Herramientas Eléctricas & Demolición',
      totalUnits: 6,
      activeUnits: 4,
      utilizationRatePercent: 66.7,
      revenueGeneratedClp: 18000000
    }
  ];

  // Gráfico con colores industriales sobrios
  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'],
    datasets: [
      {
        data: [18, 24, 28, 26, 22, 14, 8],
        label: 'Unidades Activas en Faena',
        backgroundColor: '#D97706',
        borderColor: '#B45309',
        borderWidth: 1,
        borderRadius: 4
      },
      {
        data: [4, 3, 2, 3, 4, 2, 2],
        label: 'En Taller / Alistamiento',
        backgroundColor: '#475569',
        borderColor: '#334155',
        borderWidth: 1,
        borderRadius: 4
      }
    ]
  };

  public barChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        labels: {
          color: '#0F172A',
          font: { family: 'Inter', weight: 600, size: 12 }
        }
      }
    },
    scales: {
      x: {
        grid: { color: '#E2E8F0' },
        ticks: { color: '#475569', font: { family: 'Inter', size: 11 } }
      },
      y: {
        grid: { color: '#E2E8F0' },
        ticks: { color: '#475569', font: { family: 'Inter', size: 11 } }
      }
    }
  };

  ngOnInit() {
    this.apiService.getKpis().subscribe({
      next: (data: KpiData) => {
        if (data && data.labels && data.labels.length > 0) {
          this.barChartData = {
            labels: data.labels,
            datasets: [
              {
                data: data.rentalsPerDay,
                label: 'Arriendos Registrados',
                backgroundColor: '#D97706',
                borderColor: '#B45309',
                borderWidth: 1,
                borderRadius: 4
              }
            ]
          };
        }
        this.cdr.detectChanges();
      },
      error: () => {
        // Mantiene dataset industrial
        this.cdr.detectChanges();
      }
    });
  }
}
