package com.hotel.reservas.config;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Configuración del cliente asíncrono y plantilla de mensajería para Amazon SQS.
 * 
 * ¿CÓMO funciona?
 * Inyecta el cliente asíncrono no bloqueante SqsAsyncClient (provisto por Spring Cloud AWS v3)
 * para crear un SqsTemplate de alto rendimiento.
 * 
 * ¿POR QUÉ se implementa?
 * Permite desacoplar operaciones lentas (notificaciones, confirmación bancaria y facturación)
 * de la petición HTTP del usuario, amortiguando picos repentinos de reservas hoteleras.
 */
@Configuration
public class AwsSqsConfig {

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}

