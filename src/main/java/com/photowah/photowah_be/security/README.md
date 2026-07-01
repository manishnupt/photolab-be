# security

JWT infrastructure wired into the Spring Security filter chain.

| Class | Purpose |
|---|---|
| `JwtUtil` | Generates and validates JWT tokens using the `JWT_SECRET` env var |
| `JwtFilter` | `OncePerRequestFilter` — extracts the Bearer token, validates it, and sets the `SecurityContext` |
| `UserDetailsServiceImpl` | Loads a `UserDetails` from the database by username/email for Spring Security |
