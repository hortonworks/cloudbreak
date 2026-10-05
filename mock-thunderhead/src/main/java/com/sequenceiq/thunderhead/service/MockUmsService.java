package com.sequenceiq.thunderhead.service;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static jakarta.servlet.http.HttpServletResponse.SC_FOUND;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.thunderhead.model.AltusToken;
import com.sequenceiq.thunderhead.model.IntrospectResponse;
import com.sequenceiq.thunderhead.util.CrnHelper;
import com.sequenceiq.thunderhead.util.JsonUtil;

import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class MockUmsService {

    public static final String MAC_SIGNER_SECRET_KEY = "titokamisokkaldesokkaldesokkalhosszabbhogyfipscompliantlegyen";

    public static final SecretKey SIGNATURE_VERIFIER = Keys.hmacShaKeyFor(MAC_SIGNER_SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    private static final Logger LOGGER = LoggerFactory.getLogger(MockUmsService.class);

    private static final String LOCATION_HEADER_KEY = "Location";

    private static final String JWT_COOKIE_KEY = "dps-jwt";

    private static final String CDP_SESSION_TOKEN = "cdp-session-token";

    private static final String ISS_KNOX = "KNOXSSO";

    private static final String ISS_ALTUS = "Altus IAM";

    private static final int PLUS_QUANTITY = 1;

    @Inject
    private JsonUtil jsonUtil;

    public IntrospectResponse getIntrospectResponse(@Nonnull HttpServletRequest request) {
        for (Cookie cookie : request.getCookies()) {
            if (JWT_COOKIE_KEY.equals(cookie.getName())) {
                Jws<byte[]> jws = Jwts.parser().verifyWith(SIGNATURE_VERIFIER).build().parseSignedContent(cookie.getValue());
                String tokenClaims = new String(jws.getPayload(), StandardCharsets.UTF_8);
                return jsonUtil.toObject(tokenClaims, IntrospectResponse.class);
            }
        }
        throw new NotFoundException("Can not retrieve user from token");
    }

    public void out(@Nonnull HttpServletRequest httpServletRequest, @Nonnull HttpServletResponse httpServletResponse) {
        String host = httpServletRequest.getHeader("Host");
        Cookie cookie = new Cookie(JWT_COOKIE_KEY, "");
        cookie.setDomain(host.split(":")[0]);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        httpServletResponse.addCookie(cookie);
        httpServletResponse.setHeader(LOCATION_HEADER_KEY, "/");
        httpServletResponse.setStatus(SC_FOUND);
    }

    public void auth(@Nonnull HttpServletRequest httpServletRequest,
            @Nonnull HttpServletResponse httpServletResponse,
            @Nonnull Optional<String> tenant,
            @Nonnull Optional<String> userName, String redirectUri, Boolean active) {
        if (tenant.isEmpty() || userName.isEmpty()) {
            LOGGER.info("redirect to sign in page");
            httpServletResponse.setHeader(LOCATION_HEADER_KEY, "../auth/sign-in.html?redirect_uri=" + redirectUri);
        } else {
            Cookie cdpSessionToken = new Cookie(CDP_SESSION_TOKEN, getAltusToken(tenant.get(), userName.get()));
            cdpSessionToken.setDomain("");
            cdpSessionToken.setPath("/");
            httpServletResponse.addCookie(cdpSessionToken);

            httpServletResponse.setHeader(LOCATION_HEADER_KEY, redirectUri);
        }
        httpServletResponse.setStatus(SC_FOUND);
    }

    private String getAltusToken(String tenant, String user) {
        AltusToken altusToken = new AltusToken();
        altusToken.setIss(ISS_ALTUS);
        altusToken.setAud(ISS_ALTUS);
        altusToken.setJti(UUID.randomUUID().toString());
        altusToken.setIat(Instant.now().toEpochMilli());
        altusToken.setExp(Instant.now().plus(PLUS_QUANTITY, DAYS).toEpochMilli());
        altusToken.setIat(Instant.now().toEpochMilli());
        altusToken.setSub(CrnHelper.generateCrn(tenant, user));
        String token = Jwts.builder()
                .content(jsonUtil.toJsonString(altusToken).getBytes(StandardCharsets.UTF_8), "json")
                .signWith(SIGNATURE_VERIFIER, Jwts.SIG.HS256)
                .compact();
        LOGGER.info(format("Token generated for Altus: %s", token));
        return token;
    }

}
