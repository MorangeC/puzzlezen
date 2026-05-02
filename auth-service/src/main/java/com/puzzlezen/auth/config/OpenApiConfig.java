package com.puzzlezen.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PuzzleZen — Auth Service")
                .description("Gestion de l'authentification JWT. Inscris-toi ou connecte-toi pour obtenir un token Bearer à utiliser sur les autres services.")
                .version("1.0.0")
                .contact(new Contact().name("PuzzleZen").url("https://github.com/ton-user/puzzlezen")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Colle ton token JWT ici (sans 'Bearer ')")));
    }
}
