package com.sequenceiq.cloudbreak.service.user;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.RemoteUserDetailsService;
import com.sequenceiq.cloudbreak.common.service.UserFilterField;

@Service
@Lazy
public class UserDetailsService {

    @Value("${cb.client.secret}")
    private String clientSecret;

    @Inject
    private RemoteUserDetailsService remoteUserDetailsService;

    public IdentityUser getDetails(String username, UserFilterField filterField) {
        return remoteUserDetailsService.getDetails(username, filterField, clientSecret);
    }

    public void evictUserDetails(String updatedUserId, String filterValue) {
        remoteUserDetailsService.evictUserDetails(updatedUserId, filterValue);
    }
}
