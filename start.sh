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

# 1. Lancer Docker Compose
info "Démarrage de l'infrastructure Docker (build + up)..."
docker-compose up -d --build


# 2. Attendre que la gateway réponde "UP"
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


# 3. Vérifier l'état de la banque de jeux MongoDB
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


# 4. Récapitulatif
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