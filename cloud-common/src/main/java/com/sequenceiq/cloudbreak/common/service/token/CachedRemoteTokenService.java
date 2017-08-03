package com.sequenceiq.cloudbreak.common.service.token;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import com.sequenceiq.cloudbreak.client.IdentityClient;

public class CachedRemoteTokenService implements ResourceServerTokenServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedRemoteTokenService.class);

    private final AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();

    private final IdentityClient identityClient;

    private final String clientSecret;

    public CachedRemoteTokenService(String clientId, String clientSecret, String identityServerUrl, IdentityClient identityClient) {
        this.identityClient = identityClient;
        this.clientSecret = clientSecret;
        LOGGER.info("Init RemoteTokenServices with clientId: {}, identityServerUrl: {}", clientId, identityServerUrl);
    }

    @Override
    @Cacheable(cacheNames = "tokenCache", key = "#accessToken")
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        Map<String, Object> map = identityClient.loadAuthentication(accessToken, clientSecret);
        OAuth2Authentication oAuth2Authentication = tokenConverter.extractAuthentication(map);

        if (oAuth2Authentication != null) {
            LOGGER.info("OAuth2 token verified for: {}", oAuth2Authentication.getPrincipal());
        }
        return oAuth2Authentication;
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        Map<String, Object> map = identityClient.readAccessToken(accessToken);
        return tokenConverter.extractAccessToken(accessToken, map);
    }
}
