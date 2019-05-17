package com.sequenceiq.caas.service;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static javax.servlet.http.HttpServletResponse.SC_FOUND;
import static org.springframework.security.jwt.JwtHelper.decodeAndVerify;
import static org.springframework.security.jwt.JwtHelper.encode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.stereotype.Service;

import com.sequenceiq.caas.model.AltusToken;
import com.sequenceiq.caas.model.IntrospectResponse;
import com.sequenceiq.caas.util.CrnHelper;
import com.sequenceiq.caas.util.JsonUtil;

@Service
public class MockCaasService {

    public static final MacSigner SIGNATURE_VERIFIER = new MacSigner("titok");

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCaasService.class);

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
                String tokenClaims = decodeAndVerify(cookie.getValue(), SIGNATURE_VERIFIER).getClaims();
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

    public void auth(@Nonnull HttpServletRequest httpServletRequest, @Nonnull HttpServletResponse httpServletResponse, @Nonnull Optional<String> tenant,
            @Nonnull Optional<String> userName, String redirectUri, Boolean active) {
        if (tenant.isEmpty() || userName.isEmpty()) {
            LOGGER.info("redirect to sign in page");
            httpServletResponse.setHeader(LOCATION_HEADER_KEY, "../caas/sign-in.html?redirect_uri=" + redirectUri);
        } else {
            Cookie cdpSessionToken = new Cookie(CDP_SESSION_TOKEN, getAltusToken(tenant.get(), userName.get()));
            cdpSessionToken.setDomain("");
            cdpSessionToken.setPath("/");
            httpServletResponse.addCookie(cdpSessionToken);

            httpServletResponse.setHeader(LOCATION_HEADER_KEY, redirectUri);
        }
        httpServletResponse.setStatus(SC_FOUND);
    }

    private String generateDeterministicUserId(String tenantUser) {
        return UUID.nameUUIDFromBytes(tenantUser.getBytes()).toString();
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
        String token = encode(jsonUtil.toJsonString(altusToken), SIGNATURE_VERIFIER).getEncoded();
        LOGGER.info(format("Token generated for Altus: %s", token));
        return token;
    }

}
