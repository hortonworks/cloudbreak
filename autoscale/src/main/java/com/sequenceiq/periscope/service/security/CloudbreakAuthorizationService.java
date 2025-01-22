package com.sequenceiq.periscope.service.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;

@Service
@Lazy
public class CloudbreakAuthorizationService {

    @Inject
    private CloudbreakInternalCrnClient cloudbreakClient;

    @Cacheable(cacheNames = "stackAccessByUserIdAndTenantCache")
    public void hasAccess(String stackCrn, String userId, String tenant, String permission) {
        if (!cloudbreakClient.withInternalCrn().autoscaleEndpoint().authorizeForAutoscale(stackCrn, userId, tenant, permission).isSuccess()) {
            throw new ForbiddenException(String.format("Accessing to stack '%s' is not allowed for '%s' in '%s'", stackCrn, userId, tenant));
        }
    }
}
