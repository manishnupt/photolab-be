package com.photowah.photowah_be.service;

import com.photowah.photowah_be.dto.GoogleTokenInfo;
import com.photowah.photowah_be.dto.auth.AuthResponse;
import com.photowah.photowah_be.dto.auth.LoginRequest;
import com.photowah.photowah_be.dto.auth.RegisterRequest;
import com.photowah.photowah_be.entity.Agency;
import com.photowah.photowah_be.entity.Photographer;
import com.photowah.photowah_be.entity.Subscription;
import com.photowah.photowah_be.enums.Plan;
import com.photowah.photowah_be.exception.EmailAlreadyExistsException;
import com.photowah.photowah_be.exception.InvalidCredentialsException;
import com.photowah.photowah_be.repository.AgencyRepository;
import com.photowah.photowah_be.repository.PhotographerRepository;
import com.photowah.photowah_be.repository.SubscriptionRepository;
import com.photowah.photowah_be.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private static final long  FREE_STORAGE_LIMIT_MB = 5_120L; // 5 GB
    private static final int   FREE_EVENTS_LIMIT      = 5;

    private static final RestClient GOOGLE_CLIENT =
            RestClient.create("https://oauth2.googleapis.com");

    private final AgencyRepository agencyRepository;
    private final PhotographerRepository photographerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    public AuthResponse register(RegisterRequest req) {
        if (photographerRepository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }

        Agency agency = agencyRepository.save(Agency.builder()
                .name(req.getAgencyName())
                .email(req.getEmail())
                .plan(Plan.FREE)
                .storageUsedMb(0L)
                .storageLimitMb(FREE_STORAGE_LIMIT_MB)
                .build());

        Photographer photographer = photographerRepository.save(Photographer.builder()
                .agency(agency)
                .name(req.getPhotographerName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build());

        createDefaultSubscription(agency);

        return buildResponse(photographer, agency);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Photographer photographer = photographerRepository.findByEmail(req.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.getPassword(), photographer.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildResponse(photographer, photographer.getAgency());
    }

    // -------------------------------------------------------------------------
    // Google OAuth
    // -------------------------------------------------------------------------

    public AuthResponse googleAuth(String idToken) {
        // HTTP call happens before the DB transaction does any work
        GoogleTokenInfo tokenInfo = verifyGoogleToken(idToken);

        return photographerRepository.findByEmail(tokenInfo.getEmail())
                .map(p -> buildResponse(p, p.getAgency()))
                .orElseGet(() -> registerGoogleUser(tokenInfo));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private GoogleTokenInfo verifyGoogleToken(String idToken) {
        try {
            GoogleTokenInfo info = GOOGLE_CLIENT.get()
                    .uri("/tokeninfo?id_token=" + idToken)
                    .retrieve()
                    .body(GoogleTokenInfo.class);

            if (info == null || info.getEmail() == null) {
                throw new InvalidCredentialsException("Invalid Google ID token");
            }
            return info;
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid Google ID token");
        }
    }

    private AuthResponse registerGoogleUser(GoogleTokenInfo tokenInfo) {
        Agency agency = agencyRepository.save(Agency.builder()
                .name(tokenInfo.getName() + "'s Agency")
                .email(tokenInfo.getEmail())
                .plan(Plan.FREE)
                .storageUsedMb(0L)
                .storageLimitMb(FREE_STORAGE_LIMIT_MB)
                .build());

        // Google-only accounts have no password; an unusable hash blocks password login
        String unusableHash = passwordEncoder.encode(UUID.randomUUID().toString());

        Photographer photographer = photographerRepository.save(Photographer.builder()
                .agency(agency)
                .name(tokenInfo.getName())
                .email(tokenInfo.getEmail())
                .googleSub(tokenInfo.getSub())
                .passwordHash(unusableHash)
                .build());

        createDefaultSubscription(agency);

        return buildResponse(photographer, agency);
    }

    private void createDefaultSubscription(Agency agency) {
        subscriptionRepository.save(Subscription.builder()
                .agency(agency)
                .plan(Plan.FREE)
                .eventsUsed(0)
                .eventsLimit(FREE_EVENTS_LIMIT)
                .storageLimitMb(FREE_STORAGE_LIMIT_MB)
                .renewsAt(LocalDateTime.now().plusMonths(1))
                .build());
    }

    private AuthResponse buildResponse(Photographer photographer, Agency agency) {
        String token = jwtUtil.generateToken(
                photographer.getEmail(),
                "ROLE_PHOTOGRAPHER",
                agency.getId(),
                photographer.getId()
        );
        return AuthResponse.builder()
                .token(token)
                .email(photographer.getEmail())
                .photographerName(photographer.getName())
                .agencyId(agency.getId())
                .plan(agency.getPlan().name())
                .build();
    }
}
