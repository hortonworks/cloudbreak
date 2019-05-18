package com.sequenceiq.cloudbreak.auth.security.token;

import java.util.Arrays;
import java.util.HashMap;
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

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.authentication.UmsAuthenticationService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class CommonCachedRemoteTokenService implements ResourceServerTokenServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonCachedRemoteTokenService.class);

    private final AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();

    private final UmsAuthenticationService umsAuthenticationService;

    public CommonCachedRemoteTokenService(GrpcUmsClient umsClient) {
        umsAuthenticationService = new UmsAuthenticationService(umsClient);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not supported: read access token");
    }

    @Override
    @Cacheable(cacheNames = "tokenCache", key = "#accessToken")
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        try {
            return getUmsAuthentication(accessToken);
        } catch (RuntimeException e) {
            throw new InvalidTokenException("Invalid CRN provided", e);
        }
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
}
