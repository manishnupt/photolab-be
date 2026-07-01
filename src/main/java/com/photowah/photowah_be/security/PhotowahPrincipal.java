package com.photowah.photowah_be.security;

import java.util.UUID;

/**
 * Custom principal stored in the SecurityContext after JWT validation.
 * Carries agencyId and photographerId claims so controllers can read them without re-parsing the token.
 */
public record PhotowahPrincipal(String email, UUID agencyId, UUID photographerId) {}
