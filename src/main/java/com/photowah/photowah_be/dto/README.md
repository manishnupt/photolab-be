# dto

Request and response Data Transfer Objects. Suffix convention:
- `*Request` — inbound payloads (carry Bean Validation annotations)
- `*Response` — outbound payloads returned to clients

DTOs are plain records or Lombok `@Data` classes. No JPA annotations, no business logic.
