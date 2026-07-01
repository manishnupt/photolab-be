# entity

JPA entity classes mapped to database tables. Each class is annotated with `@Entity` and uses Lombok (`@Data`, `@Builder`, etc.) to reduce boilerplate.

Entities own the canonical field definitions — DTOs are derived from them via MapStruct mappers.
