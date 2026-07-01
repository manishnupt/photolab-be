package com.photowah.photowah_be.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentAgencyId() {
        return getPrincipal().agencyId();
    }

    public static UUID getCurrentPhotographerId() {
        return getPrincipal().photographerId();
    }

    public static String getCurrentEmail() {
        return getPrincipal().email();
    }

    private static PhotowahPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PhotowahPrincipal principal) {
            return principal;
        }
        throw new IllegalStateException("No PhotowahPrincipal in SecurityContext");
    }
}
