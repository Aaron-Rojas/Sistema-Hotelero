import React, { useState } from 'react';
import { CreateReservationRequest, Reservation } from '../types/reservation';
import { reservationApi, ApiError } from '../services/api';
import { CalendarCheck, AlertTriangle, CheckCircle, Loader2 } from 'lucide-react';

interface ReservationFormProps {
  onReservationCreated: (reservation: Reservation) => void;
}

export const ReservationForm: React.FC<ReservationFormProps> = ({ onReservationCreated }) => {
  const [formData, setFormData] = useState<CreateReservationRequest>({
    roomId: 101,
    guestName: '',
    guestEmail: '',
    checkInDate: new Date().toISOString().split('T')[0],
    checkOutDate: new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0],
    totalPrice: 250.0,
  });

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const created = await reservationApi.createReservation(formData);
      setSuccessMessage(
        `Solicitud encolada (HTTP 202 Accepted). Reserva #${created.id} en estado ${created.status}: validando pago en segundo plano...`
      );
      onReservationCreated(created);
    } catch (err) {
      if (err instanceof ApiError) {
        const fieldErrors = err.problem.invalidParams
          ? ` (${Object.values(err.problem.invalidParams).join(', ')})`
          : '';
        setErrorMessage(`${err.problem.detail || err.problem.title}${fieldErrors}`);
      } else {
        setErrorMessage('Error de conexión con el backend de reservas.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
      <div className="flex items-center space-x-2 mb-4 pb-3 border-b border-slate-100">
        <CalendarCheck className="w-5 h-5 text-emerald-600" />
        <h2 className="text-base font-semibold text-slate-800">Nueva Solicitud de Reserva (CQRS Command)</h2>
      </div>

      {errorMessage && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-start space-x-2 text-sm text-red-700">
          <AlertTriangle className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
          <div>
            <span className="font-semibold">Conflicto / Validación (RFC 7807):</span> {errorMessage}
          </div>
        </div>
      )}

      {successMessage && (
        <div className="mb-4 p-3 bg-emerald-50 border border-emerald-200 rounded-lg flex items-start space-x-2 text-sm text-emerald-700">
          <CheckCircle className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
          <div>{successMessage}</div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Habitación (ID)
            </label>
            <select
              value={formData.roomId}
              onChange={(e) => setFormData({ ...formData, roomId: Number(e.target.value) })}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            >
              <option value={101}>Habitación 101 (Standard King)</option>
              <option value={102}>Habitación 102 (Deluxe Suite)</option>
              <option value={201}>Habitación 201 (Ocean View Executive)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Precio Total (USD)
            </label>
            <input
              type="number"
              step="0.01"
              value={formData.totalPrice}
              onChange={(e) => setFormData({ ...formData, totalPrice: parseFloat(e.target.value) })}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Nombre Completo del Huésped
            </label>
            <input
              type="text"
              placeholder="e.g. Carlos Mendoza"
              value={formData.guestName}
              onChange={(e) => setFormData({ ...formData, guestName: e.target.value })}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Correo Electrónico
            </label>
            <input
              type="email"
              placeholder="carlos.mendoza@example.com"
              value={formData.guestEmail}
              onChange={(e) => setFormData({ ...formData, guestEmail: e.target.value })}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Fecha Check-In
            </label>
            <input
              type="date"
              value={formData.checkInDate}
              onChange={(e) => setFormData({ ...formData, checkInDate: e.target.value })}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Fecha Check-Out
            </label>
            <input
              type="date"
              value={formData.checkOutDate}
              onChange={(e) => setFormData({ ...formData, checkOutDate: e.target.value })}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              required
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-medium py-2.5 px-4 rounded-lg flex items-center justify-center space-x-2 transition-colors shadow-sm"
        >
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Verificando disponibilidad y registrando...</span>
            </>
          ) : (
            <span>Confirmar y Encolar Reserva (SQS)</span>
          )}
        </button>
      </form>
    </div>
  );
};