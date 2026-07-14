# BFrost — University Club Platform (Backend)

BFrost is a social platform for university clubs. It provides club discovery and
membership management, a social feed with posts, comments, reactions and polls,
club events with RSVPs, direct messaging, and notifications, backed by a JWT and
OAuth2 authentication system.

This repository contains the backend API. The companion frontend repository is
linked below.

## Team

| Name                   | GitHub                          | LinkedIn                                             |
|------------------------|---------------------------------|------------------------------------------------------|
| Ayhan Aghayev          | https://github.com/AyhanAghayev | https://www.linkedin.com/in/ayhan-agayev/            |
| Abdulvahhab Alaskarov  | https://github.com/BillerPlay   | https://www.linkedin.com/in/billerplay/              |
| Yunis Sadiq            | https://github.com/YunisSadig   | https://www.linkedin.com/in/yunis-sadiq-5a99b930b/   |
| Islam Samadov          | https://github.com/IslamSamadov | https://www.linkedin.com/in/islam-samadov-2ba9b2305/ |
| Qurbanali Qurbanaliyev | https://github.com/gurban200OK  | www.linkedin.com/in/gurbanali-gurbanaliyev-563715420 |


## Project Links

| Resource | Link                                                    |
|---|---------------------------------------------------------|
| Backend repository | [link](https://github.com/AyhanAghayev/BFrost-Backend)  |
| Frontend repository | [link](https://github.com/AyhanAghayev/BFrost-Frontend) |
| Live application | [link](https://b-frost.vercel.app)                      |
| API documentation (Swagger UI) | `/swagger-ui.html` on the deployed backend              |

## Technologies Used

- Java 17
- Spring Boot 3.3 (Web, Security, Data JPA, Validation, WebSocket, Actuator)
- Spring Security OAuth2 Client (Google login)
- PostgreSQL
- Flyway (schema migrations)
- JJWT (JSON Web Tokens)
- Lombok
- springdoc-openapi (Swagger UI / OpenAPI 3)
- Maven
- JUnit 5, Mockito, AssertJ, Spring Boot Test, Spring Security Test

## Architecture Overview

The backend is organized by feature (package-by-domain) rather than by
technical layer, with each package following the same internal structure:

```
com.bfrost
├── auth/          Registration, login, JWT issuance, refresh tokens, Google OAuth2
├── user/           Profiles, follow/unfollow, account settings
├── club/           Club CRUD, membership, roles, join requests
├── post/           Feed, posts, comments, reactions, polls
├── event/         Club events, RSVPs
├── chat/           Direct conversations, WebSocket (STOMP) messaging, presence
├── notification/   Follow/like/comment/join-request notifications
├── storage/        File upload storage
├── config/         Security, WebSocket and OpenAPI configuration
└── common/         Shared exceptions, cursor pagination, global error handling
```

Within each domain package: `*Controller` classes expose REST endpoints,
`*Service` classes hold business logic and transaction boundaries, `*Repository`
interfaces are Spring Data JPA repositories, entities model the persisted data,
and a `dto` sub-package holds request and response payloads — entities are
never returned directly from an endpoint.

Key design points:

- Authentication is stateless (`SessionCreationPolicy.STATELESS`). A short-lived
  JWT access token authorizes API requests; a longer-lived refresh token is
  stored in an httpOnly cookie scoped to `/api/v1/auth`.
- Google OAuth2 login is integrated into the same JWT session model: a
  successful OAuth2 login provisions or links a local account and then issues
  the same access/refresh token pair a password login would, so the rest of
  the API is unaffected by how the user authenticated.
- Real-time chat uses STOMP over WebSocket (SockJS fallback available). The
  STOMP CONNECT frame is authenticated with the same JWT used for REST calls,
  and SUBSCRIBE frames to club topics are authorized against club membership.
- List endpoints (feed, posts, messages) use cursor-based pagination rather
  than offset pagination.
- Database schema changes are managed exclusively through Flyway migrations
  in `src/main/resources/db/migration`; Hibernate DDL auto-generation is
  disabled (`ddl-auto: validate`).

## Backend Documentation

### Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/bfrost` | JDBC URL of the PostgreSQL database |
| `DB_USERNAME` | No | `bfrost` | Database username |
| `DB_PASSWORD` | No | `bfrost` | Database password |
| `PORT` | No | `8080` | HTTP port the application listens on |
| `JWT_SECRET` | Yes in production | dev-only fallback value | HMAC signing key for access tokens (minimum 32 bytes) |
| `UPLOAD_DIR` | No | `uploads` | Local filesystem directory for uploaded files |
| `BASE_URL` | No | `http://localhost:8080` | Public base URL used when building upload URLs |
| `GOOGLE_CLIENT_ID` | Only for Google login | empty | OAuth2 client ID registered with Google |
| `GOOGLE_CLIENT_SECRET` | Only for Google login | empty | OAuth2 client secret registered with Google |
| `OAUTH_SUCCESS_REDIRECT` | No | `http://localhost:3000/oauth/callback` | Frontend URL the user is redirected to after a successful Google login, with the access token appended as a query parameter |
| `OAUTH_FAILURE_REDIRECT` | No | `http://localhost:3000/login` | Frontend URL the user is redirected to after a failed Google login |

If `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` are not set, the application
still starts normally; the Google login endpoint is simply not functional
until real credentials are supplied.

### Installation

Prerequisites:

- Java 17 JDK
- PostgreSQL 14 or later
- Maven 3.8 or later

Steps:

```bash
# 1. Create the database and role
psql -U postgres -c "CREATE DATABASE bfrost; CREATE USER bfrost WITH PASSWORD 'bfrost'; GRANT ALL ON DATABASE bfrost TO bfrost;"

# 2. Set required environment variables (or export them in your shell/profile)
export JWT_SECRET="a-long-random-production-secret-at-least-32-bytes"

# 3. Build
mvn clean package
```

Flyway applies all schema migrations automatically on application startup; no
manual migration step is required.

### Development

Run the application locally with the `dev` profile, which relaxes the JWT
secret requirement and enables SQL logging:

```bash
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

The API is served at `http://localhost:8080`. Interactive API documentation is
available at `http://localhost:8080/swagger-ui.html` once the application is
running.

Run the test suite:

```bash
mvn test
```

The test suite includes JUnit 5 / Mockito unit tests for the service layer and
Spring Boot Test / MockMvc integration tests for the controller layer, plus a
STOMP-level WebSocket integration test. The integration tests require a
running PostgreSQL instance reachable with the same connection settings as
above.

Build a runnable jar:

```bash
mvn clean verify
java -jar target/bfrost-backend-0.0.1-SNAPSHOT.jar
```

### Authentication

The API supports two ways to obtain a session, both of which converge on the
same JWT access/refresh token pair.

**Username and password**

1. `POST /api/v1/auth/register` with `username`, `email`, `password`,
   `displayName` creates an account and returns an access token in the
   response body; a refresh token is set as an httpOnly cookie.
2. `POST /api/v1/auth/login` with `email` and `password` authenticates an
   existing account the same way.
3. Send the access token on subsequent requests as
   `Authorization: Bearer <token>`. Access tokens expire after 15 minutes.
4. `POST /api/v1/auth/refresh` reads the refresh token cookie and issues a new
   access token. Refresh tokens expire after 7 days.
5. `POST /api/v1/auth/logout` revokes the caller's refresh tokens and clears
   the cookie.

**Google OAuth2**

1. Redirect the user's browser to `GET /oauth2/authorization/google`.
2. After the user authenticates with Google, the backend either creates a new
   account (first-time login) or links the Google identity to an existing
   account matched by email, then redirects to `OAUTH_SUCCESS_REDIRECT` with
   the access token as a query parameter, and sets the refresh token cookie
   the same way the password flow does. A failed attempt redirects to
   `OAUTH_FAILURE_REDIRECT` instead.

**Public endpoints**

The following do not require authentication: `POST /api/v1/auth/register`,
`POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `GET /api/v1/clubs`
and its sub-resources (club listing, detail, search), `GET /api/v1/users/*`
profile lookups (other than `/me`), and the Swagger/health endpoints. All
other endpoints require a valid access token.

### Endpoint Documentation

All REST endpoints are prefixed with `/api/v1`. Full request/response schemas
are available through the generated OpenAPI documentation at
`/v3/api-docs` and the accompanying Swagger UI.

**Auth** (`/api/v1/auth`)

| Method | Path | Description |
|---|---|---|
| POST | `/register` | Create an account |
| POST | `/login` | Authenticate with email and password |
| POST | `/refresh` | Exchange a refresh token cookie for a new access token |
| POST | `/logout` | Revoke refresh tokens and clear the session cookie |

**Users** (`/api/v1/users`)

| Method | Path | Description |
|---|---|---|
| GET | `/me` | Current user's profile |
| GET | `/me/friends` | Mutual followers eligible for direct messaging |
| GET | `/{username}` | Public profile by username |
| PATCH | `/{userId}` | Update a profile (owner only) |
| POST | `/me/password` | Change password |
| POST | `/me/email` | Change email |
| DELETE | `/me` | Delete the current account |
| GET | `/me/notification-preferences` | Read notification preferences |
| PATCH | `/me/notification-preferences` | Update notification preferences |
| GET | `/{userId}/followers` | List followers |
| GET | `/{userId}/following` | List followed users |
| POST | `/{userId}/follow` | Follow a user |
| DELETE | `/{userId}/follow` | Unfollow a user |
| GET | `/search?q=` | Search users |

**Clubs** (`/api/v1/clubs`)

| Method | Path | Description |
|---|---|---|
| GET | `/` | List clubs |
| GET | `/{slug}` | Club detail by slug or id |
| GET | `/search?q=` | Search clubs |
| POST | `/` | Create a club |
| PATCH | `/{slug}` | Update a club (moderator or owner) |
| POST | `/{clubId}/join` | Join a public club, or request to join a private one |
| DELETE | `/{clubId}/join` | Leave a club |
| GET | `/{clubId}/members` | List members (public clubs, or members of a private club) |
| GET | `/{clubId}/requests` | List pending join requests (moderator or owner) |
| PATCH | `/{clubId}/members/{userId}/role` | Change a member's role (owner only) |
| DELETE | `/{clubId}/members/{userId}` | Remove a member |
| POST | `/{clubId}/transfer/{userId}` | Transfer club ownership |
| POST | `/{clubId}/requests/{requestId}/approve` | Approve a join request |
| POST | `/{clubId}/requests/{requestId}/reject` | Reject a join request |

**Trending** (`/api/v1/trending`)

| Method | Path | Description |
|---|---|---|
| GET | `/` | Trending club tags |

**Posts** (`/api/v1`)

| Method | Path | Description |
|---|---|---|
| GET | `/feed` | Current user's home feed (cursor paginated) |
| POST | `/posts` | Create a post (text, image, link, question, or poll) |
| GET | `/posts/saved` | List saved posts |
| GET | `/posts/{postId}` | Post detail |
| DELETE | `/posts/{postId}` | Delete a post (author only) |
| POST | `/posts/{postId}/react?type=` | Like or dislike a post |
| POST | `/posts/{postId}/save` | Save a post |
| DELETE | `/posts/{postId}/save` | Unsave a post |
| GET | `/posts/{postId}/comments` | List comments |
| POST | `/posts/{postId}/comments` | Add a comment |
| POST | `/posts/{postId}/poll/{optionId}/vote` | Vote on a poll option |
| GET | `/users/{userId}/posts` | Posts on a user's page (cursor paginated) |
| GET | `/clubs/{clubSlug}/posts` | Posts on a club's page (cursor paginated) |

**Events** (`/api/v1`)

| Method | Path | Description |
|---|---|---|
| GET | `/events` | Current user's upcoming events |
| GET | `/clubs/{clubSlug}/events` | A club's events |
| POST | `/clubs/{clubId}/events` | Create an event (moderator or owner) |
| GET | `/events/{eventId}` | Event detail |
| PATCH | `/events/{eventId}` | Update an event |
| DELETE | `/events/{eventId}` | Delete an event |
| GET | `/events/{eventId}/rsvps` | List attendees (moderator or owner) |
| POST | `/events/{eventId}/rsvp?status=` | RSVP to an event |

**Chat** (`/api/v1/conversations`)

| Method | Path | Description |
|---|---|---|
| GET | `/` | List conversations |
| GET | `/unread-count` | Unread message count |
| POST | `/{conversationId}/read` | Mark a conversation read |
| POST | `/with/{otherUserId}` | Get or create a conversation with a mutual follower |
| GET | `/{conversationId}/messages` | List messages (cursor paginated) |
| POST | `/{conversationId}/messages` | Send a message over REST |
| POST | `/{conversationId}/clear` | Clear a conversation for the current user |

**Notifications** (`/api/v1/notifications`)

| Method | Path | Description |
|---|---|---|
| GET | `/` | List notifications |
| GET | `/unread-count` | Unread notification count |
| POST | `/mark-all-read` | Mark all notifications read |

**Storage** (`/api/v1/upload`)

| Method | Path | Description |
|---|---|---|
| POST | `/{folder}` | Upload a file (multipart, allow-listed folders only) |

**Presence** (`/api/v1/presence`)

| Method | Path | Description |
|---|---|---|
| GET | `/` | User ids currently connected to chat |

**WebSocket (STOMP)**

| Endpoint | Description |
|---|---|
| `ws://host/ws` | SockJS-fallback STOMP endpoint |
| `ws://host/ws-native` | Native WebSocket STOMP endpoint |

CONNECT frames must include an `Authorization: Bearer <token>` native header.

| Direction | Destination | Description |
|---|---|---|
| SEND | `/app/chat.send` | Send a chat message |
| SUBSCRIBE | `/user/queue/messages` | Receive chat messages addressed to the current user |
| SUBSCRIBE | `/topic/clubs/{clubId}/...` | Receive club broadcasts (subscription is rejected unless the user is a member of the club) |

