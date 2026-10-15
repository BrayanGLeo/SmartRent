/**
 * Modelos de Dominio: Auditoría y Trazabilidad de Eventos
 * Alineado con microservicio: ms-smartrent-audit
 * DB Table: AUDIT_TIMELINE (Event-driven vía RabbitMQ / Kafka)
 */

export interface AuditTimeline {
  id: number;
  eventType: string;
  eventPayload: string;
  timestamp: string; // ISO 8601 string
  // Metadatos parseados en cliente
  parsedPayload?: Record<string, unknown> | null;
  originService?: 'ms-smartrent-catalog' | 'ms-smartrent-cart' | 'ms-smartrent-checkout' | 'ms-smartrent-rentals' | 'ms-smartrent-notify' | string;
  severity?: 'INFO' | 'WARN' | 'ERROR' | 'AUDIT';
}

export interface AuditEvent {
  id: string | number;
  timestamp: string;
  user?: string;
  action: string;
  details: string;
  eventType?: string;
  correlationId?: string;
}

export interface AuditEventDetail {
  eventType: string;
  service: string;
  topic: string;
  entityId?: string | number;
  userId?: string;
  timestamp: string;
  data: Record<string, unknown>;
}
