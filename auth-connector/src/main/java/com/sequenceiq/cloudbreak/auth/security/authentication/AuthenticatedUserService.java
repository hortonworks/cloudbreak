package com.sequenceiq.cloudbreak.auth.security.authentication;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service
public class AuthenticatedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUserService.class);

    private static final String ACTOR_HEADER = "x-cdp-actor-crn";

    @Inject
    private AuthenticationService authenticationService;

    public CloudbreakUser getCbUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authenticationService.getCloudbreakUser(authentication);
        }
        return null;
    }

    public CloudbreakUser getCbUser(String userCrn) {
        if (StringUtils.isNotBlank(userCrn) && Crn.isCrn(userCrn)) {
            return authenticationService.getCloudbreakUser(userCrn, null);
        }
        return getCbUser();
    }

    public CloudbreakUser getCbUser(HttpServletRequest request) {
        String userCrn = request.getHeader(ACTOR_HEADER);
        return getCbUser(userCrn);
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
