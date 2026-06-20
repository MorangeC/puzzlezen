package com.puzzlezen.game.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
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
                .title("PuzzleZen — Game Service")
                .description("""
                    Gestion de la banque de jeux et sélection aléatoire.
                    
                    **Fonctionnement :**
                    - `GET /api/games/session?difficulty=EASY` → retourne 3 jeux tirés au hasard
                    - Les jeux sont seedés automatiquement au démarrage (9 jeux, 3 par niveau)
                    
                    **Niveaux disponibles :** `EASY`, `MEDIUM`, `HARD`
                    """)
                .version("1.0.0")
                .contact(new Contact().name("PuzzleZen").url("https://github.com/ton-user/puzzlezen")))
            .servers(List.of(
                new Server().url("http://localhost:8083").description("Dev local direct"),
                new Server().url("http://localhost:8080").description("Via API Gateway")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT obtenu via /api/auth/login")));
    }
}
