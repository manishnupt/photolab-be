# service

Business logic layer. Service classes are annotated with `@Service` and `@Transactional` where needed. They depend on repositories, mappers, and infrastructure clients (Redis, S3, mail).

Controllers call services; services never call controllers. Keep HTTP concerns (status codes, headers) out of this layer.
