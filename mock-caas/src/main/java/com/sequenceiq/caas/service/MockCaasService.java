package com.sequenceiq.caas.service;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static javax.servlet.http.HttpServletResponse.SC_FOUND;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.jwt.JwtHelper.decodeAndVerify;
import static org.springframework.security.jwt.JwtHelper.encode;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.stereotype.Service;

import com.sequenceiq.caas.model.AltusToken;
import com.sequenceiq.caas.model.CaasUser;
import com.sequenceiq.caas.model.CaasUserList;
import com.sequenceiq.caas.model.IntrospectRequest;
import com.sequenceiq.caas.model.IntrospectResponse;
import com.sequenceiq.caas.model.TokenResponse;
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
            Cookie jwtCookie = new Cookie(JWT_COOKIE_KEY, getCaasToken(tenant.get(), userName.get(), active).getAccessToken());
            jwtCookie.setDomain("");
            jwtCookie.setPath("/");
            httpServletResponse.addCookie(jwtCookie);

            Cookie cdpSessionToken = new Cookie(CDP_SESSION_TOKEN, getAltusToken(tenant.get(), userName.get()));
            cdpSessionToken.setDomain("");
            cdpSessionToken.setPath("/");
            httpServletResponse.addCookie(cdpSessionToken);

            httpServletResponse.setHeader(LOCATION_HEADER_KEY, redirectUri);
        }
        httpServletResponse.setStatus(SC_FOUND);
    }

    public TokenResponse getTokenResponse(String authorizationCode, String refreshToken) {
        if (authorizationCode != null) {
            return new TokenResponse(authorizationCode, authorizationCode);
        } else if (refreshToken != null) {
            return new TokenResponse(refreshToken, refreshToken);
        } else {
            throw new InvalidParameterException();
        }
    }

    public String authorize(@Nonnull HttpServletResponse httpServletResponse, Optional<String> tenant, Optional<String> userName, Optional<String> redirectUri,
            Boolean active) {
        if (tenant.isPresent() && userName.isPresent()) {
            TokenResponse token = getCaasToken(tenant.get(), userName.get(), active);
            if (redirectUri.isPresent()) {
                LOGGER.info("redirect to " + redirectUri + ".html");
                httpServletResponse.setHeader(LOCATION_HEADER_KEY, redirectUri.get() + "?authorization_code=" + token.getRefreshToken());
                httpServletResponse.setStatus(SC_FOUND);
                return null;
            } else {
                return token.getRefreshToken();
            }
        } else {
            LOGGER.info("redirect to authorize.html");
            httpServletResponse.setHeader(LOCATION_HEADER_KEY, "authorize.html");
            httpServletResponse.setStatus(SC_FOUND);
            return null;
        }
    }

    public CaasUserList getUsers(HttpServletRequest request) {
        String authenticationHeader = request.getHeader(AUTHORIZATION);
        String token;
        if (authenticationHeader.startsWith("Bearer ")) {
            token = authenticationHeader.substring(7);
        } else {
            throw new AccessDeniedException("No token in Authorization header");
        }
        IntrospectResponse introspectResponse = introSpect(token);
        List<CaasUser> caasUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CaasUser caasUser = new CaasUser();
            String userName = "mockuser" + i;
            caasUser.setName(userName);
            caasUser.setPreferredUsername(userName);
            caasUser.setTenantId(introspectResponse.getTenantName());
            caasUser.setId(generateDeterministicUserId(introspectResponse.getTenantName() + '#' + userName));
            caasUsers.add(caasUser);
        }
        return new CaasUserList(caasUsers);
    }

    public CaasUser getUserInfo(@Nonnull HttpServletRequest request) {
        IntrospectResponse introspectResponse = getIntrospectResponse(request);
        CaasUser caasUser = new CaasUser();
        caasUser.setName(introspectResponse.getSub());
        caasUser.setPreferredUsername(introspectResponse.getSub());
        caasUser.setTenantId(introspectResponse.getTenantName());
        caasUser.setId(generateDeterministicUserId(introspectResponse.getTenantName() + '#' + introspectResponse.getSub()));
        LOGGER.info(format("Generated caas user: %s", jsonUtil.toJsonString(caasUser)));
        return caasUser;
    }

    public IntrospectResponse introSpect(@Nonnull IntrospectRequest encodedToken) {
        return introSpect(encodedToken.getToken());
    }

    private String generateDeterministicUserId(String tenantUser) {
        return UUID.nameUUIDFromBytes(tenantUser.getBytes()).toString();
    }

    private IntrospectResponse introSpect(String encodedToken) throws AccessDeniedException {
        try {
            Jwt token = decodeAndVerify(encodedToken, SIGNATURE_VERIFIER);
            IntrospectResponse introspectResponse = jsonUtil.toObject(token.getClaims(), IntrospectResponse.class);
            LOGGER.info(format("IntrospectResponse: %s", jsonUtil.toJsonString(introspectResponse)));
            return introspectResponse;
        } catch (Exception e) {
            LOGGER.error("Exception in introspect call", e);
            throw new AccessDeniedException("Token is invalid", e);
        }
    }

    private TokenResponse getCaasToken(String tenant, String user, boolean active) {
        IntrospectResponse payload = new IntrospectResponse();
        payload.setSub(user);
        payload.setTenantName(tenant);
        payload.setIss(ISS_KNOX);
        payload.setActive(active);
        payload.setExp(Instant.now().plus(PLUS_QUANTITY, DAYS).toEpochMilli());
        String token = encode(jsonUtil.toJsonString(payload), SIGNATURE_VERIFIER).getEncoded();
        LOGGER.info(format("Token generated: %s", token));
        return new TokenResponse(token, token);
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
