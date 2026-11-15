import { CreateReservationRequest, Reservation, ProblemDetail } from '../types/reservation';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

/**
 * Cliente HTTP para comunicación stateless con el backend.
 * 
 * ¿CÓMO maneja los errores?
 * Intercepta respuestas no exitosas (4xx y 5xx) y parsea el cuerpo
 * según la especificación RFC 7807 Problem Details emitida por Spring Boot.
 */
export class ApiError extends Error {
  constructor(public problem: ProblemDetail) {
    super(problem.detail || problem.title);
    this.name = 'ApiError';
  }
}

export const reservationApi = {
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

  async getReservationById(id: number): Promise<Reservation> {
    const response = await fetch(`${API_BASE_URL}/reservations/${id}`);
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

