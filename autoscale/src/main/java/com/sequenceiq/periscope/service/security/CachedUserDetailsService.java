package com.sequenceiq.periscope.service.security;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.periscope.domain.PeriscopeUser;

@Service
@Lazy
public class CachedUserDetailsService {

    @Value("${periscope.client.secret}")
    private String clientSecret;

    @Inject
    private UserDetailsService cachedUserDetailsService;

    @Cacheable(cacheNames = "identityUserCache", key = "{ #username, #tenant }")
    public PeriscopeUser getDetails(String username, String tenant, UserFilterField filterField) {
        CloudbreakUser cloudbreakUser = cachedUserDetailsService.getDetails(username, tenant, filterField, clientSecret);
        return new PeriscopeUser(cloudbreakUser.getUserId(), cloudbreakUser.getUsername(), cloudbreakUser.getTenant());
    }

}
