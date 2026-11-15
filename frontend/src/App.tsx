import React, { useState, useEffect, useRef } from 'react';
import { Navbar } from './components/Navbar';
import { ReservationForm } from './components/ReservationForm';
import { ReservationList } from './components/ReservationList';
import { Reservation } from './types/reservation';
import { reservationApi } from './services/api';
import { Layers, Database, Cpu, ArrowUpRight } from 'lucide-react';

export const App: React.FC = () => {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const pollAttemptsRef = useRef<Record<number, number>>({});

  // Polling CQRS Query: consulta el estado de las reservas PENDING cada 2s
  useEffect(() => {
    const pendingReservations = reservations.filter(
      (r) => r.status === 'PENDING' && (pollAttemptsRef.current[r.id] ?? 0) < 6
    );

    if (pendingReservations.length === 0) return;

    const intervalId = setInterval(async () => {
      for (const pending of pendingReservations) {
        const currentAttempt = (pollAttemptsRef.current[pending.id] ?? 0) + 1;
        pollAttemptsRef.current[pending.id] = currentAttempt;

        try {
          const fresh = await reservationApi.getReservationById(pending.id);
          if (fresh.status !== 'PENDING') {
            setReservations((prev) =>
              prev.map((item) => (item.id === fresh.id ? fresh : item))
            );
          }
        } catch (err) {
          console.error(`Error consultando estado de reserva #${pending.id}:`, err);
        }
      }
    }, 2000);

    return () => clearInterval(intervalId);
  }, [reservations]);

  const handleReservationCreated = (newReservation: Reservation) => {
    pollAttemptsRef.current[newReservation.id] = 0;
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
              Arquitectura CQRS + Event-Driven (Java 21 + Spring Boot 3.3 &amp; React)
            </span>
            <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              Sistema Hotelero Cloud-Native Desacoplado
            </h2>
            <p className="mt-2 text-slate-300 text-sm sm:text-base leading-relaxed">
              Segregación de comandos ACID con bloqueo pesimista en PostgreSQL, consultas ultrarrápidas
              en ElastiCache Redis (&lt; 5ms) y procesamiento asíncrono de pagos vía Amazon SQS.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6 pt-6 border-t border-slate-700/60">
            <div className="flex items-center space-x-3 text-slate-300 text-xs">
              <Cpu className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>CQRS Command / Query Stateless</span>
            </div>
            <div className="flex items-center space-x-3 text-slate-300 text-xs">
              <Database className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>PostgreSQL 15 + Redis Cache-Aside</span>
            </div>
            <div className="flex items-center space-x-3 text-slate-300 text-xs">
              <Layers className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>Amazon SQS + Saga Compensation</span>
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
        <p>Sistema de Reservas Hoteleras Cloud-Native — Patrón CQRS &amp; RFC 7807</p>
      </footer>
    </div>
  );
};