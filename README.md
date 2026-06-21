# PuzzleZen

Plateforme de jeux de réflexion construite en architecture microservices Java, conteneurisée avec Docker et orchestrée par Kubernetes. Ce document retrace les décisions d'architecture, les choix techniques effectués, le guide de démarrage et de débogage pas à pas, ainsi que l'historique des problèmes rencontrés et des solutions retenues.

---

## Sommaire

1. [Présentation du projet](#présentation-du-projet)
2. [Architecture](#architecture)
3. [Stack technique](#stack-technique)
4. [Les jeux](#les-jeux)
5. [Démarrage — guide pas à pas](#démarrage--guide-pas-à-pas)
6. [Script de démarrage automatisé](#script-de-démarrage-automatisé)
7. [Créer un compte joueur](#créer-un-compte-joueur)
8. [Guide de débogage](#guide-de-débogage)
9. [Journal des bugs rencontrés et corrigés](#journal-des-bugs-rencontrés-et-corrigés)
10. [API Reference](#api-reference)
11. [Monitoring](#monitoring)
12. [Tests](#tests)
13. [Déploiement Kubernetes](#déploiement-kubernetes)
14. [Décisions techniques et retours d'expérience](#décisions-techniques-et-retours-dexpérience)
15. [Structure du projet](#structure-du-projet)

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

La banque de jeux est peuplée automatiquement au premier démarrage via un `CommandLineRunner` (`GameDataSeeder`) dans le game-service. Si la collection MongoDB `games` contient déjà des données, le seed est ignoré.

### Niveau facile (300s, 180s, 120s)

**Sudoku 4×4** (`SUDOKU_4`) - Grille générée avec une solution unique. Le joueur remplit les cases vides ; les erreurs sont signalées en rouge en temps réel.

**Puzzle image** (`PUZZLE_IMAGE`) - Neuf pièces colorées mélangées. Le joueur clique deux pièces pour les échanger jusqu'à reconstituer l'image de référence affichée à côté.

**Décodage Morse** (`MORSE_DECODE`) - Un message en code Morse est affiché avec une table de référence. Le joueur saisit la réponse en clair.

### Niveau intermédiaire (240s, 180s, 300s)

**Parking Rush** (`PARKING_RUSH`) - Grille 6×6 avec des véhicules à déplacer. La voiture cible doit être guidée vers la sortie droite de la ligne 3. Les voitures sont déplaçables à la souris avec détection de collision complète : aucun véhicule ne peut traverser un autre ni sortir du plateau.

**Labyrinthe** (`LABYRINTH`) - Généré procéduralement via l'algorithme de backtracking récursif (DFS). Le joueur se déplace au clavier (flèches directionnelles) ou via les boutons à l'écran. Le labyrinthe est différent à chaque partie.

**Recréer l'image** (`IMAGE_RECREATE`) - Six pièces colorées à placer dans les bons emplacements par glisser-déposer. L'image cible est affichée en référence.

### Niveau difficile (1200s, 180s, 600s)

**Sudoku 9×9** (`SUDOKU_9`) - Grille générée aléatoirement par backtracking avec 52 cellules retirées. Navigation possible au clavier entre les cases (les cases pré-remplies sont automatiquement ignorées). Vérification complète à la soumission.

**Chiffrement César** (`CAESAR_CIPHER`) - Un message chiffré est affiché. Le joueur ajuste un curseur de décalage (1 à 25) et voit le déchiffrement en temps réel. La validation compare le texte obtenu à la solution.

**Tour de Hanoï** (`HANOI_TOWER`) - Cinq disques, trois tiges. Le joueur clique une tige source puis une tige destination. Le moteur vérifie qu'un grand disque ne peut jamais être posé sur un petit. Le nombre de mouvements minimum (31) est affiché pour information.

> ⚠️ Les valeurs entre parenthèses ci-dessus sont les vraies valeurs de l'enum `Game.GameType` côté backend. Le frontend doit utiliser **exactement** ces chaînes (`SUDOKU_4`, pas `SUDOKU_4x4` ; `SUDOKU_9`, pas `SUDOKU_9x9`) dans `renderGame()`, sous peine de tomber dans le rendu générique de secours. Voir [Journal des bugs](#journal-des-bugs-rencontrés-et-corrigés).

---

## Démarrage — guide pas à pas

### Prérequis

- Docker Desktop installé et démarré
- Node.js (pour `npx serve`, optionnel)
- Java 21 et Maven 3.9 uniquement si l'on souhaite travailler hors Docker

### 1. Cloner et lancer l'infrastructure

```bash
git clone https://github.com/votre-utilisateur/puzzlezen.git
cd puzzlezen

# Lancer toute l'infrastructure et les microservices
docker-compose up -d --build
```

Le premier lancement est long (téléchargement des images, compilation Maven dans les conteneurs). Les lancements suivants sont beaucoup plus rapides.

### 2. Vérifier que tout est démarré

```bash
# Tous les conteneurs doivent être Started ou Healthy
docker ps
```

Tu dois voir 15 conteneurs actifs : `puzzlezen-gateway`, `puzzlezen-auth`, `puzzlezen-user`, `puzzlezen-game`, `puzzlezen-leaderboard`, `puzzlezen-notification`, `puzzlezen-postgres`, `puzzlezen-mongo`, `puzzlezen-redis`, `puzzlezen-kafka`, `puzzlezen-zookeeper`, `puzzlezen-grafana`, `puzzlezen-prometheus`.

```bash
# Health check global via la gateway
curl http://localhost:8080/actuator/health
# Réponse attendue : {"status":"UP"}

# Tester la sélection aléatoire de jeux
curl "http://localhost:8083/api/games/session?difficulty=EASY"
```

### 3. Lancer le frontend

```bash
cd frontend
npx serve .
```

Ouvrir ensuite http://localhost:3000 dans le navigateur. Sans `npx`, ouvrir `frontend/index.html` directement (certains navigateurs bloquent les requêtes vers localhost depuis un fichier local ; utiliser un serveur est recommandé).

> Le terminal `npx serve` n'a **aucun rapport** avec les containers Docker — il sert uniquement les fichiers statiques du frontend sur le port 3000. Il peut tourner en parallèle, dans un troisième terminal, sans interférer avec les commandes `docker` lancées ailleurs.

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

## Script de démarrage automatisé

**Une fois les bugs listés dans ce document corrigés dans le code source, il n'est plus nécessaire de retoucher les bases de données manuellement à chaque démarrage.** Le volume MongoDB persiste entre les redémarrages (`mongo_data` dans `docker-compose.yml`) : tant que la collection `games` contient des documents valides, `GameDataSeeder` ne fait rien (`if (gameRepository.count() > 0) { ... seed ignoré }`), et `docker-compose up -d --build` suffit pour relancer toute la plateforme.

Le reseed manuel n'est utile que dans deux cas : premier lancement sur un volume vierge, ou modification ultérieure des enums/données du seeder (voir [Journal des bugs #1](#1-niveaux-facile-et-difficile-inaccessibles--500-internal-server-error)).

Le script `start.sh` à la racine du projet automatise ce flux : il démarre Docker Compose, attend que la gateway soit en `UP`, vérifie le nombre de documents dans `game_db.games`, et ne déclenche un reseed que si la collection est vide (ou si on le force explicitement).

```bash
chmod +x start.sh   # une seule fois

./start.sh              # démarrage normal — reseed automatique seulement si nécessaire
./start.sh --reseed     # force un reseed même si la banque de jeux n'est pas vide
./start.sh --logs       # démarrage + suit les logs de tous les services ensuite
```

Le script demande le mot de passe MongoDB de façon interactive s'il n'est pas déjà exporté dans l'environnement :
```bash
export MONGO_PASSWORD=ton_mot_de_passe   # évite la saisie interactive à chaque lancement
./start.sh
```

```bash
#!/usr/bin/env bash
#
# start.sh — Démarrage automatisé de PuzzleZen
#
# Lance toute l'infrastructure Docker, attend que les services critiques
# soient sains, et propose un reseed MongoDB si la banque de jeux semble
# vide ou corrompue. À lancer depuis la racine du projet.
#
# Usage :
#   ./start.sh           Démarrage normal
#   ./start.sh --reseed  Force un reseed de la banque de jeux MongoDB
#   ./start.sh --logs    Affiche les logs de tous les services après démarrage
#
set -euo pipefail

MONGO_USER="puzzlezen"
MONGO_DB="game_db"
GATEWAY_HEALTH_URL="http://localhost:8080/actuator/health"
MAX_WAIT_SECONDS=120

FORCE_RESEED=false
SHOW_LOGS=false
for arg in "$@"; do
  case $arg in
    --reseed) FORCE_RESEED=true ;;
    --logs) SHOW_LOGS=true ;;
  esac
done

color() { printf "\033[%sm%s\033[0m\n" "$1" "$2"; }
info()  { color "36" "→ $1"; }
ok()    { color "32" "✓ $1"; }
warn()  { color "33" "⚠ $1"; }
err()   { color "31" "✗ $1"; }

# ── 1. Lancer Docker Compose ─────────────────────────────────────────
info "Démarrage de l'infrastructure Docker (build + up)..."
docker-compose up -d --build

# ── 2. Attendre que la gateway réponde "UP" ──────────────────────────
info "Attente du démarrage complet des services (jusqu'à ${MAX_WAIT_SECONDS}s)..."
elapsed=0
until curl -sf "$GATEWAY_HEALTH_URL" 2>/dev/null | grep -q '"status":"UP"'; do
  if [ "$elapsed" -ge "$MAX_WAIT_SECONDS" ]; then
    err "Timeout : la gateway ne répond pas après ${MAX_WAIT_SECONDS}s."
    warn "Vérifie les logs : docker logs puzzlezen-gateway --tail 50"
    exit 1
  fi
  sleep 3
  elapsed=$((elapsed + 3))
  printf "."
done
echo
ok "Gateway opérationnelle (${elapsed}s)."

# ── 3. Vérifier l'état de la banque de jeux MongoDB ──────────────────
info "Vérification de la banque de jeux (MongoDB)..."

MONGO_PASSWORD="${MONGO_PASSWORD:-}"
if [ -z "$MONGO_PASSWORD" ]; then
  warn "Variable MONGO_PASSWORD non définie dans l'environnement."
  warn "Lis le mot de passe depuis docker-compose.yml (MONGO_INITDB_ROOT_PASSWORD) si besoin,"
  warn "ou exporte-le avant de relancer : export MONGO_PASSWORD=ton_mot_de_passe"
  read -rsp "Mot de passe MongoDB (utilisateur '${MONGO_USER}') : " MONGO_PASSWORD
  echo
fi

GAME_COUNT=$(docker exec -i puzzlezen-mongo mongosh -u "$MONGO_USER" -p "$MONGO_PASSWORD" \
  --authenticationDatabase admin --quiet --eval \
  "db.getSiblingDB('${MONGO_DB}').games.countDocuments()" 2>/dev/null || echo "ERROR")

if [ "$GAME_COUNT" = "ERROR" ]; then
  err "Impossible de se connecter à MongoDB. Vérifie le mot de passe et que le container tourne."
  exit 1
fi

if [ "$FORCE_RESEED" = true ] || [ "$GAME_COUNT" -eq 0 ]; then
  if [ "$FORCE_RESEED" = true ]; then
    warn "Reseed forcé demandé (--reseed)."
  else
    warn "Banque de jeux vide (0 document) — reseed nécessaire."
  fi
  info "Suppression de la collection 'games' dans '${MONGO_DB}'..."
  docker exec -i puzzlezen-mongo mongosh -u "$MONGO_USER" -p "$MONGO_PASSWORD" \
    --authenticationDatabase admin --quiet --eval \
    "db.getSiblingDB('${MONGO_DB}').games.drop()" >/dev/null

  info "Redémarrage de game-service pour déclencher le seed automatique..."
  docker restart puzzlezen-game >/dev/null
  sleep 8

  NEW_COUNT=$(docker exec -i puzzlezen-mongo mongosh -u "$MONGO_USER" -p "$MONGO_PASSWORD" \
    --authenticationDatabase admin --quiet --eval \
    "db.getSiblingDB('${MONGO_DB}').games.countDocuments()" 2>/dev/null || echo "0")

  if [ "$NEW_COUNT" -gt 0 ]; then
    ok "Banque de jeux repeuplée : ${NEW_COUNT} jeux insérés."
  else
    err "Le reseed semble avoir échoué. Vérifie : docker logs puzzlezen-game --tail 30"
    exit 1
  fi
else
  ok "Banque de jeux déjà peuplée (${GAME_COUNT} jeux) — pas de reseed nécessaire."
fi

# ── 4. Récapitulatif ──────────────────────────────────────────────────
echo
ok "PuzzleZen est prêt."
echo
echo "  Frontend          → cd frontend && npx serve ."
echo "  Puis ouvrir        http://localhost:3000"
echo "  API Gateway        http://localhost:8080"
echo "  Grafana            http://localhost:3001 (admin / puzzlezen)"
echo

if [ "$SHOW_LOGS" = true ]; then
  info "Logs de tous les services (Ctrl+C pour quitter) :"
  docker-compose logs -f
fi
```

> Place ce script à la racine du projet (à côté de `docker-compose.yml`) sous le nom `start.sh`.

---

## Créer un compte joueur

Pour qu'une nouvelle personne (un prof, un camarade, etc.) puisse jouer, elle doit créer un compte via le formulaire d'inscription du frontend.

### Via l'interface

1. Ouvrir http://localhost:3000
2. Cliquer sur **« S'inscrire »** sous le formulaire de connexion
3. Remplir : nom d'utilisateur, email, mot de passe
4. Valider

**Contrainte de validation backend (`AuthController.RegisterRequest`) :**

| Champ | Règle |
|---|---|
| `username` | 3 à 30 caractères, non vide |
| `email` | doit être un email valide |
| `password` | **8 caractères minimum**, non vide |

Si le mot de passe fait moins de 8 caractères, le backend renvoie un `403 Forbidden` (la validation `@Valid` rejette la requête avant même d'atteindre la logique métier). Le frontend affiche désormais un message clair grâce au parsing du champ `errors` renvoyé par Spring (voir [Journal des bugs](#journal-des-bugs-rencontrés-et-corrigés)).

### Via psql (création manuelle, dépannage uniquement)

```bash
docker exec -it puzzlezen-postgres psql -U puzzlezen -d auth_db
```

```sql
INSERT INTO users (username, email, password, role)
VALUES ('test', 'test@test.com', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', 'PLAYER');
```

> Le champ `password` doit être un hash BCrypt valide, pas un mot de passe en clair — privilégier l'inscription via l'API/le frontend dans la quasi-totalité des cas.

---

## Guide de débogage

Méthodologie générale utilisée pendant le développement, à réutiliser en cas de nouveau bug.

### Étape 1 — Identifier la bonne source de logs

Le frontend (`npx serve`, port 3000) n'affiche que des requêtes HTTP statiques — il ne donne **aucune information** sur la cause d'une erreur 500/403/401. Toujours remonter vers le microservice concerné :

```bash
docker ps                              # lister tous les containers et leurs noms
docker logs <nom-du-container> --tail 30
```

| Symptôme frontend | Service à inspecter | Nom du container |
|---|---|---|
| Erreur sur `/api/auth/...` (register, login) | auth-service | `puzzlezen-auth` |
| Erreur sur `/api/games/...` | game-service | `puzzlezen-game` |
| Erreur sur `/api/users/...` | user-service | `puzzlezen-user` |
| Erreur sur `/api/leaderboard/...` | leaderboard-service | `puzzlezen-leaderboard` |

> Astuce : un `401` sur `/api/users/results` ou un `500` sur `/api/leaderboard/submit` juste après une connexion peut être le symptôme d'un **token JWT invalide généré en amont**, pas forcément un bug dans le service qui répond l'erreur — toujours vérifier d'abord que `/api/auth/login` renvoie un vrai `token` exploitable (voir [Journal des bugs #5](#5-connexion-réussie-mais-pseudo--undefined--et-401500-en-cascade)) avant de creuser plus loin dans user-service ou leaderboard-service.

### Étape 2 — Lire la stacktrace en entier

Une erreur `500` côté navigateur ne donne qu'un statut HTTP générique. La vraie cause (exception Java, ligne de code précise) n'apparaît que dans les logs du container, généralement sous forme de stacktrace avec un `Caused by:`. Ne pas hésiter à demander `--tail 50` ou plus si le log se coupe avant la ligne utile.

### Étape 3 — Vérifier les bases de données directement

**MongoDB (game-service) :**

```bash
docker exec -it puzzlezen-mongo mongosh -u puzzlezen -p
# (mot de passe : voir docker-compose.yml → MONGO_INITDB_ROOT_PASSWORD)
```

```js
show dbs                          // lister toutes les bases — attention aux noms !
use game_db                       // la vraie base utilisée par game-service (voir docker-compose.yml)
db.games.countDocuments()
db.games.distinct("type")
```

> ⚠️ Piège vécu : la base `puzzlezen` existe par défaut dans Mongo (créée à la connexion) mais **n'est pas** celle utilisée par le service. Toujours vérifier la variable d'environnement `SPRING_DATA_MONGODB_URI` du service dans `docker-compose.yml` pour confirmer le nom exact de la base (`game_db` dans ce projet), plutôt que de supposer.

**PostgreSQL (auth-service, user-service) :**

```bash
docker exec -it puzzlezen-postgres psql -U puzzlezen -d auth_db
# ou -d user_db selon le service
```

### Étape 4 — Forcer un reseed MongoDB après correction de données

Si des documents MongoDB contiennent des valeurs obsolètes (ex. un enum renommé côté Java mais jamais mis à jour en base) :

```bash
docker exec -it puzzlezen-mongo mongosh -u puzzlezen -p
```
```js
use game_db
db.games.drop()
exit
```
```bash
docker restart puzzlezen-game
docker logs puzzlezen-game --tail 15
```

Résultat attendu :
```
Peuplement de la banque de jeux...
✅ 9 jeux insérés.
```

Si le log indique encore `Banque de jeux déjà peuplée (X jeux), seed ignoré.` après un `drop()`, c'est très probablement que le `drop()` a été exécuté sur la mauvaise base (voir piège ci-dessus).

### Étape 5 — Vérifier la console navigateur (F12)

Pour les bugs de logique frontend (pas d'erreur HTTP, mais comportement incorrect à l'écran), la console navigateur reste indispensable, notamment pour les `console.log` de debug déjà présents dans `index.html` (`STATUS:`, `RAW RESPONSE:`).

---

## Journal des bugs rencontrés et corrigés

### 1. Niveaux Facile et Difficile inaccessibles — `500 Internal Server Error`

**Symptôme :** seul le niveau Intermédiaire fonctionnait ; Facile et Difficile renvoyaient une erreur 500 sur `GET /api/games/session?difficulty=EASY`.

**Cause :** la stacktrace du game-service révélait :
```
java.lang.IllegalArgumentException: No enum constant com.puzzlezen.game.model.Game.GameType.SUDOKU_4x4
```
L'enum `Game.GameType` avait été renommé côté code (`SUDOKU_4x4` → `SUDOKU_4`, `SUDOKU_9x9` → `SUDOKU_9`), mais les anciens documents MongoDB stockaient encore l'ancienne valeur textuelle. Spring Data MongoDB plantait à la désérialisation.

**Fix :** vider la collection `games` dans la bonne base (`game_db`, pas `puzzlezen`) et redémarrer le service pour déclencher le reseed automatique (voir [Étape 4](#étape-4--forcer-un-reseed-mongodb-après-correction-de-données)).

---

### 2. Session marquée « complète » après seulement 2 jeux sur 3

**Symptôme :** le message « Tu as terminé les 3 jeux » s'affichait après seulement 2 jeux réellement joués.

**Cause n°1 — mauvais nom de type côté frontend :** dans `renderGame()`, les conditions testaient `game.type === 'SUDOKU_4x4'` et `'SUDOKU_9x9'`, qui ne correspondaient plus aux vraies valeurs d'enum (`SUDOKU_4`, `SUDOKU_9`). Le Sudoku tombait donc dans `renderGeneric()`, qui propose un bouton **« Continuer → »** déclenchant directement `gameWon(game.id, 500)` sans jeu réel.

**Fix n°1 :**
```js
// AVANT
if (game.type === 'SUDOKU_4x4') renderSudoku(game, body, 4);
else if (game.type === 'SUDOKU_9x9') renderSudoku9(game, body);

// APRÈS
if (game.type === 'SUDOKU_4') renderSudoku(game, body, 4);
else if (game.type === 'SUDOKU_9') renderSudoku9(game, body);
```

**Cause n°2 — double comptage dans le `Set` de jeux complétés :** `gameWon()` ajoutait à la fois l'ID Mongo **et** l'index du jeu dans `completedGames` :
```js
completedGames.add(gameId);
const gameIdx = currentSession.findIndex(g => g.id === gameId);
if (gameIdx >= 0) completedGames.add(gameIdx);
```
Si l'index d'un jeu coïncidait avec l'ID d'un autre jeu déjà complété (collision de valeurs dans un même `Set`), `completedGames.size` dépassait artificiellement `currentSession.length` après seulement 2 jeux.

**Fix n°2 :**
```js
// gameWon() — ne garder que l'ID
function gameWon(gameId, score) {
  clearInterval(timerInterval);
  completedGames.add(gameId);   // plus d'ajout d'index
  sessionScore += score;
  ...
}

// renderSessionGrid() — ne vérifier que l'ID
const done = completedGames.has(g.id);   // plus de .has(i)
```

---

### 3. Tour de Hanoï — empilement visuel inversé

**Symptôme :** le plus petit disque apparaissait en bas de la pile au lieu du sommet, et cliquer sur une tige déplaçait le disque du bas plutôt que celui du dessus.

**Cause :** dans `renderPegs()`, le rendu DOM mélangeait `.reverse()` et `insertBefore(d, cont.firstChild)`, ce qui inversait l'ordre d'affichage par rapport à l'ordre logique de la pile (`pegs[pi]`, ordonnée base → sommet, soit grand → petit).

**Fix :** conserver `.reverse()` (on parcourt petit → grand pour l'affichage) mais utiliser `appendChild` au lieu de `insertBefore` :
```js
// AVANT
[...pegs[pi]].reverse().forEach(size=>{
  const d = document.createElement('div');
  ...
  cont.insertBefore(d, cont.firstChild);
});

// APRÈS
[...pegs[pi]].reverse().forEach(size=>{
  const d = document.createElement('div');
  ...
  cont.appendChild(d);
});
```
Avec `flex-direction: column; justify-content: flex-end`, l'ordre des enfants dans le DOM correspond à l'ordre d'affichage de haut en bas ; le premier enfant ajouté (le plus petit disque, grâce au `.reverse()`) se retrouve donc visuellement en haut, et le dernier ajouté (le plus grand) en bas — cohérent avec la logique de jeu (`from[from.length - 1]` = sommet de pile = disque déplaçable).

---

### 4. Inscription bloquée — `403 Forbidden` au corps vide

**Symptôme :** la création de compte échouait systématiquement avec un `403 Forbidden` et une réponse vide, sans message d'erreur exploitable côté interface.

**Cause :** validation Bean (`@Valid`) sur `RegisterRequest` :
```java
@NotBlank @Size(min = 8)
private String password;
```
Un mot de passe de moins de 8 caractères déclenchait un rejet **avant** d'atteindre le contrôleur, avec un format de réponse Spring (`errors: { password: "..." }`) différent du format `{ message: "..." }` que le frontend savait lire. Résultat : l'erreur réelle n'était jamais affichée à l'utilisateur.

**Fix côté frontend (`submitAuth()`) :**
```js
if (!res.ok) {
  if (data.errors && typeof data.errors === 'object') {
    const messages = Object.values(data.errors).join(' · ');
    throw new Error(messages || `Erreur HTTP ${res.status}`);
  }
  throw new Error(data.message || `Erreur HTTP ${res.status}`);
}
```

**Fix UX complémentaire :** ajout d'une indication visible sous le champ mot de passe :
```html
<p style="font-size:.65rem;color:var(--muted);margin-top:.3rem">8 caractères minimum</p>
```

**Diagnostic qui a permis de trouver la cause exacte** — logs du auth-service :
```
Validation failed for argument [0] ... Field error in object 'registerRequest'
on field 'password': rejected value [testi]; ...
default message [la taille doit être comprise entre 8 et 2147483647]
```

---

### 5. Connexion réussie mais pseudo « undefined » et 401/500 en cascade

**Symptôme :** après connexion, le pseudo affiché en haut à droite restait vide / `undefined`. Toutes les requêtes authentifiées suivantes échouaient : `401 Unauthorized` sur `/api/users/results`, `500 Internal Server Error` sur `/api/leaderboard/submit`.

**Cause :** le endpoint `POST /api/auth/login` était resté un **placeholder de développement** dans `AuthController` :
```java
@PostMapping("/login")
public ResponseEntity<Map> login(@RequestBody LoginRequest req) {
    return ResponseEntity.ok(Map.of("test", "ok"));
}
```
Il ne vérifiait jamais les identifiants et ne générait aucun JWT — confirmé côté navigateur par `RAW RESPONSE: {"test":"ok"}` dans les logs de debug. Le frontend lisait ensuite `data.token` et `data.username`, tous deux `undefined`, et les stockait tels quels dans `localStorage`. Le token étant invalide, chaque appel authentifié ultérieur échouait.

La vraie logique de connexion (vérification BCrypt du mot de passe, génération du JWT) existait déjà et fonctionnait dans `AuthService.login()` — elle n'était simplement jamais appelée par le contrôleur.

**Fix (`AuthController.java`) :**
```java
// AVANT
@PostMapping("/login")
public ResponseEntity<Map> login(@RequestBody LoginRequest req) {
    return ResponseEntity.ok(Map.of("test", "ok"));
}

// APRÈS
@PostMapping("/login")
public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
    return ResponseEntity.ok(
        authService.login(req.getUsername(), req.getPassword())
    );
}
```

Après modification, recompiler et redéployer uniquement le auth-service :
```bash
docker-compose up -d --build auth-service
```

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

Prometheus scrape les endpoints `/actuator/prometheus` de chaque microservice toutes les 15 secondes. Grafana se connecte automatiquement à Prometheus au démarrage et charge le dashboard préconfiguré.

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

Les déploiements Kubernetes configurent des `readinessProbe` et `livenessProbe` sur `/actuator/health` pour chaque service, ce qui permet à Kubernetes de gérer les redémarrages automatiquement.

---

## Décisions techniques et retours d'expérience

### Ce qui a été conservé tel quel

**Base de données par service.** Chaque microservice possède sa propre base de données, conformément au pattern Domain-Driven Design. Cela implique qu'il n'y a pas de jointures entre services : le user-service ne connaît pas MongoDB, le game-service ne connaît pas PostgreSQL. La cohérence entre services est assurée par les événements Kafka.

**JWT stateless.** Le choix d'une authentification sans état côté serveur simplifie le scaling horizontal : n'importe quelle instance du service peut valider un token sans consulter une session centralisée.

**Redis Sorted Sets pour le leaderboard.** L'insertion et la lecture d'un rang se font en O(log n) avec `ZADD` et `ZREVRANK`, quelle que soit la taille du classement. Une solution SQL aurait nécessité un `ORDER BY` avec `RANK()`, moins efficace à grande échelle.

**Multi-stage Docker builds.** Les Dockerfiles compilent avec Maven dans un premier stage, puis copient uniquement le JAR dans une image JRE Alpine. Cela réduit significativement la taille des images finales en excluant Maven, les sources et les dépendances de compilation.

### Ce qui a été modifié en cours de développement

**L'endpoint de soumission du leaderboard.** Dans la conception initiale, le leaderboard était mis à jour exclusivement via Kafka (topic `game-completed`). En pratique, cela suppose que Kafka soit parfaitement opérationnel et que le consumer soit démarré avant la première partie. Pour fiabiliser la démonstration, un endpoint `POST /api/leaderboard/submit` a été ajouté directement sur le leaderboard-service. Le frontend l'appelle après chaque victoire. Le chemin Kafka reste présent et fonctionnel ; les deux mécanismes coexistent.

**La gestion du state de session côté frontend.** Une version intermédiaire stockait à la fois l'identifiant MongoDB et l'index du jeu dans le `Set` des jeux complétés, dans l'idée de fiabiliser la correspondance. En pratique, cela provoquait un comptage incorrect (voir [Journal des bugs #2](#2-session-marquée--complète--après-seulement-2-jeux-sur-3)) : la version retenue ne stocke que l'identifiant MongoDB, qui est la seule clé véritablement unique et stable pour un jeu donné.

**Le Parking Rush.** La première implémentation utilisait un rendu statique avec un bouton "Vérifier la sortie" qui simulait une victoire. Elle a été entièrement remplacée par un moteur avec détection de collision pixel-accurate, drag-and-drop souris et tactile, snap sur la grille et détection automatique de victoire dès que la voiture cible atteint la colonne de sortie.

**`ctx.roundRect()` dans le labyrinthe.** Cette méthode Canvas n'est pas supportée par les versions antérieures de Safari et Firefox. Elle a été remplacée par `ctx.rect()` pour garantir la compatibilité.

**La largeur de la modal.** La modal était initialement limitée à 580px, ce qui rendait le Puzzle Image illisible car ses deux panneaux côte à côte dépassaient. Elle a été élargie à 640px.

**Les valeurs de l'enum `GameType`.** `SUDOKU_4x4` et `SUDOKU_9x9` ont été renommés en `SUDOKU_4` et `SUDOKU_9` côté backend. Ce renommage a nécessité (a) un reseed complet de la collection MongoDB `games` pour purger les anciens documents stockant la valeur textuelle obsolète, et (b) une mise à jour synchronisée du frontend (`renderGame()`) qui comparait encore aux anciennes valeurs (voir [Journal des bugs #1 et #2](#1-niveaux-facile-et-difficile-inaccessibles--500-internal-server-error)).

**L'endpoint `/api/auth/login`.** Resté un placeholder de réponse statique (`{"test":"ok"}`) pendant une partie du développement, le temps de stabiliser le reste du flux d'authentification. Le branchement vers la vraie logique `AuthService.login()` (déjà fonctionnelle et testée) a été fait une fois le reste de la plateforme stabilisé (voir [Journal des bugs #5](#5-connexion-réussie-mais-pseudo--undefined--et-401500-en-cascade)).

### Ce qui a été abandonné

**springdoc sur l'api-gateway.** La gateway utilise Spring Cloud Gateway qui repose sur WebFlux (réactif), alors que les autres services utilisent le stack Web classique (Servlet). springdoc propose deux artefacts distincts : `webmvc-ui` et `webflux-ui`. L'artefact WebFlux a été ajouté mais la documentation de la gateway reste moins riche que celle des autres services, car la gateway ne définit pas de controllers annotés : ses routes sont déclarées en YAML.

**Les images dans le Puzzle Image.** La version initiale prévoyait d'utiliser de vraies images chargées depuis `/assets/`. Cela aurait nécessité un serveur de fichiers statiques supplémentaire. Pour simplifier le déploiement et éviter une dépendance externe, les pièces ont été remplacées par des couleurs vives et des formes géométriques générées en CSS/SVG pur, ce qui fonctionne sans aucune ressource externe.

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
|   `-- grafana/              Dashboard préconfiguré, provisioning automatique
|-- k8s/                      Manifests Kubernetes (namespace, secrets, deployments)
|-- scripts/                  Initialisation PostgreSQL multi-base
|-- docker-compose.yml        Environnement de développement complet (15 conteneurs)
`-- .github/workflows/        Pipeline CI/CD GitHub Actions
```