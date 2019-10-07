package com.sequenceiq.periscope.service.security;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;

@Service
@Lazy
public class StackSecurityService {

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Cacheable(cacheNames = "stackAccessByOwnerCache")
    public boolean hasAccess(Long stackId, String owner, String permission) {
        return cloudbreakClient.autoscaleUserAuthorizationEndpoint().authorizeForAutoscale(stackId, owner, permission);
    }
}
