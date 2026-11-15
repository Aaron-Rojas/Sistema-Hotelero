package com.hotel.reservas;

import com.hotel.reservas.dto.request.CreateReservationRequest;
import com.hotel.reservas.service.RedisCacheService;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "api"})
class ReservationConcurrencyAndContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private SqsTemplate sqsTemplate;

    @MockBean
    private RedisCacheService redisCacheService;

    @BeforeEach
    void setUp() {
        // Simula Cache Miss para forzar la contienda real en la base de datos
        when(redisCacheService.checkAvailabilityInCache(any(), any(), any())).thenReturn(null);
    }

    @Test
    @DisplayName("Lado Command: 10 reservas concurrentes -> 1 HTTP 202 y 9 HTTP 409 (0% Overbooking)")
    void shouldPreventOverbookingUnderHighConcurrency() throws Exception {
        int totalThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(12);

        List<Callable<ResponseEntity<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            final int index = i;
            tasks.add(() -> {
                CreateReservationRequest request = CreateReservationRequest.builder()
                        .roomId(101L)
                        .guestName("Huesped Concurrente " + index)
                        .guestEmail("huesped" + index + "@hotel.com")
                        .checkInDate(checkIn)
                        .checkOutDate(checkOut)
                        .totalPrice(new BigDecimal("250.00"))
                        .build();

                readyLatch.countDown();
                startLatch.await();
                return restTemplate.postForEntity("/api/v1/reservations", request, String.class);
            });
        }

        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (Callable<ResponseEntity<String>> task : tasks) {
            futures.add(executor.submit(task));
        }

        readyLatch.await();
        startLatch.countDown();

        long acceptedCount = 0;
        long conflictCount = 0;

        for (Future<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response = future.get();
            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                acceptedCount++;
            } else if (response.getStatusCode() == HttpStatus.CONFLICT) {
                conflictCount++;
            }
        }
        executor.shutdown();

        assertThat(acceptedCount).as("Exactamente 1 reserva debe ser aceptada (202)").isEqualTo(1);
        assertThat(conflictCount).as("Las otras 9 deben fallar por overbooking (409)").isEqualTo(9);
    }

    @Test
    @DisplayName("Contratos RFC 7807: Validación 400 Bad Request y Lado Query 404 Not Found")
    void shouldReturnRfc7807ProblemDetailsOnErrors() {
        // 1. Payload inválido -> 400 Bad Request
        CreateReservationRequest invalidRequest = CreateReservationRequest.builder()
                .roomId(102L)
                .guestName("")
                .guestEmail("correo-sin-arroba")
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(2))
                .totalPrice(BigDecimal.ZERO)
                .build();

        ResponseEntity<ProblemDetail> badRequestResponse =
                restTemplate.postForEntity("/api/v1/reservations", invalidRequest, ProblemDetail.class);

        assertThat(badRequestResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequestResponse.getBody()).isNotNull();
        assertThat(badRequestResponse.getBody().getStatus()).isEqualTo(400);

        // 2. Consulta Lado Query inexistente -> 404 Not Found
        ResponseEntity<ProblemDetail> notFoundResponse =
                restTemplate.getForEntity("/api/v1/reservations/999999", ProblemDetail.class);

        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFoundResponse.getBody()).isNotNull();
        assertThat(notFoundResponse.getBody().getStatus()).isEqualTo(404);
    }
}