# exception

Centralised error handling.

| Class | Purpose |
|---|---|
| `GlobalExceptionHandler` | `@RestControllerAdvice` — maps exceptions to structured `ProblemDetail` / error responses |
| Custom exception classes | e.g. `ResourceNotFoundException`, `UnauthorizedException` — extend `RuntimeException` and carry an HTTP status |

Throw custom exceptions from the service layer; `GlobalExceptionHandler` translates them to the right HTTP response.
