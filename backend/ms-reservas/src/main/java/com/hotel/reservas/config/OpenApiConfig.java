package com.hotel.reservas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración centralizada de OpenAPI 3.1 / Swagger.
 * 
 * ¿CÓMO funciona?
 * Define la metadata del contrato de la API RESTful que sirve como documentación
 * viva y punto de integración para el cliente Frontend.
 * 
 * ¿POR QUÉ se implementa?
 * En arquitecturas desacopladas, el contrato de API debe ser la única fuente de verdad
 * entre el backend y los consumidores web o móviles.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hotel Reservation Service API")
                        .version("1.0.0")
                        .description("API RESTful Stateless para el sistema de reservas hoteleras cloud-native con soporte para AWS SQS y RDS Multi-AZ.")
                        .contact(new Contact()
                                .name("Cloud Architecture Team")
                                .email("architecture@hotel.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}

