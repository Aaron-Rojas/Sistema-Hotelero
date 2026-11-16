package com.hotel.reservas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Configuración del cliente asíncrono y plantilla de mensajería para Amazon SQS.
 *
 * ¿CÓMO funciona?
 * Inyecta el cliente asíncrono no bloqueante SqsAsyncClient (provisto por Spring Cloud AWS v3)
 * para crear un SqsTemplate de alto rendimiento.
 *
 * ¿POR QUÉ se registra JavaTimeModule?
 * El SqsTemplate construye internamente su propio ObjectMapper. Sin JavaTimeModule, los tipos
 * java.time.* (LocalDate, Instant) no son serializables, generando errores al publicar eventos.
 * Se inyecta un ObjectMapper configurado con JavaTimeModule y WRITE_DATES_AS_TIMESTAMPS=false
 * para garantizar el formato ISO-8601 en los mensajes SQS.
 */
@Configuration
public class AwsSqsConfig {

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        // Configura ObjectMapper con soporte para java.time.* (JSR-310)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Serializa fechas como strings ISO-8601, no como timestamps numéricos
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2MessageConverter payloadConverter = new MappingJackson2MessageConverter();
        payloadConverter.setObjectMapper(objectMapper);
        payloadConverter.setSerializedPayloadClass(String.class);

        SqsMessagingMessageConverter messageConverter = new SqsMessagingMessageConverter();
        messageConverter.setPayloadMessageConverter(payloadConverter);

        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .messageConverter(messageConverter)
                .build();
    }
}


