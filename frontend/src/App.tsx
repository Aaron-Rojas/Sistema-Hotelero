import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { ReservationForm } from './components/ReservationForm';
import { ReservationList } from './components/ReservationList';
import { Reservation } from './types/reservation';
import { reservationApi } from './services/api';
import { Layers, Database, Cpu, ArrowUpRight } from 'lucide-react';

export const App: React.FC = () => {
  const [reservations, setReservations] = useState<Reservation[]>([]);

  const handleReservationCreated = (newReservation: Reservation) => {
    setReservations((prev) => [newReservation, ...prev]);
  };

  const handleCancelReservation = async (id: number) => {
    try {
      const updated = await reservationApi.cancelReservation(id);
      setReservations((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      console.error('Failed to cancel reservation', err);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Banner Arquitectónico */}
        <section className="bg-gradient-to-r from-slate-900 via-slate-800 to-emerald-950 text-white rounded-2xl p-6 sm:p-8 shadow-md">
          <div className="max-w-3xl">
            <span className="inline-block px-3 py-1 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-full text-xs font-semibold uppercase tracking-wider mb-3">
              Fase 1: Stack Tecnológico Seleccionado (Java 21 + Spring Boot 3.3 &amp; React)
            </span>
            <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              Arquitectura Cloud-Native Desacoplada
            </h2>
            <p className="mt-2 text-slate-300 text-sm sm:text-base leading-relaxed">
              Sistema de alta disponibilidad diseñado para prevenir condiciones de carrera (overbooking)
              mediante bloqueo optimista en Amazon RDS y amortiguar eventos mediante colas Amazon SQS.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6 pt-6 border-t border-slate-700/60">
            <div className="flex items-center space-x-3 text-slate-300 text-xs">
              <Cpu className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>Spring Boot 3.3 Stateless Controller</span>
            </div>
            <div className="flex items-center space-x-3 text-slate-300 text-xs">
              <Database className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>PostgreSQL 15 Multi-AZ con HikariCP</span>
            </div>
            <div className="flex items-center space-x-3 text-slate-300 text-xs">
              <Layers className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>Amazon SQS Decoupled Processing</span>
            </div>
          </div>
        </section>

        {/* Dashboard Central de Reservas */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <div className="lg:col-span-6 space-y-6">
            <ReservationForm onReservationCreated={handleReservationCreated} />

            <div className="bg-white p-5 rounded-xl border border-slate-200 text-xs text-slate-600 space-y-2">
              <div className="font-semibold text-slate-800 flex items-center justify-between">
                <span>Contrato OpenAPI &amp; Documentación Swagger</span>
                <ArrowUpRight className="w-4 h-4 text-emerald-600" />
              </div>
              <p>
                La API expone su documentación interactiva en{' '}
                <a
                  href="http://localhost:8080/swagger-ui.html"
                  target="_blank"
                  rel="noreferrer"
                  className="text-emerald-600 font-mono hover:underline"
                >
                  http://localhost:8080/swagger-ui.html
                </a>
              </p>
            </div>
          </div>

          <div className="lg:col-span-6">
            <ReservationList
              reservations={reservations}
              onCancel={handleCancelReservation}
            />
          </div>
        </div>
      </main>

      <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500">
        <p>Sistema de Reservas Hoteleras Cloud-Native — Monorepositorio Arquitectónico</p>
      </footer>
    </div>
  );
};

