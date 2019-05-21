package com.sequenceiq.cloudbreak.auth.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.authentication.UmsAuthenticationService;
import com.sequenceiq.cloudbreak.auth.uaa.IdentityClient;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class CachedRemoteTokenService implements ResourceServerTokenServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedRemoteTokenService.class);

    private final AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();

    private final IdentityClient identityClient;

    private final GrpcUmsClient umsClient;

    private final ObjectMapper objectMapper;

    private final String clientSecret;

    private final JwtAccessTokenConverter jwtAccessTokenConverter;

    private final UmsAuthenticationService umsAuthenticationService;

    public CachedRemoteTokenService(String clientId, String clientSecret, String identityServerUrl,
            GrpcUmsClient umsClient, IdentityClient identityClient) {
        this(clientId, clientSecret, identityServerUrl, umsClient, null, identityClient);
    }

    public CachedRemoteTokenService(String clientId, String clientSecret, String identityServerUrl,
            GrpcUmsClient umsClient, String jwtSignKey,
            IdentityClient identityClient) {
        this.identityClient = identityClient;
        this.umsClient = umsClient;
        this.clientSecret = clientSecret;
        objectMapper = new ObjectMapper();
        jwtAccessTokenConverter = new JwtAccessTokenConverter();
        umsAuthenticationService = new UmsAuthenticationService(umsClient);
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
        if (Crn.isCrn(accessToken)) {
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
            JwtHelper.decode(accessToken);
        } catch (RuntimeException e) {
            throw new InvalidTokenException("Invalid JWT token", e);
        }
        return getOAuth2Authentication(accessToken);
    }

    private OAuth2Authentication getUmsAuthentication(String crnText) {
        Crn crn = Crn.fromString(crnText);
        CloudbreakUser cloudbreakUser = umsAuthenticationService.getCloudbreakUser(crnText, null);
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("tenant", crn.getAccountId());
        tokenMap.put("crn", crnText);
        tokenMap.put("user_id", cloudbreakUser.getUserId());
        tokenMap.put("user_name", cloudbreakUser.getEmail());
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

    private boolean isAssymetricKey(String key) {
        return key.startsWith("-----BEGIN");
    }

    public static class MapTypeReference extends TypeReference<Map<String, Object>> {
    }
}
