/**
 * Tipos e interfaces de TypeScript para el dominio de reservas.
 * Mapeados estrictamente contra los contratos del Backend Spring Boot 3.
 */

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED';

export interface Reservation {
  id: number;
  roomId: number;
  guestName: string;
  guestEmail: string;
  checkInDate: string;
  checkOutDate: string;
  totalPrice: number;
  status: ReservationStatus;
  version: number;
  createdAt: string;
}

export interface CreateReservationRequest {
  roomId: number;
  guestName: string;
  guestEmail: string;
  checkInDate: string;
  checkOutDate: string;
  totalPrice: number;
}

/**
 * Estructura estándar RFC 7807 (Problem Details for HTTP APIs)
 */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  invalidParams?: Record<string, string>;
  timestamp?: string;
}

