Seat-Booking-API
|Entité             |Rôle |
|---------------|---------------------------------------------------|
|**Event**    |Un événement avec une capacité maximale |
|**Seat**      |Un siège appartenant à un événement |
|**Customer** |Le client qui réserve |
|**Reservation**|Le lien entre un client et un siège, avec un statut|

Les statuts d’une Reservation : Une réservation peut être dans 3 états :
PENDING → CONFIRMED
↓
EXPIRED

∙ PENDING : siège bloqué temporairement pendant le paiement
∙ CONFIRMED : paiement complété, siège définitivement réservé
∙ EXPIRED : délai dépassé, siège redevenu disponible

Étape 2 — Les règles métier
1. Quand refuse-t-on une réservation ?
∙ Si l’événement est complet (plus de sièges disponibles)
∙ Si le siège est déjà en PENDING ou CONFIRMED
2. Deux clients en même temps sur le même siège ?
∙ C’est le problème de concurrence — on doit s’assurer qu’un seul gagne
∙ Solution : verrou en base de données (on en parlera plus tard)
3. Délai d’expiration ?
∙ Je décide : 15 minutes — c’est cohérent avec ce qu’on voit sur Ticketmaster, la SNCF,…

Un siège à bien son propre état, indépendamment de la réservation :

AVAILABLE → HELD → BOOKED
↓
AVAILABLE (si expiration)

∙ AVAILABLE : libre, réservable
∙ HELD : bloqué temporairement (pendant les 15 min)
∙ BOOKED : définitivement réservé

 
Le modèle complet

Event
└── Seat (AVAILABLE / HELD / BOOKED)
└── Reservation (PENDING / CONFIRMED / EXPIRED)

C’est plus propre ainsi : le Seat porte l’état opérationnel, la Reservation porte l’historique et le lien avec le Customer.

Étape 3 — Les endpoints API
POST /events → créer un événement
GET /events/{id} → détails + sièges disponibles
POST /events/{eventId}/reservations → réserver un siège (→ PENDING)
POST /reservations/{id}/confirm → confirmer le paiement (→ CONFIRMED)
DELETE /reservations/{id} → annuler une réservation

Étape 4 — La structure du projet Spring Boot
Voici l’architecture en couches que je recommande :
src/main/java/com/carrefour/kata/
│
├── domain/
│ ├── Event.java
│ ├── Seat.java
│ ├── Customer.java
│ └── Reservation.java
│
├── repository/
│ ├── EventRepository.java
│ ├── SeatRepository.java
│ └── ReservationRepository.java
│
├── service/
│ └── ReservationService.java ← toute la logique métier ici
│
├── controller/
│ └── ReservationController.java
│
└── scheduler/
└── ReservationExpirationJob.java ← gère les 15 min d'expiration

Étape 5 — Par où commencer concrètement ?
L’ordre idéal :
1. Générer le projet sur start.spring.io avec :
∙ Spring Web
∙ Spring Data JPA
∙ H2 Database (base en mémoire, simple pour un kata)
∙ Spring Boot DevTools
2. Coder les entités (domain/)
3. Coder les repositories
4. Coder le service avec la logique métier
5. Coder le controller
6. Ajouter le scheduler pour l’expiration

L’architecture choisie : Architecture en couches (Layered Architecture)

Les 4 couches et leur rôle
1. Controller (couche présentation)
∙ Reçoit les requêtes HTTP
∙ Valide le format des données entrantes
∙ Retourne les réponses HTTP
∙ Ne contient aucune logique métier
2. Service (couche métier)
∙ C’est le cerveau de l’application
∙ Contient toutes les règles métier :
∙ Est-ce que le siège est disponible ?
∙ Est-ce que l’événement est complet ?
∙ Gérer l’expiration des 15 min
3. Repository (couche accès données)
∙ Dialogue avec la base de données
∙ Aucune logique métier, juste des requêtes
∙ Avec Spring Data JPA, c’est quasi automatique
4. Domain (couche modèle)
∙ Représente les objets métier purs
∙ Event, Seat, Reservation, Customer

Pourquoi cette architecture ?

|Raison |Explication |
|----------------------------------|-----------------------------------------------------------------|
|**Séparation des responsabilités**|Chaque couche a un rôle précis, on ne mélange pas tout |
|**Testabilité** |On peut tester le Service sans démarrer HTTP ni la BDD |
|**Lisibilité** |N’importe quel dev Java comprend immédiatement la structure |
|**Standard Spring Boot** |C’est ce qu’on attend dans un contexte entreprise comme Carrefour|

Pourquoi pas une autre architecture ?
Tu pourrais entendre parler de Hexagonale / Clean Architecture — c’est plus avancé, plus découplé, mais :
∙ Plus complexe à mettre en place en 2h
∙ OverSkill pour un kata
∙ Risque de ne pas finir le MVP
La layered architecture est le bon compromis simplicité / professionnalisme pour ce contexte. Tu dois pouvoir dire :

“J’ai choisi une architecture en couches classique car elle est adaptée au contexte, lisible, et me permettait de livrer un MVP complet en 2h. Si le projet grandissait, j’envisagerais une architecture hexagonale pour mieux isoler le domaine.”

Remarques importantes
∙ J’ai simplifié Customer à juste un customerEmail dans Reservation — suffisant pour un kata, évite une entité inutile
∙ expiresAt est la clé pour le mécanisme d’expiration automatique
∙ Les @ManyToOne / @OneToMany définissent les relations en base

Question importante avant d’avancer
Dans le ReservationService, quand on crée une réservation, il faut gérer la concurrence — deux clients sur le même siège simultanément.
La concurrence — Optimistic Locking
Le problème
Client A → vérifie siège → AVAILABLE 
Client B → vérifie siège → AVAILABLE 
Client A → réserve siège 
Client B → réserve siège  ← PROBLÈME, double réservation !

La solution : @Version
JPA propose un mécanisme natif appelé Optimistic Locking :

@Entity
public class Seat {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
private String seatNumber;
@Enumerated(EnumType.STRING)
private SeatStatus status = SeatStatus.AVAILABLE;
@ManyToOne
private Event event;
@Version
private Long version; // ← ajoute juste ça
}

Client A lit Seat → version = 1
Client B lit Seat → version = 1
Client A sauvegarde → version devient 2 
Client B sauvegarde → version attendue = 1, mais en BDD = 2
→ JPA lance OptimisticLockException 

Le deuxième client reçoit une erreur → on lui retourne un 409 Conflict.

Pourquoi Optimistic et pas Pessimistic ?
| |Optimistic |Pessimistic |
|---------------|----------------------------------|-------------------------------------|
|**Principe** |On tente, on gère le conflit après|On verrouille la ligne dès la lecture|
|**Performance**|Meilleure (pas de verrou) |Moins bonne |
|**Usage** |Conflits rares |Conflits fréquents |
|**Kata** | Parfait |Overkill |

Pour une billetterie, les conflits sont rares → Optimistic est le bon choix.

Ce que tu diras en entretien
“J’ai utilisé l’Optimistic Locking de JPA via @Version pour gérer la concurrence. En cas de conflit, le service retourne un 409. C’est adapté car les collisions simultanées sur un même siège restent rares.”

Spring Data JPA génère la requête SQL automatiquement à partir du nom de la méthode. Pas besoin d’écrire du SQL.



Ce que tu diras en entretien
“J’ai utilisé @Scheduled de Spring pour scanner toutes les 60 secondes les réservations PENDING expirées. C’est simple et suffisant pour un kata. En production, j’aurais envisagé un job plus robuste avec Quartz Scheduler ou un mécanisme event-driven avec Kafka pour éviter le polling.”

Vue d’ensemble de ce qu’on a construit

HTTP Request
↓
Controller → valide le format
↓
Service → applique les règles métier
↓
Repository → accès base de données
↓
H2 Database

Structure des tests — pattern AAA
Chaque test suit le pattern Arrange / Act / Assert (Given / When / Then) :

GIVEN → je prépare le contexte
WHEN → j'exécute l'action
THEN → je vérifie le résultat

## Stratégie des commits Git

Ne fais pas un seul gros commit. Découpe proprement :

```bash
git commit -m "feat: add domain entities (Event, Seat, Reservation)"
git commit -m "feat: add reservation hold and confirm logic"
git commit -m "feat: add expiration scheduler"
git commit -m "feat: add REST controller and exception handling"
git commit -m "test: add ReservationService unit tests"
git commit -m "docs: add README with architecture decisions"


Tu as tout ce qu’il faut pour livrer un kata solide et défendable en entretien. Bonne chance !  

"J'ai choisi une architecture en couches car elle correspond bien à la taille et la complexité de ce projet. C'est un contexte borné avec peu de domaines métier, donc introduire une architecture hexagonale aurait été de l'over-engineering.
Ce qui compte pour moi c'est que les couches respectent bien leur responsabilité : le controller ne contient aucune logique métier, le service ne connaît pas HTTP, le repository ne fait que de la persistance. C'est ça le vrai principe SOLID appliqué ici, pas le pattern architectural en lui-même.
Si le projet devait évoluer vers plusieurs bounded contexts ou nécessiter des ports d'entrée multiples (REST + messaging par exemple), je migrerais vers une architecture hexagonale. Mais pour ce kata, la lisibilité et la simplicité l'emportent."

Pourquoi un package dto séparé ?
∙ DTO (Data Transfer Object) = objets qui transitent entre le client et l’API
∙ Ils ne sont pas des entités JPA (pas de @Entity)
∙ Séparer les DTOs des entités domaine évite d’exposer directement ta base de données à l’extérieur

Les commits suivent la convention Conventional Commits :

|Préfixe |Usage |
|----------|-------------------------------------------------------|
|`feat` |Nouvelle fonctionnalité |
|`fix` |Correction de bug |
|`chore` |Tâche technique sans impact métier (renommage, config…)|
|`docs` |Documentation |
|`test` |Ajout/modification de tests |
|`refactor`|Restructuration du code sans changer le comportement |

Questions types en entretien technique

1. Architecture
Q : Pourquoi une architecture en couches ?
J’ai choisi une architecture en couches classique car elle est lisible, standard en contexte Spring Boot entreprise, et me permettait de livrer un MVP complet en 2h. Si le projet grandissait, j’envisagerais une architecture hexagonale pour mieux isoler le domaine métier de l’infrastructure.

Q : Pourquoi avoir séparé le statut du Seat et le statut de la Reservation ?
Ce sont deux cycles de vie différents. Le Seat porte l’état opérationnel en temps réel (est-ce que cette place est physiquement disponible ?). La Reservation porte l’historique métier (qu’est-ce qui s’est passé avec ce client ?). Les mélanger aurait créé du couplage inutile.

2. Concurrence
Q : Comment gères-tu deux clients qui réservent le même siège simultanément ?
J’utilise l’Optimistic Locking de JPA via @Version sur l’entité Seat. Chaque modification incrémente un numéro de version. Si deux clients lisent la même version et que le premier sauvegarde, le deuxième reçoit une OptimisticLockException que je transforme en 409 Conflict.

Q : Pourquoi Optimistic et pas Pessimistic Locking ?
Le Pessimistic Locking pose un verrou sur la ligne dès la lecture, ce qui bloque tous les autres accès. C’est adapté quand les conflits sont fréquents. Dans une billetterie, deux clients sur le même siège au même milliseconde est rare — l’Optimistic Locking est plus performant et suffisant.

3. Expiration
Q : Comment gères-tu l’expiration des réservations ?
Un @Scheduled job tourne toutes les 60 secondes. Il cherche toutes les réservations en statut PENDING dont le expiresAt est dépassé, les passe en EXPIRED, et remet les sièges en AVAILABLE.

Q : Pourquoi 15 minutes ?
C’est une décision métier que j’ai prise en m’inspirant des standards de l’industrie — SNCF, Ticketmaster utilisent des durées similaires. Le kata ne précisait pas ce délai, j’ai documenté ce choix dans le README.

Q : Quels sont les limites du Scheduler en production ?
Le @Scheduled fait du polling toutes les 60 secondes — ce n’est pas temps réel. En production j’envisagerais soit Quartz Scheduler pour plus de robustesse, soit un mécanisme event-driven avec Kafka et des messages à TTL pour déclencher l’expiration exactement à l’échéance.

4. Base de données
Q : Pourquoi H2 ?
C’est une base en mémoire, zéro configuration, parfaite pour un kata. En production je migrerais vers PostgreSQL ou MySQL avec les mêmes entités JPA — seule la configuration change.

Q : Comment migrerais-tu vers PostgreSQL ?
Je remplacerais la dépendance H2 par le driver PostgreSQL dans le pom.xml, et je mettrais à jour application.properties avec l’URL de connexion. Le code métier ne changerait pas grâce à l’abstraction JPA.

5. Tests
Q : Qu’est-ce que tu as testé et pourquoi ?
J’ai testé la couche Service car c’est là que se trouve la logique métier. J’ai utilisé Mockito pour mocker les repositories et tester les cas nominaux et les cas d’erreur — siège indisponible, réservation expirée, réservation déjà confirmée.

Q : Qu’est-ce que tu aurais ajouté avec plus de temps ?
Des tests d’intégration avec @SpringBootTest et H2 pour tester le flux complet HTTP → BDD. Et des tests de concurrence pour valider l’Optimistic Locking sous charge.

6. Améliorations possibles
Q : Qu’est-ce que tu améliorerais si c’était un vrai projet ?
Plusieurs choses :
∙ Remplacer H2 par PostgreSQL
∙ Ajouter une authentification (Spring Security + JWT)
∙ Remplacer le Scheduler par un mécanisme event-driven (Kafka)
∙ Ajouter une pagination sur les endpoints GET
∙ Migrer vers une architecture hexagonale pour mieux isoler le domaine
∙ Ajouter des tests d’intégration et des tests de charge

Enrichir la BDD au démarrage
La meilleure approche : un fichier data.sql que Spring Boot charge automatiquement.
Étape 1 — Crée le fichier data.sql
Dans src/main/resources/ :
src/main/resources/
├── application.properties
└── data.sql ← crée ce fichier

Étape 2 — Contenu du data.sql
-- Créer des événements
INSERT INTO EVENT (id, name, total_capacity) VALUES (1, 'Concert Paris', 5);
INSERT INTO EVENT (id, name, total_capacity) VALUES (2, 'Match Football', 3);
-- Créer les sièges pour Concert Paris
INSERT INTO SEAT (id, seat_number, status, event_id, version) VALUES (1, 'S1', 'AVAILABLE', 1, 0);
INSERT INTO SEAT (id, seat_number, status, event_id, version) VALUES (2, 'S2', 'AVAILABLE', 1, 0);
INSERT INTO SEAT (id, seat_number, status, event_id, version) VALUES (3, 'S3', 'AVAILABLE', 1, 0);
INSERT INTO SEAT (id, seat_number, status, event_id, version) VALUES (4, 'S4', 'AVAILABLE', 1, 0);
INSERT INTO SEAT (id, seat_number, status, event_id, version) VALUES (5, 'S5', 'AVAILABLE', 1, 0);
-- Créer les sièges pour Match Football
INSERT INTO SEAT (id, seat_number, status, event_id, version) VALUES (6, 'S1', 'AVAILABLE', 2, 0);
INSERT INTO SEAT (id, seat_number, status, event_id, version) VALUES (7, 'S2', 'AVAILABLE', 2, 0);
INSERT INTO SEAT (id, seat_number, status, event_id, version) VALUES (8, 'S3', 'AVAILABLE', 2, 0);


Étape 3 — Mettre à jour application.properties
spring.application.name=seat-booking-api
# H2
spring.datasource.url=jdbc:h2:mem:kata
spring.datasource.driverClassName=org.h2.Driver
spring.h2.console.enabled=true
# JPA
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=create-drop
# Charger data.sql après la création des tables
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always

Étape 4 — Les cas à tester avec Postman
Cas 1 — Voir un événement
GET http://localhost:8080/api/events/1
Cas 2 — Réserver un siège
POST http://localhost:8080/api/events/1/reservations
Content-Type: application/json
{
"seatId": 1,
"customerEmail": "john@example.com"
}

Cas 3 — Confirmer le paiement
POST http://localhost:8080/api/reservations/1/confirm
Cas 4 — Réserver un siège déjà pris
POST http://localhost:8080/api/events/1/reservations
Content-Type: application/json
{
"seatId": 1,
"customerEmail": "autre@example.com"
}
→ doit retourner 409 Conflict
Cas 5 — Annuler une réservation
DELETE http://localhost:8080/api/reservations/1
Cas 6 — Vérifier la BDD via H2 Console
http://localhost:8080/h2-console
JDBC URL : jdbc:h2:mem:kata
3. Tests d’intégration
Concept
Contrairement aux tests unitaires qui mockent tout, les tests d’intégration testent le flux complet :

HTTP Request → Controller → Service → Repository → H2 Database


Pipeline CI/CD GitLab
Crée le fichier .gitlab-ci.yml à la racine du projet :

image: maven:3.9-eclipse-temurin-21

stages:
- build
- test
- package

variables:
MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

cache:
paths:
- .m2/repository/

#  Étape 1 — Compilation
build:
stage: build
script:
- mvn compile
only:
- main
- merge_requests

#  Étape 2 — Tests
test:
stage: test
script:
- mvn test
artifacts:
when: always
reports:
junit:
- target/surefire-reports/TEST-*.xml
only:
- main
- merge_requests

#  Étape 3 — Package
package:
stage: package
script:
- mvn package -DskipTests
artifacts:
paths:
- target/*.jar
expire_in: 1 week
only:
- main


Ce que fait chaque étape



|Étape |Commande |Rôle |
|-----------|-------------|-----------------------------------------------|
|**build** |`mvn compile`|Vérifie que le code compile |
|**test** |`mvn test` |Lance tous les tests unitaires et d’intégration|
|**package**|`mvn package`|Génère le JAR final |

Le cache .m2

cache:
paths:
- .m2/repository/


Évite de retélécharger toutes les dépendances Maven à chaque pipeline → beaucoup plus rapide.

Les artifacts

artifacts:
reports:
junit:
- target/surefire-reports/TEST-*.xml

GitLab affiche directement les résultats des tests dans l’interface — tu vois quels tests passent et lesquels échouent.

Commit et push

git add .gitlab-ci.yml
git commit -m "ci: add GitLab CI/CD pipeline"
git push origin main

Vérifier que la pipeline tourne

GitLab → ton repo → CI/CD → Pipelines


Tu verras les 3 étapes s’exécuter en temps réel  


