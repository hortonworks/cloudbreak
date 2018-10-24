package com.sequenceiq.periscope.service;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.token.CachedRemoteTokenService;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.service.security.CachedUserDetailsService;

@Service
public class AuthenticatedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUserService.class);

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    public PeriscopeUser getPeriscopeUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication oauth = (OAuth2Authentication) authentication;
            if (oauth.getUserAuthentication() != null) {
                String username = (String) authentication.getPrincipal();
                String tenant = getTenant(oauth);
                return cachedUserDetailsService.getDetails(username, tenant, UserFilterField.USERNAME);
            }
        }
        return null;
    }

    public static String getTenant(OAuth2Authentication oauth) {
        Jwt decodedJwt = JwtHelper.decode(((OAuth2AuthenticationDetails) oauth.getDetails()).getTokenValue());
        String tenant = "DEFAULT";
        try {
            Map<String, Object> claims = JsonUtil.readValue(decodedJwt.getClaims(), new CachedRemoteTokenService.MapTypeReference());
            if (claims.get("aud") != null) {
                tenant = claims.get("aud").toString();
            }
        } catch (IOException e) {
            LOGGER.warn("can not get claims from token", e);
        }
        return tenant;
    }
}
