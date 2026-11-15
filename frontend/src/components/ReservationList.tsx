import React from 'react';
import { Reservation } from '../types/reservation';
import { Clock, CheckCircle2, XCircle, Ban } from 'lucide-react';

interface ReservationListProps {
  reservations: Reservation[];
  onCancel: (id: number) => void;
}

export const ReservationList: React.FC<ReservationListProps> = ({ reservations, onCancel }) => {
  if (reservations.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-8 text-center text-slate-500">
        <p className="text-sm">No hay reservas registradas en la sesión actual.</p>
      </div>
    );
  }

  const renderStatusBadge = (status: Reservation['status']) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>CONFIRMED</span>
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-medium bg-rose-100 text-rose-800">
            <XCircle className="w-3.5 h-3.5" />
            <span>CANCELLED</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-medium bg-amber-100 text-amber-800">
            <Clock className="w-3.5 h-3.5" />
            <span>PENDING</span>
          </span>
        );
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      <div className="p-4 border-b border-slate-100 flex justify-between items-center">
        <h3 className="font-semibold text-slate-800 text-sm">Reservas Recientes (Persistencia RDS)</h3>
        <span className="text-xs text-slate-500 font-mono">Total: {reservations.length}</span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-slate-600">
          <thead className="bg-slate-50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-200">
            <tr>
              <th className="px-4 py-3">ID / Version</th>
              <th className="px-4 py-3">Huésped</th>
              <th className="px-4 py-3">Habitación</th>
              <th className="px-4 py-3">Fechas</th>
              <th className="px-4 py-3">Total</th>
              <th className="px-4 py-3">Estado</th>
              <th className="px-4 py-3 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {reservations.map((r) => (
              <tr key={r.id} className="hover:bg-slate-50/60 transition-colors">
                <td className="px-4 py-3 font-mono font-medium text-slate-900">
                  #{r.id} <span className="text-xs text-slate-400 font-normal">(v{r.version ?? 0})</span>
                </td>
                <td className="px-4 py-3">
                  <div className="font-medium text-slate-900">{r.guestName}</div>
                  <div className="text-xs text-slate-400">{r.guestEmail}</div>
                </td>
                <td className="px-4 py-3">
                  <span className="font-medium text-slate-800">Hab #{r.roomId}</span>
                </td>
                <td className="px-4 py-3 text-xs">
                  <div>{r.checkInDate} al</div>
                  <div className="text-slate-500">{r.checkOutDate}</div>
                </td>
                <td className="px-4 py-3 font-medium text-slate-900">
                  ${r.totalPrice.toFixed(2)}
                </td>
                <td className="px-4 py-3">{renderStatusBadge(r.status)}</td>
                <td className="px-4 py-3 text-right">
                  {r.status !== 'CANCELLED' && (
                    <button
                      onClick={() => onCancel(r.id)}
                      className="inline-flex items-center space-x-1 text-xs text-rose-600 hover:text-rose-800 font-medium px-2 py-1 rounded hover:bg-rose-50 transition-colors"
                      title="Cancelar Reserva"
                    >
                      <Ban className="w-3.5 h-3.5" />
                      <span>Cancelar</span>
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

