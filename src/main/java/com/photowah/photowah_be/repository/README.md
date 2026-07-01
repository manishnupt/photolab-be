# repository

Spring Data JPA repository interfaces. Extend `JpaRepository<Entity, ID>` for standard CRUD. Add custom JPQL or native queries here using `@Query` when the derived method name becomes unreadable.

No business logic lives here — keep repositories as thin data-access ports.
