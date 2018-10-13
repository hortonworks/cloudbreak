package com.sequenceiq.cloudbreak.service.user;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
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

    @Cacheable(cacheNames = "identityUserCache", key = "{ #username, #tenant }")
    public CloudbreakUser getDetails(String username, String tenant, UserFilterField filterField) {
        CloudbreakUser cloudbreakUser = cachedUserDetailsService.getDetails(username, tenant, filterField, clientSecret);
        //ensure that the user is created into our database
        userService.getOrCreate(cloudbreakUser);
        return cloudbreakUser;
    }

    @CacheEvict(value = "identityUserCache", key = "{ #username, #tenant }")
    public void evictUserDetails(String updatedUserId, String username, String tenant) {
        LOGGER.debug("Remove userid: {} / username: {} / tenant: {} from user cache", updatedUserId, username, tenant);
    }
}
