package com.sequenceiq.cloudbreak.service.user;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Service
public class CachedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedUserService.class);

    @Cacheable(cacheNames = "userCache", key = "#cloudbreakUser")
    public User getUser(CloudbreakUser cloudbreakUser, BiFunction<String, String, User> findByTenantAndUsername, Function<CloudbreakUser, User> createUser) {
        return Optional.ofNullable(findByTenantAndUsername.apply(cloudbreakUser.getTenant(), cloudbreakUser.getUserId()))
                .orElseGet(() -> createUser.apply(cloudbreakUser));
    }

    @CacheEvict(cacheNames = "userCache", key = "#cloudbreakUser")
    public void evictByIdentityUser(CloudbreakUser cloudbreakUser) {
        LOGGER.debug("Remove user from user cache, tenant: {} / username: {} ", cloudbreakUser.getTenant(), cloudbreakUser.getUserId());
    }

    @CacheEvict(value = "userCache", allEntries = true)
    public void evictUser(User user) {
        LOGGER.debug("Remove all from user cache");
    }
}
