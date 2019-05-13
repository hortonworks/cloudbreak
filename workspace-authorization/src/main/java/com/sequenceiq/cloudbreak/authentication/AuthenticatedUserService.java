package com.sequenceiq.cloudbreak.authentication;

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

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.cloudbreak.util.MapTypeReference;

@Service
public class AuthenticatedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUserService.class);

    @Inject
    private AuthenticationService authenticationService;

    public CloudbreakUser getCbUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication oauth = (OAuth2Authentication) authentication;
            if (oauth.getUserAuthentication() != null) {
                return authenticationService.getCloudbreakUser(oauth);
            }
        }
        return null;
    }

    public String getTokenValue(OAuth2Authentication auth) {
        return ((OAuth2AuthenticationDetails) auth.getDetails()).getTokenValue();
    }

    public static String getTenant(OAuth2Authentication oauth) {
        Jwt decodedJwt = JwtHelper.decode(((OAuth2AuthenticationDetails) oauth.getDetails()).getTokenValue());
        String tenant = "DEFAULT";
        try {
            Map<String, Object> claims = JsonUtil.readValue(decodedJwt.getClaims(), new MapTypeReference());
            if (claims.get("tenant_name") != null) {
                tenant = claims.get("tenant_name").toString();
            }
        } catch (IOException e) {
            LOGGER.warn("Can not get claims from token", e);
        }
        LOGGER.debug("Tenant_name claim from jwt token: {}", tenant);
        return tenant;
    }

    public String getServiceAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication oauth = (OAuth2Authentication) authentication;
            return oauth.getName();
        }
        return "";
    }
}
