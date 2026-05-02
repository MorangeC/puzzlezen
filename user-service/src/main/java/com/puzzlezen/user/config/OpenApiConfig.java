package com.puzzlezen.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PuzzleZen — User Service")
                .description("""
                    Gestion des profils joueurs et de l'historique des parties.
                    
                    **Ce service écoute aussi Kafka** : quand auth-service publie `user-registered`,
                    un profil est automatiquement créé ici.
                    """)
                .version("1.0.0"))
            .servers(List.of(
                new Server().url("http://localhost:8082").description("Dev local"),
                new Server().url("http://localhost:8080").description("Via Gateway")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
