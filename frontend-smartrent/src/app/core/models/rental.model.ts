/**
 * Modelos de Dominio: Gestión de Arriendos Operativos
 * Alineado con microservicio: ms-smartrent-rentals
 * DB Table: rentals
 */

import { Machine } from './catalog.model';

/**
 * Estados del ciclo de vida del arriendo:
 * - Backend nativo: SOLICITADO, APROBADO, EN_PREPARACION, EN_TERRENO, DEVUELTO, CANCELADO
 * - Alias/B2B: PENDING, ACTIVE, COMPLETED, CANCELLED, OVERDUE
 */
export type RentalStatus =
  | 'SOLICITADO'
  | 'APROBADO'
  | 'EN_PREPARACION'
  | 'EN_TERRENO'
  | 'DEVUELTO'
  | 'CANCELADO'
  | 'PENDING'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'OVERDUE';

export interface Rental {
  id: number | string;
  machineId: number | string;
  userId: string;
  startDate: string;
  endDate: string;
  status: RentalStatus | string;
  lesseeId?: string;
  // Metadatos enriquecidos en frontend / BFF
  machine?: Machine;
  machineName?: string;
  lesseeName?: string;
  dailyRate?: number;
  totalDays?: number;
  totalCost?: number;
  workSiteAddress?: string;
}

export interface RentalFilter {
  status?: RentalStatus | 'ALL';
  userId?: string;
  machineId?: number | string;
  startDateFrom?: string;
  endDateTo?: string;
}

export interface RentalStatusConfig {
  label: string;
  badgeClass: string;
  description: string;
}

export const RENTAL_STATUS_MAP: Record<string, RentalStatusConfig> = {
  SOLICITADO: {
    label: 'Solicitado',
    badgeClass: 'badge-pending',
    description: 'En espera de validación comercial y garantía',
  },
  PENDING: {
    label: 'Pendiente',
    badgeClass: 'badge-pending',
    description: 'En espera de confirmación',
  },
  APROBADO: {
    label: 'Aprobado',
    badgeClass: 'badge-info',
    description: 'Aprobado técnica y financieramente',
  },
  EN_PREPARACION: {
    label: 'En Taller / Alistamiento',
    badgeClass: 'badge-warning',
    description: 'Inspección pre-operacional en patio',
  },
  EN_TERRENO: {
    label: 'En Faena',
    badgeClass: 'badge-rented',
    description: 'Operando en faena de cliente',
  },
  ACTIVE: {
    label: 'Activo en Obra',
    badgeClass: 'badge-rented',
    description: 'En periodo de arriendo vigente',
  },
  DEVUELTO: {
    label: 'Devuelto / Inspeccionado',
    badgeClass: 'badge-available',
    description: 'Retornado al patio de origen sin observaciones',
  },
  COMPLETED: {
    label: 'Completado',
    badgeClass: 'badge-available',
    description: 'Ciclo de arriendo finalizado',
  },
  CANCELADO: {
    label: 'Cancelado',
    badgeClass: 'badge-danger',
    description: 'Orden anulada por cliente o bodega',
  },
  OVERDUE: {
    label: 'Atrasado / En Mora',
    badgeClass: 'badge-overdue',
    description: 'Excedió la fecha contractual de devolución',
  },
};
