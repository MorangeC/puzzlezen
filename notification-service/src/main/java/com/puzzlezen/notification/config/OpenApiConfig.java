package com.puzzlezen.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                .title("PuzzleZen — Notification Service")
                .description("""
                    Service de notifications temps réel via WebSocket (STOMP over SockJS).
                    
                    **Connexion WebSocket :**
                    ```
                    ws://localhost:8085/ws
                    ```
                    
                    **Topics disponibles :**
                    - `/topic/leaderboard` — mise à jour du classement après chaque partie
                    - `/queue/notifications` — notifications personnelles (timer, fin de session)
                    
                    **Note :** ce service n'expose pas d'endpoints REST — il est piloté par Kafka.
                    """)
                .version("1.0.0"))
            .servers(List.of(
                new Server().url("http://localhost:8085").description("Dev local")));
    }
}
