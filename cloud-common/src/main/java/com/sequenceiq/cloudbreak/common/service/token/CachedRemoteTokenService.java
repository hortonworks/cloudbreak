package com.sequenceiq.cloudbreak.common.service.token;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.client.CaasClient;
import com.sequenceiq.cloudbreak.client.CaasUser;
import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.client.IntrospectResponse;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class CachedRemoteTokenService implements ResourceServerTokenServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedRemoteTokenService.class);

    private final AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();

    private final IdentityClient identityClient;

    private final GrpcUmsClient umsClient;

    private final CaasClient caasClient;

    private final ObjectMapper objectMapper;

    private final String clientSecret;

    private final JwtAccessTokenConverter jwtAccessTokenConverter;

    public CachedRemoteTokenService(String clientId, String clientSecret, String identityServerUrl,
            GrpcUmsClient umsClient, CaasClient caasClient, IdentityClient identityClient) {
        this(clientId, clientSecret, identityServerUrl, umsClient, caasClient, null, identityClient);
    }

    public CachedRemoteTokenService(String clientId, String clientSecret, String identityServerUrl,
            GrpcUmsClient umsClient, CaasClient caasClient, String jwtSignKey,
            IdentityClient identityClient) {
        this.identityClient = identityClient;
        this.umsClient = umsClient;
        this.caasClient = caasClient;
        this.clientSecret = clientSecret;
        objectMapper = new ObjectMapper();
        jwtAccessTokenConverter = new JwtAccessTokenConverter();
        LOGGER.debug("Init RemoteTokenServices with clientId: {}, identityServerUrl: {}", clientId, identityServerUrl);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        Map<String, Object> map = identityClient.readAccessToken(accessToken);
        return tokenConverter.extractAccessToken(accessToken, map);
    }

    @Override
    @Cacheable(cacheNames = "tokenCache", key = "#accessToken")
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        if (umsClient.isUmsUsable(accessToken)) {
            try {
                return getUmsAuthentication(accessToken);
            } catch (RuntimeException e) {
                throw new InvalidTokenException("Invalid CRN provided", e);
            }
        }
        return extractJwtAuthentication(accessToken);
    }

    private OAuth2Authentication extractJwtAuthentication(String accessToken) {
        Jwt jwtToken;
        try {
            jwtToken = JwtHelper.decode(accessToken);
        } catch (RuntimeException e) {
            throw new InvalidTokenException("Invalid JWT token", e);
        }
        try {
            Map<String, String> claims = objectMapper.readValue(jwtToken.getClaims(), new MapTypeReference());
            return "uaa".equals(claims.get("zid")) ? getOAuth2Authentication(accessToken) : getSSOAuthentication(accessToken, claims);
        } catch (IOException e) {
            LOGGER.error("Token does not claim anything", e);
            throw new InvalidTokenException("Invalid JWT token, does not claim anything", e);
        }
    }

    private OAuth2Authentication getUmsAuthentication(String crn) {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        UserManagementProto.User user = umsClient.getUserDetails(crn, crn, Optional.ofNullable(requestId));
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("tenant", Crn.fromString(crn).getAccountId());
        tokenMap.put("crn", user.getCrn());
        tokenMap.put("user_id", user.getUserId());
        tokenMap.put("user_name", user.getEmail());
        tokenMap.put("scope", Arrays.asList("cloudbreak.networks.read", "periscope.cluster", "cloudbreak.usages.user", "cloudbreak.recipes", "openid",
                "cloudbreak.templates.read", "cloudbreak.usages.account", "cloudbreak.events", "cloudbreak.stacks.read",
                "cloudbreak.blueprints", "cloudbreak.networks", "cloudbreak.templates", "cloudbreak.credentials.read",
                "cloudbreak.securitygroups.read", "cloudbreak.securitygroups", "cloudbreak.stacks", "cloudbreak.credentials",
                "cloudbreak.recipes.read", "cloudbreak.blueprints.read"));
        return tokenConverter.extractAuthentication(tokenMap);
    }

    private OAuth2Authentication getOAuth2Authentication(String accessToken) {
        Map<String, Object> map = identityClient.loadAuthentication(accessToken, clientSecret);
        OAuth2Authentication oAuth2Authentication = tokenConverter.extractAuthentication(map);
        if (oAuth2Authentication != null) {
            LOGGER.debug("OAuth2 token verified for: {}", oAuth2Authentication.getPrincipal());
        }
        return oAuth2Authentication;
    }

    private OAuth2Authentication getSSOAuthentication(String accessToken, Map<String, String> claims) {
        try {
            String tenant;
            if (claims.get("tenant_name") != null) {
                tenant = claims.get("tenant_name");
                LOGGER.debug("Tenant for the token is: {}", tenant);
            } else {
                throw new InvalidTokenException("No 'tenant_name' claim in token");
            }
            IntrospectResponse introspectResponse = caasClient.introSpect(accessToken);
            if (!introspectResponse.isActive()) {
                throw new InvalidTokenException("The specified JWT token is not active");
            }
            CaasUser userInfo = caasClient.getUserInfo(accessToken);
            Map<String, Object> tokenMap = new HashMap<>();
            tokenMap.put("tenant", tenant);
            tokenMap.put("user_id", userInfo.getId());
            tokenMap.put("user_name", userInfo.getPreferredUsername());
            tokenMap.put("scope", Arrays.asList("cloudbreak.networks.read", "periscope.cluster", "cloudbreak.usages.user", "cloudbreak.recipes", "openid",
                    "cloudbreak.templates.read", "cloudbreak.usages.account", "cloudbreak.events", "cloudbreak.stacks.read",
                    "cloudbreak.blueprints", "cloudbreak.networks", "cloudbreak.templates", "cloudbreak.credentials.read",
                    "cloudbreak.securitygroups.read", "cloudbreak.securitygroups", "cloudbreak.stacks", "cloudbreak.credentials",
                    "cloudbreak.recipes.read", "cloudbreak.blueprints.read"));

            OAuth2AccessToken oAuth2AccessToken = jwtAccessTokenConverter.extractAccessToken(accessToken, tokenMap);
            if (oAuth2AccessToken.isExpired()) {
                throw new InvalidTokenException("The token has expired");
            }
            OAuth2Authentication oAuth2Authentication = jwtAccessTokenConverter.extractAuthentication(tokenMap);
            if (oAuth2Authentication != null) {
                LOGGER.debug("JWT token verified for: {}", oAuth2Authentication.getPrincipal());
            }
            return oAuth2Authentication;
        } catch (ProcessingException e) {
            LOGGER.error("Failed to parse the JWT token", e);
            throw new InvalidTokenException("The specified JWT token is invalid", e);
        }
    }

    private boolean isAssymetricKey(String key) {
        return key.startsWith("-----BEGIN");
    }

    public static class MapTypeReference extends TypeReference<Map<String, Object>> {
    }
}
