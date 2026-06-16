package com.eticaret.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("E-Ticaret Ürün ve Sipariş API")
                .version("1.0.0")
                .description("""
                    JPA & Hibernate tüm ilişkiler, Liquibase migration,
                    Specification pattern, Java 21 Sealed Classes ve Virtual Threads
                    ile geliştirilmiş e-ticaret REST API.
                    """)
                .contact(new Contact()
                    .name("E-Ticaret Dev Team")
                    .email("dev@eticaret.com"))
                .license(new License().name("MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Yerel Geliştirme"),
                new Server().url("http://localhost:8080").description("Docker")
            ));
    }
}
