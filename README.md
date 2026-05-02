# PuzzleZen

Plateforme de jeux de réflexion construite en architecture microservices Java, conteneurisée avec Docker et orchestrée par Kubernetes. Ce document retrace les décisions d'architecture, les choix techniques effectués, les problèmes rencontrés et les solutions retenues.

---

## Sommaire

1. [Présentation du projet](#présentation-du-projet)
2. [Architecture](#architecture)
3. [Stack technique](#stack-technique)
4. [Les jeux](#les-jeux)
5. [Lancer la plateforme](#lancer-la-plateforme)
6. [API Reference](#api-reference)
7. [Monitoring](#monitoring)
8. [Tests](#tests)
9. [Déploiement Kubernetes](#déploiement-kubernetes)
10. [Décisions techniques et retours d'expérience](#décisions-techniques-et-retours-dexpérience)

---

## Présentation du projet

PuzzleZen propose trois niveaux de difficulté, chacun associé à trois jeux tirés aléatoirement depuis une banque de données stockée en MongoDB. Le joueur s'authentifie, choisit son niveau, résout les puzzles dans le temps imparti et voit son score apparaître dans un classement en temps réel.

L'objectif technique était de concevoir une architecture rigoureusement orientée microservices : chaque service est indépendant, possède sa propre base de données, communique de manière asynchrone via Kafka lorsque c'est pertinent, et expose une API documentée via OpenAPI.

---

## Architecture

```
Frontend (HTML/CSS/JS)
         |
         | HTTP
         v
  [ API Gateway :8080 ]
  Spring Cloud Gateway
         |
   ______v______________________________________
   |        |            |            |        |
   v        v            v            v        v
auth    user-svc     game-svc    leaderboard  notif
:8081   :8082        :8083        :8084       :8085
 JWT   PostgreSQL   MongoDB      Redis       WebSocket
       PostgreSQL                            STOMP
         |            |            |
         v            v            v
         +------------+------------+
                      |
                   Kafka
              (bus d'événements)
```

Chaque microservice est packagé dans une image Docker construite en deux étapes (multi-stage build : Maven pour compiler, JRE Alpine pour exécuter), ce qui réduit la taille des images finales.

---

## Stack technique

| Couche | Technologie | Version |
|---|---|---|
| Langage | Java | 21 |
| Framework | Spring Boot | 3.2.3 |
| Gateway | Spring Cloud Gateway | 2023.0.0 |
| Authentification | Spring Security + JWT (jjwt) | 0.12.3 |
| Base relationnelle | PostgreSQL | 16 |
| Base documentaire | MongoDB | 7 |
| Cache et classements | Redis | 7 |
| Messaging | Apache Kafka + Zookeeper | 7.5.0 |
| WebSocket | STOMP over SockJS | - |
| Documentation API | springdoc-openapi | 2.3.0 |
| Métriques | Micrometer + Prometheus | - |
| Monitoring | Grafana | 10.3.0 |
| Conteneurs | Docker | - |
| Orchestration | Kubernetes + Helm | - |
| CI/CD | GitHub Actions | - |

---

## Les jeux

La banque de jeux est peuplée automatiquement au premier démarrage via un `CommandLineRunner` dans le game-service. Si la collection MongoDB contient déjà des données, le seed est ignoré.

### Niveau facile (300s, 180s, 120s)

**Sudoku 4x4** - Grille générée avec une solution unique. Le joueur remplit les cases vides ; les erreurs sont signalées en rouge en temps réel.

**Puzzle image** - Neuf pièces colorées mélangées. Le joueur clique deux pièces pour les échanger jusqu'à reconstituer l'image de référence affichée à côté.

**Décodage Morse** - Un message en code Morse est affiché avec une table de référence. Le joueur saisit la réponse en clair.

### Niveau intermédiaire (240s, 180s, 300s)

**Parking Rush** - Grille 6x6 avec des véhicules à déplacer. La voiture cible doit être guidée vers la sortie droite de la ligne 3. Les voitures sont déplaçables à la souris avec détection de collision complète : aucun véhicule ne peut traverser un autre ni sortir du plateau.

**Labyrinthe** - Généré procéduralement via l'algorithme de backtracking récursif (DFS). Le joueur se déplace au clavier (flèches directionnelles) ou via les boutons à l'écran. Le labyrinthe est différent à chaque partie.

**Recréer l'image** - Six pièces colorées à placer dans les bons emplacements par glisser-déposer. L'image cible est affichée en référence.

### Niveau difficile (1200s, 180s, 600s)

**Sudoku 9x9** - Grille générée aléatoirement par backtracking avec 52 cellules retirées. Navigation possible au clavier entre les cases (les cases pré-remplies sont automatiquement ignorées). Vérification complète à la soumission.

**Chiffrement César** - Un message chiffré est affiché. Le joueur ajuste un curseur de décalage (1 à 25) et voit le déchiffrement en temps réel. La validation compare le texte obtenu à la solution.

**Tour de Hanoï** - Cinq disques, trois tiges. Le joueur clique une tige source puis une tige destination. Le moteur vérifie qu'un grand disque ne peut jamais être posé sur un petit. Le nombre de mouvements minimum (31) est affiché pour information.

---

## Lancer la plateforme

### Prérequis

- Docker Desktop installé et démarré
- Node.js (pour `npx serve`, optionnel)
- Java 21 et Maven 3.9 uniquement si l'on souhaite travailler hors Docker

### Démarrage complet

```bash
# Cloner le dépôt
git clone https://github.com/votre-utilisateur/puzzlezen.git
cd puzzlezen

# Lancer toute l'infrastructure et les microservices
docker-compose up -d --build
```

Le premier lancement est long (téléchargement des images, compilation Maven dans les conteneurs). Les lancements suivants sont beaucoup plus rapides.

### Vérification

```bash
# Tous les conteneurs doivent être Started ou Healthy
docker-compose ps

# Health check global via la gateway
curl http://localhost:8080/actuator/health
# Réponse attendue : {"status":"UP"}

# Tester la sélection aléatoire de jeux
curl "http://localhost:8083/api/games/session?difficulty=EASY"
```

### Accéder au frontend

```bash
cd frontend
npx serve .
```

Ouvrir ensuite http://localhost:3000 dans le navigateur. Sans `npx`, ouvrir `frontend/index.html` directement (certains navigateurs bloquent les requêtes vers localhost depuis un fichier local ; utiliser un serveur est recommandé).

### URLs disponibles

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Hub documentation API | http://localhost:3000/api-docs.html |
| API Gateway | http://localhost:8080 |
| Auth Service Swagger | http://localhost:8081/swagger-ui |
| User Service Swagger | http://localhost:8082/swagger-ui |
| Game Service Swagger | http://localhost:8083/swagger-ui |
| Leaderboard Service Swagger | http://localhost:8084/swagger-ui |
| Notification Service Swagger | http://localhost:8085/swagger-ui |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (admin / puzzlezen) |

---

## API Reference

### Auth Service - :8081

| Méthode | Endpoint | Corps | Description |
|---|---|---|---|
| POST | /api/auth/register | `{username, email, password}` | Créer un compte, retourne un JWT |
| POST | /api/auth/login | `{username, password}` | Se connecter, retourne un JWT |

Le token retourné est valable 24 heures. Il doit être transmis dans le header `Authorization: Bearer <token>` pour tous les autres endpoints.

### Game Service - :8083

| Méthode | Endpoint | Description |
|---|---|---|
| GET | /api/games/session?difficulty=EASY | 3 jeux aléatoires pour le niveau donné |
| GET | /api/games/{id} | Configuration complète d'un jeu |
| GET | /api/games | Tous les jeux de la banque |
| POST | /api/games | Ajouter un jeu (usage admin) |

Valeurs acceptées pour `difficulty` : `EASY`, `MEDIUM`, `HARD`.

### User Service - :8082

| Méthode | Endpoint | Description |
|---|---|---|
| GET | /api/users/{username}/profile | Profil, score total, parties jouées/gagnées |
| GET | /api/users/{username}/history | Historique trié par date décroissante |
| POST | /api/users/results | Enregistrer le résultat d'une partie |

### Leaderboard Service - :8084

| Méthode | Endpoint | Description |
|---|---|---|
| GET | /api/leaderboard/global?limit=10 | Top N tous niveaux confondus |
| GET | /api/leaderboard/{difficulty} | Top N pour un niveau donné |
| GET | /api/leaderboard/{difficulty}/rank/{username} | Rang d'un joueur (base 1) |
| POST | /api/leaderboard/submit | Soumettre un score `{username, difficulty, score}` |

### Flux Kafka

```
auth-service     --[user-registered]-->  user-service       (création automatique du profil)
game-service     --[game-completed]-->   leaderboard-service (mise à jour du score Redis)
                                    -->  notification-service (broadcast WebSocket)
```

---

## Monitoring

Prometheus scrape les endpoints `/actuator/prometheus` de chaque microservice toutes les 15 secondes. Grafana se connecte automatiquement à Prometheus au démarrage et charge le dashboard préconfigured.

Le dashboard **PuzzleZen - Microservices Overview** expose quatre sections :

**Services Health** - nombre de services UP, requêtes par minute, taux d'erreur 5xx, latence moyenne.

**HTTP Traffic** - courbes de trafic par service (req/s), latence au percentile 95 par service.

**JVM et ressources** - consommation heap par service, usage CPU, nombre de threads actifs.

**Métriques métier** - sessions de jeu démarrées (appels à `/api/games/session`), requêtes vers le leaderboard.

---

## Tests

Cinq fichiers de tests unitaires couvrent les composants critiques, pour un total de 24 cas de test.

| Fichier | Composant testé | Cas de test |
|---|---|---|
| GameServiceTest | Sélection aléatoire, cas limites, mélange | 5 |
| GameControllerTest | Endpoints REST via MockMvc | 3 |
| AuthServiceTest | Register, login, cas d'erreur | 6 |
| JwtUtilsTest | Génération, validation, extraction | 5 |
| LeaderboardServiceTest | Redis Sorted Sets, rangs | 5 |

Lancer les tests depuis la racine d'un service :

```bash
cd auth-service
mvn test
```

---

## Déploiement Kubernetes

Les manifests sont dans le dossier `k8s/`. Ils supposent que les images Docker ont été construites et poussées sur un registre.

```bash
# Créer le namespace
kubectl apply -f k8s/namespace.yml

# Créer les secrets (modifier les valeurs avant)
kubectl apply -f k8s/secrets.yml

# Déployer les services
kubectl apply -f k8s/deployments.yml

# Vérifier
kubectl get pods -n puzzlezen
kubectl get services -n puzzlezen

# Logs d'un service
kubectl logs -f deployment/game-service -n puzzlezen
```

Les deploiements Kubernetes configurent des `readinessProbe` et `livenessProbe` sur `/actuator/health` pour chaque service, ce qui permet à Kubernetes de gérer les redémarrages automatiquement.

---

## Décisions techniques et retours d'expérience

### Ce qui a été conservé tel quel

**Base de données par service.** Chaque microservice possède sa propre base de données, conformément au pattern Domain-Driven Design. Cela implique qu'il n'y a pas de jointures entre services : le user-service ne connaît pas MongoDB, le game-service ne connaît pas PostgreSQL. La cohérence entre services est assurée par les événements Kafka.

**JWT stateless.** Le choix d'une authentification sans état côté serveur simplifie le scaling horizontal : n'importe quelle instance du service peut valider un token sans consulter une session centralisée.

**Redis Sorted Sets pour le leaderboard.** L'insertion et la lecture d'un rang se font en O(log n) avec `ZADD` et `ZREVRANK`, quelle que soit la taille du classement. Une solution SQL aurait nécessité un `ORDER BY` avec `RANK()`, moins efficace à grande échelle.

**Multi-stage Docker builds.** Les Dockerfiles compilent avec Maven dans un premier stage, puis copient uniquement le JAR dans une image JRE Alpine. Cela réduit significativement la taille des images finales en excluant Maven, les sources et les dépendances de compilation.

### Ce qui a été modifié en cours de développement

**L'endpoint de soumission du leaderboard.** Dans la conception initiale, le leaderboard était mis à jour exclusivement via Kafka (topic `game-completed`). En pratique, cela suppose que Kafka soit parfaitement opérationnel et que le consumer soit démarré avant la première partie. Pour fiabiliser la démonstration, un endpoint `POST /api/leaderboard/submit` a été ajouté directement sur le leaderboard-service. Le frontend l'appelle après chaque victoire. Le chemin Kafka reste présent et fonctionnel ; les deux mécanismes coexistent.

**La gestion du state de session côté frontend.** La première version stockait uniquement les identifiants MongoDB des jeux terminés dans un `Set`. Lorsque la modal se fermait, `renderSessionGrid()` reconstruisait les cartes depuis `currentSession` mais ne retrouvait pas toujours les identifiants si ceux-ci avaient une forme inattendue. La solution retenue stocke à la fois l'identifiant MongoDB et l'index du jeu dans la session, ce qui rend la correspondance robuste dans tous les cas.

**Le Parking Rush.** La première implémentation utilisait un rendu statique avec un bouton "Vérifier la sortie" qui simulait une victoire. Elle a été entièrement remplacée par un moteur avec détection de collision pixel-accurate, drag-and-drop souris et tactile, snap sur la grille et détection automatique de victoire dès que la voiture cible atteint la colonne de sortie.

**`ctx.roundRect()` dans le labyrinthe.** Cette méthode Canvas n'est pas supportée par les versions antérieures de Safari et Firefox. Elle a été remplacée par `ctx.rect()` pour garantir la compatibilité.

**La largeur de la modal.** La modal était initialement limitée à 580px, ce qui rendait le Puzzle Image illisible car ses deux panneaux côte à côte dépassaient. Elle a été élargie à 640px.

### Ce qui a été abandonné

**springdoc sur l'api-gateway.** La gateway utilise Spring Cloud Gateway qui repose sur WebFlux (réactif), alors que les autres services utilisent le stack Web classique (Servlet). springdoc propose deux artefacts distincts : `webmvc-ui` et `webflux-ui`. L'artefact WebFlux a été ajouté mais la documentation de la gateway reste moins riche que celle des autres services, car la gateway ne définit pas de controllers annotés : ses routes sont déclarées en YAML.

**Les images dans le Puzzle Image.** La version initiale prévoyait d'utiliser de vraies images chargées depuis `/assets/`. Cela aurait nécessité un serveur de fichiers statiques supplémentaire. Pour simplifier le déploiement et éviter une dépendance externe, les pièces ont été remplacées par des couleurs vives et des formes géométriques générées en CSS pur, ce qui fonctionne sans aucune ressource externe.

**Helm charts détaillés.** Les templates Helm ont été initiés dans le dossier `helm/` mais non complétés. Les manifests `k8s/` bruts sont suffisants pour une démonstration ; les Helm charts apportent de la valeur pour un déploiement paramétrisé sur plusieurs environnements (staging, production), ce qui dépasse le périmètre actuel du projet.

---

## Structure du projet

```
puzzlezen/
|-- api-gateway/              Point d'entrée unique, routage vers les services
|-- auth-service/             Authentification JWT, PostgreSQL
|-- user-service/             Profils joueurs, historique, Kafka consumer
|-- game-service/             Banque de jeux MongoDB, seed automatique, sélection aléatoire
|-- leaderboard-service/      Classements Redis Sorted Sets, Kafka consumer
|-- notification-service/     WebSocket STOMP, broadcast Kafka vers clients
|-- frontend/
|   |-- index.html            Application principale (auth, jeux, classement)
|   |-- parking-rush.html     Version standalone du Parking Rush
|   `-- api-docs.html         Hub centralisé vers les Swagger UI
|-- monitoring/
|   |-- prometheus.yml        Configuration du scraping
|   `-- grafana/              Dashboard préconfigured, provisioning automatique
|-- k8s/                      Manifests Kubernetes (namespace, secrets, deployments)
|-- scripts/                  Initialisation PostgreSQL multi-base
|-- docker-compose.yml        Environnement de développement complet (15 conteneurs)
`-- .github/workflows/        Pipeline CI/CD GitHub Actions
```