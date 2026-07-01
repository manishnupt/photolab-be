# controller

REST controllers annotated with `@RestController`. Responsible only for request mapping, input validation (`@Valid`), and delegating to the service layer.

Controllers return DTOs, never entities. All routes are documented via SpringDoc `@Operation` / `@Tag` annotations.
