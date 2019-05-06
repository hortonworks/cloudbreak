package com.sequenceiq.cloudbreak.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationService.class);

    @PreAuthorize("hasPermission(#target,#permission)")
    public void hasPermission(Object target, String permission) {
        LOGGER.debug("User has permission to {} resource: {}", permission, target);
    }
}
