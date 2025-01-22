package com.sequenceiq.cloudbreak.auth.security.authentication;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service
public class AuthenticatedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUserService.class);

    private static final String ACTOR_HEADER = "x-cdp-actor-crn";

    @Inject
    private AuthenticationService authenticationService;

    public CloudbreakUser getCbUser(HttpServletRequest request) {
        String userCrn = request.getHeader(ACTOR_HEADER);
        return authenticationService.getCloudbreakUser(userCrn);
    }
}
