# PhotoWah — CLAUDE.md

## Project Overview
PhotoWah is an event photography SaaS platform. Photographers upload photos
to events, the system detects and clusters faces automatically, and guests
can find their own photos by uploading a selfie.

## Architecture
- Monorepo with two services:
  1. Spring Boot backend (Java, Maven) — all API, auth, DB, S3, job queue
  2. recognition-service/ (Python, FastAPI) — face detection and embedding only
- Web frontend: React (to be built)
- Mobile: React Native Android (to be built)

## Tech Stack
### Spring Boot Service
- Java 21, Spring Boot 4.x, Maven
- Spring Security with JWT (HS256) + Google OAuth2
- Spring Data JPA + Hibernate (PostgreSQL dialect)
- Liquibase for DB migrations (XML format)
- AWS SDK v2 for S3 (presigned URLs — no file bytes touch our server)
- Spring @Async with ThreadPoolTaskExecutor for recognition job queue
- RestClient for internal HTTP calls to Python service
- Springdoc OpenAPI (Swagger at /swagger-ui.html)
- Lombok + MapStruct

### Python Service (recognition-service/)
- Python 3.11, FastAPI, Uvicorn
- InsightFace (buffalo_l model, ArcFace 512-d embeddings)
- ONNX Runtime (CPU inference, ctx_id=-1)
- boto3 for S3 download
- Pillow for image handling
- scipy for cosine similarity

### Database
- PostgreSQL (local for dev, Railway for prod)
- Redis (Docker for dev, Railway for prod) — job queue state, session cache

### Infrastructure
- Backend deployed on Railway
- AWS S3 for photo storage
- Docker Compose for local Redis

## Package Structure (Spring Boot)
```
src/main/java/com/photowah/
  config/        — SecurityConfig, AsyncConfig, S3Config, SwaggerConfig
  controller/    — REST controllers (AuthController, AgencyController,
                   EventController, PhotoController)
  dto/           — Request/response DTOs grouped by domain
    auth/
    agency/
    event/
    photo/
    recognition/
  entity/        — JPA entities (Agency, Photographer, Event, Photo,
                   FaceTag, PhotoFaceTag, SelfieSearch, Subscription)
  enums/         — RecognitionStatus, Plan, EventStatus
  exception/     — GlobalExceptionHandler + custom exceptions
  mapper/        — MapStruct mappers
  repository/    — Spring Data JPA repositories
  security/      — JwtUtil, JwtFilter, UserDetailsServiceImpl, SecurityUtils
  service/       — Business logic (AgencyService, EventService, PhotoService,
                   RecognitionJobService, RecognitionTransactionHelper, S3Service)
```

## Database Schema
Key tables and relationships:
- agencies → photographers (one agency has many photographers)
- photographers → events (one photographer creates many events)
- events → photos (one event has many photos)
- events → face_tags (one event has many face tags — one tag = one person)
- photos ↔ face_tags via photo_face_tags (junction table, has similarity_score)
- events → selfie_searches (each selfie upload is logged here)
- agencies → subscriptions (billing limits: eventsLimit, storageLimitMb)

Liquibase migrations live in:
```
src/main/resources/db/changelog/
  db.changelog-master.xml  — includes all changesets in order
  changes/V1__create_agencies.xml through V9__add_thumb_ready_to_photos.xml
```

## Face Recognition Flow
1. Photographer uploads photo → Spring Boot saves Photo with status=PENDING
2. Client calls /confirm → async job enqueued on "recognitionExecutor" thread pool
3. RecognitionJobService:
   a. Sets status=PROCESSING (committed immediately via REQUIRES_NEW transaction
      inside RecognitionTransactionHelper.markProcessing)
   b. POSTs to Python /recognize with { photoId, s3KeyOriginal, eventId }
4. Python service:
   a. Downloads image from S3
   b. Detects faces, generates 512-d ArcFace embeddings
   c. For each embedding: cosine similarity vs existing tag centroids (threshold=0.6)
   d. Returns faces array with tagId/tagLabel/isNewTag/similarityScore/centroidEmbedding
5. Spring Boot saves FaceTag (new or updated centroid) + PhotoFaceTag rows
6. Sets status=DONE (or FAILED on error)

Tag labels are auto-generated: PERSON_1, PERSON_2, etc. per event.
Centroid embedding = running average of all face embeddings for that tag.

The two-transaction split in RecognitionJobService is intentional: markProcessing
commits in its own REQUIRES_NEW transaction so pollers can observe PROCESSING
before the Python call starts. Never collapse this back into one transaction.

## S3 Structure
```
events/{eventId}/originals/{filename}   — full resolution photo
events/{eventId}/thumbs/{filename}      — thumbnail (generated after upload)
events/{eventId}/faces/{filename}       — cropped face images (optional)
selfies/{uploadId}/{filename}           — guest selfie uploads
```

All client access via presigned URLs (PUT for upload, GET for download).
Presigned upload URL expiry: 15 minutes.
Presigned download URL expiry: 60 minutes.

## Auth
- Photographers: email/password (BCrypt) or Google OAuth2
- JWT claims: email, role (ROLE_PHOTOGRAPHER), agencyId
- Public routes: POST /api/auth/**, GET /api/events/public/**
- All other routes require Bearer token

## Key Business Rules
- Agency has one Subscription with eventsLimit and storageLimitMb
- Creating an event checks eventsUsed < eventsLimit (throws 403 if exceeded)
- Photo upload checks storageUsedMb < storageLimitMb (throws 403 if exceeded)
- Archived events cannot receive new photo uploads
- thumbReady flag on Photo entity — presigned thumb URL only returned if true
- Public shareable link (/api/events/public/{token}) requires no auth

## Known Issues / TODOs
- Thumbnail generation not yet implemented (thumbReady always false)
- Face crop storage in S3 not yet implemented
- Selfie search endpoint not yet built
- React web frontend not yet started
- React Native Android app not yet started
- Subscription plan enforcement (FREE/PRO/ENTERPRISE limits) partially done

## Environment Variables
See .env.example for full list. Key ones:
```
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
REDIS_HOST, REDIS_PORT
AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_BUCKET, AWS_REGION
JWT_SECRET          — must be Base64-encoded; generate: openssl rand -base64 32
JWT_EXPIRY_MS       — e.g. 86400000 for 24h
PYTHON_SERVICE_URL  — default: http://localhost:8000
```

## Running Locally
```bash
# Start Redis
docker-compose up -d

# Start Python recognition service
cd recognition-service/
pip install -r requirements.txt
uvicorn main:app --reload --port 8000

# Start Spring Boot
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## API Docs
Swagger UI: http://localhost:8080/swagger-ui.html

## Coding Conventions
- All new entities use UUID primary keys
- All new Liquibase changesets go in changes/ with next version number
- DTOs never expose entity objects directly — always map through service layer
- Async jobs always commit PROCESSING status before starting work
- Never store file bytes in Spring Boot — always use S3 presigned URLs
- Python service is stateless — all state lives in PostgreSQL via Spring Boot
- RecognitionTransactionHelper is package-private by design — don't make it public
