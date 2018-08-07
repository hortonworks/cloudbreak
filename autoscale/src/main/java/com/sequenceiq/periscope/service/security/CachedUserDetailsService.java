package com.sequenceiq.periscope.service.security;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
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

    @Cacheable(cacheNames = "userCache", key = "#username")
    public PeriscopeUser getDetails(String username, UserFilterField filterField) {
        IdentityUser identityUser = cachedUserDetailsService.getDetails(username, filterField, clientSecret);
        return new PeriscopeUser(identityUser.getUserId(), identityUser.getUsername(), identityUser.getAccount());
    }

}
