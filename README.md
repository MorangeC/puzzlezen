# 🧩 PuzzleZen — Plateforme de jeux en microservices

Architecture microservices complète en Java Spring Boot, dockerisée et orchestrée par Kubernetes.

## Architecture

```
puzzlezen/
├── api-gateway/          # Spring Cloud Gateway — point d'entrée unique
├── auth-service/         # Authentification JWT
├── user-service/         # Profils & historique
├── game-service/         # Banque de jeux & sélection aléatoire
├── leaderboard-service/  # Classements (Redis)
├── notification-service/ # WebSocket (timers, alertes)
├── frontend/             # React (optionnel)
├── k8s/                  # Manifests Kubernetes
├── helm/                 # Helm charts
└── docker-compose.yml    # Dev local
```

## Stack technique

| Couche | Technologie |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot 3.2, Spring Cloud 2023 |
| API Gateway | Spring Cloud Gateway |
| Auth | Spring Security + JWT (jjwt) |
| Base de données | PostgreSQL (auth, user), MongoDB (games), Redis (leaderboard) |
| Messaging | Kafka |
| Conteneurs | Docker + Kubernetes |
| Deploy | Helm |
| CI/CD | GitHub Actions |

## Lancer le projet en local

### Prérequis
- Java 21
- Docker + Docker Compose
- Maven 3.9+

### Démarrage

```bash
# 1. Cloner le repo
git clone https://github.com/ton-user/puzzlezen.git
cd puzzlezen

# 2. Lancer toute l'infra (BDD, Kafka, Redis)
docker-compose up -d

# 3. Lancer chaque service (dans des terminaux séparés ou via IDE)
cd auth-service && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd game-service && mvn spring-boot:run
cd leaderboard-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

## Ports

| Service | Port |
|---|---|
| API Gateway | 8080 |
| Auth Service | 8081 |
| User Service | 8082 |
| Game Service | 8083 |
| Leaderboard Service | 8084 |
| Notification Service | 8085 |
| PostgreSQL | 5432 |
| MongoDB | 27017 |
| Redis | 6379 |
| Kafka | 9092 |

## Jeux disponibles

### 🟢 Facile
- **Sudoku 4×4** — grille simple générée aléatoirement
- **Puzzle image** — image découpée en 9 pièces
- **Morse** — décoder un message en morse

### 🟡 Intermédiaire
- **Parking Rush** — faire sortir la voiture du parking
- **Labyrinthe** — trouver la sortie
- **Recréer une image** — replacer les éléments au bon endroit

### 🔴 Difficile
- **Sudoku 9×9** — grille complète
- **Cryptage César** — décoder un message chiffré
- **Tour de Hanoi** — résoudre le problème en N déplacements

## Déploiement Kubernetes

```bash
# Déployer avec Helm
helm install puzzlezen ./helm/puzzlezen

# Vérifier les pods
kubectl get pods -n puzzlezen

# Voir les logs d'un service
kubectl logs -f deployment/game-service -n puzzlezen
```


après avoir allumer dockerdesktop, docker-compose up -d   dans le dossier racine :)

les vérifications :

# 1. Health check de la gateway
curl http://localhost:8080/actuator/health

# 2. 3 jeux aléatoires niveau EASY
curl "http://localhost:8083/api/games/session?difficulty=EASY"

# 3. Créer un compte (remplace les valeurs)
curl -X POST http://localhost:8081/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"username":"morgan","email":"morgan@test.com","password":"password123"}'