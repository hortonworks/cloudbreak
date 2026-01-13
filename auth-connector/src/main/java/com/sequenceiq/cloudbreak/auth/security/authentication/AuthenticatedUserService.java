package com.sequenceiq.cloudbreak.auth.security.authentication;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service
public class AuthenticatedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUserService.class);

    @Inject
    private AuthenticationService authenticationService;

    public CloudbreakUser getCbUser(HttpServletRequest request) {
        String userCrn = request.getHeader(ACTOR_CRN_HEADER);
        return authenticationService.getCloudbreakUser(userCrn);
    }
}
