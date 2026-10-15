/**
 * Modelos de Dominio: Reportes Operativos y Financieros
 * Alineado con microservicio: ms-smartrent-report
 */

export interface KpiData {
  rentalsPerDay: number[];
  labels: string[];
  totalRentalsPeriod?: number;
  activeRentalsCount?: number;
  totalRevenueClp?: number;
  averageRentalDurationDays?: number;
}

export interface FleetUtilizationMetric {
  category: string;
  totalUnits: number;
  activeUnits: number;
  utilizationRatePercent: number; // ej: 78.5%
  revenueGeneratedClp: number;
}

export interface FinancialSummary {
  grossRevenueClp: number;
  ivaClp: number;
  netRevenueClp: number;
  guaranteesHeldClp: number;
  overdueRentalsCount: number;
}
