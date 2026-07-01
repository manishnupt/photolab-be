# mapper

MapStruct mapper interfaces annotated with `@Mapper(componentModel = "spring")`. They convert between entities and DTOs at compile time with zero reflection overhead.

One mapper per aggregate root is a good default. Inject mappers into services, not controllers.
