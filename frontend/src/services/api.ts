import { CreateReservationRequest, Reservation, ProblemDetail } from '../types/reservation';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

/**
 * Cliente HTTP CQRS para comunicación stateless con el backend.
 * Intercepta errores 4xx/5xx y los transforma en ApiError según RFC 7807.
 */
export class ApiError extends Error {
  constructor(public problem: ProblemDetail) {
    super(problem.detail || problem.title);
    this.name = 'ApiError';
  }
}

export const reservationApi = {
  // LADO COMMAND (POST -> 202 Accepted / PENDING)
  async createReservation(data: CreateReservationRequest): Promise<Reservation> {
    const response = await fetch(`${API_BASE_URL}/reservations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const problem: ProblemDetail = await response.json();
      throw new ApiError(problem);
    }

    return response.json();
  },

  // LADO QUERY (GET -> Consulta por ID para Polling asíncrono)
  async getReservationById(id: number): Promise<Reservation> {
    const response = await fetch(`${API_BASE_URL}/reservations/${id}`);
    if (!response.ok) {
      const problem: ProblemDetail = await response.json();
      throw new ApiError(problem);
    }
    return response.json();
  },

  // LADO QUERY (GET -> Consulta rápida en caché Redis < 5ms)
  async checkAvailability(roomId: number, checkInDate: string, checkOutDate: string): Promise<boolean> {
    const params = new URLSearchParams({
      roomId: String(roomId),
      checkInDate,
      checkOutDate,
    });
    const response = await fetch(`${API_BASE_URL}/reservations/availability?${params.toString()}`);
    if (!response.ok) {
      const problem: ProblemDetail = await response.json();
      throw new ApiError(problem);
    }
    return response.json();
  },

  async getReservationsByGuestEmail(email: string): Promise<Reservation[]> {
    const response = await fetch(`${API_BASE_URL}/reservations?guestEmail=${encodeURIComponent(email)}`);
    if (!response.ok) {
      const problem: ProblemDetail = await response.json();
      throw new ApiError(problem);
    }
    return response.json();
  },

  async cancelReservation(id: number): Promise<Reservation> {
    const response = await fetch(`${API_BASE_URL}/reservations/${id}/cancel`, {
      method: 'PATCH',
    });
    if (!response.ok) {
      const problem: ProblemDetail = await response.json();
      throw new ApiError(problem);
    }
    return response.json();
  },
};