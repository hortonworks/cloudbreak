package com.sequenceiq.cloudbreak.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationService.class);

    @PreAuthorize("hasPermission(#target,'read')")
    public void hasReadPermission(Object target) {
        LOGGER.debug("User has permission to read resource: {}", target);
    }

    @PreAuthorize("hasPermission(#target,'write')")
    public void hasWritePermission(Object target) {
        LOGGER.debug("User has permission to write resource: {}", target);
    }
}
