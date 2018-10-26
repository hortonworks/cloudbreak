package com.sequenceiq.periscope.service.security;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;

@Service
@Lazy
public class CloudbreakAuthorizationService {

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Cacheable(cacheNames = "stackAccessByUserIdAndTenantCache")
    public void hasAccess(Long stackId, String userId, String tenant, String permission) {
        if (!cloudbreakClient.autoscaleEndpoint().authorizeForAutoscale(stackId, userId, tenant, permission)) {
            throw new AccessDeniedException(String.format("Accessing to stack '%s' is not allowed for '%s' in '%s'", stackId, userId, tenant));
        }
    }
}
