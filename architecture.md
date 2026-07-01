# Spring Boot Backend — Architecture

## Overview

Java 21 / Spring Boot 4.x REST API. Owns all business logic, the PostgreSQL
database, S3 presigned URL generation, and the async face recognition job queue.
No image bytes ever transit this service — all file I/O goes directly between the
client and S3, or between the Python service and S3.

---

## Package Layout

```
com.photowah.photowah_be/
  config/       SecurityConfig, AsyncConfig, S3Config, SwaggerConfig
  controller/   AuthController, AgencyController, EventController,
                PhotoController, SelfieSearchController
  dto/          Request / response DTOs (grouped by domain)
  entity/       JPA entities
  enums/        RecognitionStatus, Plan, EventStatus
  exception/    GlobalExceptionHandler + custom exceptions
  mapper/       MapStruct mappers
  repository/   Spring Data JPA repositories
  security/     JwtUtil, JwtFilter, UserDetailsServiceImpl, PhotowahPrincipal
  service/      Business logic services
```

---

## REST API

### Authentication  `POST /api/auth/**`  (no JWT required)

| Method | Path | Body | Response | Notes |
|--------|------|------|----------|-------|
| POST | `/api/auth/register` | `{ agencyName, photographerName, email, password }` | `AuthResponse` | Creates Agency + Photographer + Subscription (FREE plan, 5 GB / 5 events) |
| POST | `/api/auth/login` | `{ email, password }` | `AuthResponse` | BCrypt password check |
| POST | `/api/auth/google` | `{ idToken }` | `AuthResponse` | Verifies Google ID token via `oauth2.googleapis.com/tokeninfo`; auto-creates account on first login |

`AuthResponse`: `{ token, email, photographerName, agencyId, plan }`

JWT claims: `email`, `role` (ROLE_PHOTOGRAPHER), `agencyId`, `photographerId`  
Extracted into `PhotowahPrincipal` record by `JwtFilter` on every authenticated request.

---

### Agency  `GET /api/agency/**`  (JWT required)

| Method | Path | Response | Notes |
|--------|------|----------|-------|
| GET | `/api/agency/me` | `AgencyResponse` | Agency profile for the caller |
| GET | `/api/agency/stats` | `StorageStatsResponse` | `{ storageUsedMb, storageLimitMb, storageUsedPercent, eventsUsed, eventsLimit }` |

---

### Events  `/api/events/**`

| Method | Path | Auth | Response | Notes |
|--------|------|------|----------|-------|
| POST | `/api/events` | JWT | `EventResponse` | Creates event; checks `eventsUsed < eventsLimit` (403 if exceeded) |
| GET | `/api/events` | JWT | `List<EventSummaryResponse>` | All events for the caller's photographer ID |
| GET | `/api/events/{eventId}` | JWT | `EventResponse` | Must belong to caller |
| PATCH | `/api/events/{eventId}/archive` | JWT | 204 | Sets status = ARCHIVED |
| GET | `/api/events/public/{token}` | None | `EventResponse` | Public lookup by UUID shareable token |

`EventResponse` includes: `id, name, eventDate, location, status, shareableToken, photoCount, createdAt`

---

### Photos  `/api/events/**`

Upload is a 3-step protocol — no bytes touch this server:

**Step 1 — Initiate**

| Method | Path | Auth | Body | Response |
|--------|------|------|------|----------|
| POST | `/api/events/{eventId}/photos/initiate` | JWT | `{ filename, contentType, fileSizeKb }` | `{ photoId, presignedUploadUrl, s3Key, expiresInSeconds }` |

- Checks event ownership and ACTIVE status
- Checks `storageUsedMb + newFileMb <= storageLimitMb` (403 if exceeded)
- Creates `Photo` row (status = PENDING)
- Increments `agency.storageUsedMb`
- Returns a presigned S3 PUT URL (15-minute expiry)

**Step 2 — S3 PUT** (client → S3 directly, backend not involved)

**Step 3 — Confirm**

| Method | Path | Auth | Response |
|--------|------|------|----------|
| POST | `/api/events/{eventId}/photos/{photoId}/confirm` | JWT | 202 Accepted |

- Verifies photo ownership
- Calls `recognitionJobService.enqueueRecognitionJob(photoId)` (async, returns immediately)

**List photos**

| Method | Path | Auth | Response |
|--------|------|------|----------|
| GET | `/api/events/{eventId}/photos` | JWT | `PhotoListResponse` |
| GET | `/api/events/public/{token}/photos` | None | `PhotoListResponse` |

`PhotoListResponse`: `{ photos: Photo[], totalCount: int }`

`Photo` fields: `id, eventId, presignedThumbUrl (null if thumbReady=false), presignedOriginalUrl, recognitionStatus, uploadedAt`  
Presigned download URLs: 60-minute expiry.

---

### Selfie Search  `POST /api/selfie/search`  (no JWT required)

| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/selfie/search` | `{ eventToken, selfieBase64 }` | `SelfieSearchResponse` |

Always returns HTTP 200. Outcome conveyed in `message` field:
- `"No face detected"` — Python `/embed` found no face in the selfie
- `"No match found"` — best cosine similarity < 0.5 against all event FaceTags
- `"Match found"` — match score ≥ 0.5; `photos` array contains matched photos

`SelfieSearchResponse`: `{ message, matchedTagId?, matchedTagLabel?, matchScore, photos[] }`

All selfie search attempts are logged in the `selfie_searches` table regardless of outcome.

---

### Health  (no auth)

| Method | Path | Response |
|--------|------|----------|
| GET | `/actuator/health` | `{ status: "UP" }` |

Exposed via Spring Boot Actuator. Used by Railway health checks.

---

## Key Flows

### Photo Upload + Recognition

```
Client                    Spring Boot              S3             Python Service
  │                           │                    │                    │
  │ POST /photos/initiate      │                    │                    │
  │──────────────────────────>│                    │                    │
  │                           │ create Photo(PENDING)                   │
  │                           │ generate presigned PUT URL              │
  │<──────────────────────────│                    │                    │
  │                           │                    │                    │
  │ PUT {presignedUrl} + file  │                    │                    │
  │─────────────────────────────────────────────>  │                    │
  │<─────────────────────────────────────────────  │                    │
  │                           │                    │                    │
  │ POST /photos/{id}/confirm  │                    │                    │
  │──────────────────────────>│                    │                    │
  │  202 Accepted             │ enqueue async job  │                    │
  │<──────────────────────────│                    │                    │
                              │                    │                    │
  [async on recognitionExecutor thread pool]       │                    │
                              │ markProcessing()   │                    │
                              │ (REQUIRES_NEW tx)  │                    │
                              │                    │                    │
                              │ POST /recognize ───────────────────────>│
                              │                    │ download from S3   │
                              │                    │<─────────────────  │
                              │                    │  (InsightFace)     │
                              │<───────────────────────────────────────│
                              │ persist FaceTag + PhotoFaceTag rows     │
                              │ set status = DONE  │                    │
                              │ generate thumbnail  │                   │
                              │ set thumbReady=true │                   │
```

The `markProcessing()` call commits in its own `REQUIRES_NEW` transaction so that
clients polling `GET /photos` can observe PROCESSING status before the slow Python
call begins. Do not collapse the two transactions.

### Selfie Search

```
Guest Client              Spring Boot              Python Service
  │                           │                         │
  │ POST /api/selfie/search    │                         │
  │──────────────────────────>│                         │
  │                           │ resolve event by token  │
  │                           │                         │
  │                           │ POST /embed (selfieBase64)
  │                           │────────────────────────>│
  │                           │                         │ InsightFace detect
  │                           │<────────────────────────│
  │                           │  { faceDetected, embedding }
  │                           │                         │
  │                           │ if faceDetected:        │
  │                           │   cosine similarity vs all FaceTags in event
  │                           │   threshold = 0.5       │
  │                           │   collect matching PhotoFaceTag rows
  │                           │   generate presigned GET URLs (60 min)
  │                           │                         │
  │ 200 { message, photos }   │                         │
  │<──────────────────────────│                         │
```

### Authentication — Google OAuth

```
Client                    Spring Boot              Google
  │                           │                      │
  │ POST /api/auth/google      │                      │
  │  { idToken }              │                      │
  │──────────────────────────>│                      │
  │                           │ GET /tokeninfo?id_token=…
  │                           │─────────────────────>│
  │                           │<─────────────────────│
  │                           │  { email, name, sub } │
  │                           │                      │
  │                           │ if email exists → login
  │                           │ else → auto-register (Agency + FREE subscription)
  │                           │                      │
  │  AuthResponse (JWT)       │                      │
  │<──────────────────────────│                      │
```

---

## Data Model

```
agencies ──────────────────────────────────────────────────────────────────┐
  id (UUID PK)                                                             │
  name, email, plan (FREE|PRO|ENTERPRISE)                                  │
  storageUsedMb, storageLimitMb                                            │
  └─── subscriptions (1:1)                                                 │
         eventsUsed, eventsLimit, storageLimitMb, renewsAt                 │
  └─── photographers (1:many) ────────────────────────────────────────────┘
         id (UUID PK)                                                       │
         name, email, passwordHash, googleSub?                             │
         └─── events (1:many) ────────────────────────────────────────────┘
                id (UUID PK)
                name, eventDate, location, status (ACTIVE|ARCHIVED)
                shareableToken (UUID — used in public URLs)
                photoCount
                └─── photos (1:many)
                       id (UUID PK)
                       s3KeyOriginal, s3KeyThumb
                       fileSizeKb
                       recognitionStatus (PENDING|PROCESSING|DONE|FAILED)
                       thumbReady (bool)
                       uploadedAt
                └─── face_tags (1:many)
                       id (UUID PK)
                       tagLabel (PERSON_1, PERSON_2, …)
                       centroidEmbedding (float[512])
                       faceCount
                └─── photo_face_tags (junction: photos ↔ face_tags)
                       photoId, faceTagId (composite PK)
                       similarityScore
                └─── selfie_searches (1:many)
                       id (UUID PK)
                       matchedTag? (FK → face_tags)
                       matchScore
                       createdAt
```

---

## Security

- `JwtFilter` runs before `UsernamePasswordAuthenticationFilter` on every request
- Extracts `PhotowahPrincipal(email, agencyId, photographerId)` from JWT claims
- `SecurityUtils.getCurrent*()` pulls these from `SecurityContextHolder` in service calls
- Public routes (no JWT): `POST /api/auth/**`, `GET /api/events/public/**`, `/api/selfie/**`, `/actuator/health`, Swagger UI
- Google-only accounts: password set to BCrypt of a random UUID (blocks password login)

---

## Async / Threading

`AsyncConfig` declares a `ThreadPoolTaskExecutor` named `recognitionExecutor`:
- Used exclusively for `@Async("recognitionExecutor")` on `RecognitionJobService.enqueueRecognitionJob`
- Jobs are fire-and-forget from the HTTP request thread; the client gets 202 immediately

---

## S3 Key Structure

```
events/{eventId}/originals/{filename}   — full-resolution upload
events/{eventId}/thumbs/{filename}      — thumbnail (generated post-recognition)
selfies/{uploadId}/{filename}           — guest selfie uploads
```

`S3Service` generates all presigned URLs. PUT expiry: 15 min. GET expiry: 60 min.

---

## External Dependencies

| Dependency | Purpose |
|------------|---------|
| `oauth2.googleapis.com/tokeninfo` | Verify Google ID tokens on `/api/auth/google` |
| Python recognition service (`PYTHON_SERVICE_URL`) | `POST /recognize` (photo recognition), `POST /embed` (selfie embedding) |
| AWS S3 (`ap-south-1`, bucket `photowah-dev-test`) | Photo and selfie storage |
| PostgreSQL | Primary database (Liquibase migrations) |
| Redis | Job queue state, session cache |

---

## Environment Variables

| Variable | Default | Notes |
|----------|---------|-------|
| `DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD` | localhost/5432/photowah | |
| `REDIS_HOST / REDIS_PORT` | localhost/6379 | |
| `JWT_SECRET` | — | Base64-encoded 32-byte secret |
| `JWT_EXPIRY_MS` | 86400000 | 24 h |
| `AWS_ACCESS_KEY / AWS_SECRET_KEY / AWS_REGION / AWS_BUCKET` | — | |
| `PYTHON_SERVICE_URL` | http://localhost:8000 | Internal URL of recognition service |
| `MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD` | smtp.gmail.com/587 | |
| `CORS_ALLOWED_ORIGINS` | http://localhost:5173 | Pattern-matched; use `http://localhost:*` in dev |

Swagger UI: `http://localhost:8080/swagger-ui.html`
