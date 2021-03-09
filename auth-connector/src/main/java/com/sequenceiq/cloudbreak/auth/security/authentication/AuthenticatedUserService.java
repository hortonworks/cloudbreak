package com.sequenceiq.cloudbreak.auth.security.authentication;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service
public class AuthenticatedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUserService.class);

    @Inject
    private AuthenticationService authenticationService;

    public CloudbreakUser getCbUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authenticationService.getCloudbreakUser(authentication);
        }
        return null;
    }

    public String getAccountId() {
        CloudbreakUser cbUser = getCbUser();
        if (cbUser == null) {
            throw new AccessDeniedException("No authentication found in the SecurityContextHolder!");
        }
        return cbUser.getTenant();
    }

    public String getTokenValue(Authentication auth) {
        return ((CrnUser) auth.getPrincipal()).getUserCrn();
    }

    public String getServiceAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CrnUser) {
            CrnUser user = (CrnUser) authentication.getPrincipal();
            return user.getTenant();
        }
        return "";
    }
}
