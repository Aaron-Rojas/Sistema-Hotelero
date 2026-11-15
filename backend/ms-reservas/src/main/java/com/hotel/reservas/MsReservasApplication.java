package com.hotel.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del microservicio de reservas hoteleras.
 * 
 * ¿POR QUÉ esta configuración?
 * @SpringBootApplication activa la configuración automática, el escaneo de componentes
 * y la configuración de propiedades. La aplicación opera de forma 100% Stateless,
 * permitiendo escalabilidad horizontal elástica tras un Application Load Balancer (ALB).
 */
@SpringBootApplication
public class MsReservasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsReservasApplication.class, args);
    }
}

