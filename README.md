# PuzzleZen

> Plateforme de jeux de réflexion construite en architecture microservices Java - projet portfolio.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen?style=flat-square&logo=springboot)
![Docker](https://img.shields.io/badge/Docker-ready-blue?style=flat-square&logo=docker)
![Kubernetes](https://img.shields.io/badge/Kubernetes-manifests-326ce5?style=flat-square&logo=kubernetes)
![Kafka](https://img.shields.io/badge/Kafka-event_bus-231F20?style=flat-square&logo=apachekafka)

---

## Concept

PuzzleZen propose **3 jeux sélectionnés aléatoirement** parmi une banque, pour chaque niveau de difficulté. Le joueur s'authentifie, choisit son niveau, résout les puzzles et se retrouve au classement en temps réel.

## Architecture microservices

```
                        ┌─────────────────────┐
                        │     Frontend         │
                        │   HTML/CSS/JS        │
                        └──────────┬──────────┘
                                   │ HTTP
                        ┌──────────▼──────────┐
                        │     API Gateway      │  :8080
                        │  Spring Cloud GW     │
                        └──┬──┬──┬──┬──┬──────┘
                           │  │  │  │  │
          ┌────────────────┘  │  │  │  └──────────────────┐
          │         ┌─────────┘  └──────────┐             │
          ▼         ▼                        ▼             ▼
    ┌──────────┐ ┌──────────┐ ┌──────────────────┐ ┌──────────────┐
    │  auth    │ │  user    │ │  game-service     │ │ leaderboard  │
    │ :8081    │ │ :8082    │ │  :8083            │ │  :8084       │
    │ JWT/PG   │ │ PG/Kafka │ │  MongoDB/Random   │ │  Redis ZSet  │
    └──────────┘ └──────────┘ └──────────────────┘ └──────────────┘
          │           │              │                     │
          └───────────┴──────────────┴─────────────────────┘
                              │ Kafka
                    ┌─────────▼─────────┐
                    │ notification-svc  │  :8085
                    │  WebSocket STOMP  │
                    └───────────────────┘
```

## Jeux disponibles

| Niveau | Jeux |
|--------|------|
| 🟢 **Facile** | Sudoku 4×4 · Puzzle image (3×3) · Décodage Morse |
| 🟡 **Intermédiaire** | Parking Rush · Labyrinthe 10×10 · Recréer l'image |
| 🔴 **Difficile** | Sudoku 9×9 · Chiffrement César · Tour de Hanoï (5 disques) |

## Stack technique

| Couche | Technologie |
|--------|------------|
| Langage | Java 21 |
| Framework | Spring Boot 3.2 · Spring Cloud 2023 |
| Gateway | Spring Cloud Gateway |
| Auth | Spring Security + JWT (jjwt 0.12) |
| BDD relationnelle | PostgreSQL 16 |
| BDD documentaire | MongoDB 7 |
| Cache / Classement | Redis 7 (Sorted Sets) |
| Messaging | Apache Kafka |
| WebSocket | STOMP over SockJS |
| Conteneurs | Docker (multi-stage builds) |
| Orchestration | Kubernetes + Helm |
| CI/CD | GitHub Actions |

## Démarrage rapide

```bash
git clone https://github.com/ton-user/puzzlezen.git
cd puzzlezen
docker-compose up -d

# Health check
curl http://localhost:8080/actuator/health
# → {"status":"UP"}

# 3 jeux aléatoires
curl "http://localhost:8083/api/games/session?difficulty=EASY"
```

Ouvre ensuite `frontend/index.html` dans ton navigateur.

## Flux événements Kafka

```
auth-service  ──► [user-registered]  ──► user-service (crée le profil)
game-service  ──► [game-completed]   ──► leaderboard-service (score)
                                     └─► notification-service (WS broadcast)
```

## Points techniques notables

- **Database per service** - chaque microservice a sa propre base (pattern DDD)
- **Event-driven** - communication asynchrone via Kafka
- **JWT stateless** - authentification sans session serveur
- **Redis Sorted Sets** - classement en O(log n)
- **Multi-stage Docker builds** - images légères (JRE alpine)
- **Seed automatique** - banque de jeux MongoDB peuplée au démarrage

---
*Architecture microservices Java · Spring Boot · Docker · Kubernetes*
