package com.hotel.reservas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de carga básica del contexto de Spring Boot.
 */
@SpringBootTest
@ActiveProfiles("test")
class MsReservasApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que la inyección de dependencias y configuración de Spring arranque correctamente
    }
}

