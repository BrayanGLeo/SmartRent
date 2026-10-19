/**
 * Modelos de Dominio: Catálogo de Maquinarias y Equipos
 * Alineado con microservicio: ms-smartrent-catalog
 * DB Tables: categories, machines
 */

export interface Category {
  id: number;
  name: string;
  description: string;
  code?: string;
  icon?: string;
  isHeavyMachinery?: boolean;
}

export interface MachineSpecs {
  operatingWeightKg?: number;     // Peso operativo (kg)
  powerHp?: number;               // Potencia neta (HP / kW)
  loadCapacityKg?: number;        // Capacidad de carga (kg)
  maxReachMeters?: number;        // Altura / Alcance máximo (m)
  fuelType?: 'DIESEL' | 'BENCINA' | 'ELECTRICO_TRIFASICO' | 'BATERIA_ION_LITIO';
  bucketCapacityM3?: number;      // Capacidad de balde (m3)
  noiseLevelDb?: number;          // Nivel sonoro (dB)
  year?: number;                  // Año de fabricación
  brand?: string;                 // Marca (Caterpillar, John Deere, Bomag, etc.)
}

export type MachineAvailabilityStatus = 'DISPONIBLE' | 'ARRENDADO' | 'EN_MANTENCION' | 'BAJA_TECNICA';

export interface Machine {
  id: number | string;
  name: string;
  serialNumber?: string;
  category: Category | string;
  dailyPrice: number;
  isAvailable: boolean;
  status: string;               // UI status: 'DISPONIBLE' | 'ARRENDADO' | 'EN_MANTENCION'
  pricePerDay?: number;          // Alias de compatibilidad: dailyPrice
  description?: string;
  brand?: string;
  modelYear?: number;
  specs?: MachineSpecs;
  imageUrl?: string;
  warrantyDepositClp?: number;    // Monto de garantía de arriendo (CLP)
  locationDepot?: string;         // Sucursal / Patio de acopio (ej: "Bodega Pudahuel Norte")
  minRentalDays?: number;         // Días mínimos de arriendo
  sku?: string;                   // Código SKU de inventario
  totalStock?: number;            // Flota total registrada
  availableStock?: number;        // Unidades actualmente disponibles en patio
}

export interface MachineFilter {
  categoryId?: number | string;
  search?: string;
  minPrice?: number;
  maxPrice?: number;
  isAvailable?: boolean;
  startDate?: string;
  endDate?: string;
  brand?: string;
  sortBy?: 'price_asc' | 'price_desc' | 'name_asc' | 'popularity';
}
