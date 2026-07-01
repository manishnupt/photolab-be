# config

Spring configuration beans.

| Class | Purpose |
|---|---|
| `SecurityConfig` | Spring Security filter chain — CORS, CSRF, stateless session, route permissions |
| `RedisConfig` | `RedisTemplate` and connection factory wired from `REDIS_HOST`/`REDIS_PORT` |
| `S3Config` | `S3Client` bean configured with AWS credentials and region |
| `SwaggerConfig` | SpringDoc/OpenAPI metadata — title, version, JWT bearer auth scheme |
