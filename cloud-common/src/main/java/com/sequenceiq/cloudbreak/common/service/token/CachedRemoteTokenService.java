package com.sequenceiq.cloudbreak.common.service.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

public class CachedRemoteTokenService implements ResourceServerTokenServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedRemoteTokenService.class);

    private final RemoteTokenServices remoteTokenServices;

    public CachedRemoteTokenService(String clientId, String clientSecret, String identityServerUrl) {
        remoteTokenServices = new RemoteTokenServices();
        remoteTokenServices.setClientId(clientId);
        remoteTokenServices.setClientSecret(clientSecret);
        String url = identityServerUrl + "/check_token";
        remoteTokenServices.setCheckTokenEndpointUrl(url);
        LOGGER.info("Init RemoteTokenServices with clientId: {}, identityServerUrl: {}", clientId, url);
    }

    @Override
    @Cacheable(cacheNames = "tokenCache", key = "#accessToken")
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        OAuth2Authentication oauth2Authentication = remoteTokenServices.loadAuthentication(accessToken);
        if (oauth2Authentication != null) {
            LOGGER.info("OAuth2 token verified for: {}", oauth2Authentication.getPrincipal());
        }
        return oauth2Authentication;
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        return remoteTokenServices.readAccessToken(accessToken);
    }
}
