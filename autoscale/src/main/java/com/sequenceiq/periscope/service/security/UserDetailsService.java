package com.sequenceiq.periscope.service.security;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.CachedUserDetailsService;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.periscope.domain.PeriscopeUser;

@Service
@Lazy
public class UserDetailsService {

    @Value("${periscope.client.secret}")
    private String clientSecret;

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    public PeriscopeUser getDetails(String fieldValue, UserFilterField filterField) {
        IdentityUser identityUser = cachedUserDetailsService.getDetails(fieldValue, filterField, clientSecret);
        return new PeriscopeUser(identityUser.getUserId(), identityUser.getUsername(), identityUser.getAccount());
    }

}
