package com.hotel.reservas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;

/**
 * Prueba de carga básica del contexto de Spring Boot.
 */
@SpringBootTest
@ActiveProfiles({"test", "api"})
class MsReservasApplicationTests {

    // Mock de dependencias que no están disponibles en el perfil test
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private SqsTemplate sqsTemplate;

    @Test
    void contextLoads() {
        // Verifica que la inyección de dependencias y configuración de Spring arranque correctamente
    }
}

