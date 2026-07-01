# enums

Shared enum types used across entities and DTOs.

| Enum | Values (placeholder) |
|---|---|
| `RecognitionStatus` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `Plan` | `FREE`, `PRO`, `ENTERPRISE` |
| `EventStatus` | `DRAFT`, `ACTIVE`, `CLOSED`, `ARCHIVED` |

Store enums as strings in the database (`@Enumerated(EnumType.STRING)`) to keep migrations readable.
