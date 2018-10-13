package com.sequenceiq.periscope.service;

import javax.inject.Inject;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.service.security.CachedUserDetailsService;

@Service
public class AuthenticatedUserService {

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    public PeriscopeUser getPeriscopeUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication oauth = (OAuth2Authentication) authentication;
            if (oauth.getUserAuthentication() != null) {
                String username = (String) authentication.getPrincipal();
                return cachedUserDetailsService.getDetails(username, "tenant", UserFilterField.USERNAME);
            }
        }
        return null;
    }
}
