package com.sequenceiq.cloudbreak.service.user;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;

@Service
@Lazy
public class CachedUserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedUserDetailsService.class);

    @Value("${cb.client.secret}")
    private String clientSecret;

    @Inject
    private UserDetailsService cachedUserDetailsService;

    @Inject
    private UserService userService;

    @Cacheable(cacheNames = "identityUserCache", key = "#username")
    public IdentityUser getDetails(String username, UserFilterField filterField) {
        IdentityUser identityUser = cachedUserDetailsService.getDetails(username, filterField, clientSecret);
        //ensure that the user is created into our database
        userService.getOrCreate(identityUser);
        return identityUser;
    }

    @CacheEvict(value = "identityUserCache", key = "#username")
    public void evictUserDetails(String updatedUserId, String username) {
        LOGGER.debug("Remove userid: {} / username: {} from user cache", updatedUserId, username);
    }
}
