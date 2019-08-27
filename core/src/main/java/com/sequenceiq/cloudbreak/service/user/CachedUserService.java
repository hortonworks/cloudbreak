package com.sequenceiq.cloudbreak.service.user;

import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Service
public class CachedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedUserService.class);

    @Cacheable(cacheNames = "userCache", key = "#cloudbreakUser")
    public User getUser(CloudbreakUser cloudbreakUser, Function<CloudbreakUser, User> findByTenantAndUsername, Function<CloudbreakUser, User> createUser) {
        try {
            return getOrCreateUser(cloudbreakUser, findByTenantAndUsername, createUser);
        } catch (TransactionRuntimeExecutionException e) {
            LOGGER.debug("User is already created, possibly on another thread", e);
            return getOrCreateUser(cloudbreakUser, findByTenantAndUsername, createUser);
        }
    }

    @CacheEvict(cacheNames = "userCache", key = "#cloudbreakUser")
    public void evictByIdentityUser(CloudbreakUser cloudbreakUser) {
        LOGGER.debug("Remove user from user cache, tenant: {} / username: {} ", cloudbreakUser.getTenant(), cloudbreakUser.getUserId());
    }

    @CacheEvict(value = "userCache", allEntries = true)
    public void evictUser(User user) {
        LOGGER.debug("Remove all from user cache");
    }

    private User getOrCreateUser(CloudbreakUser cloudbreakUser, Function<CloudbreakUser, User> findUser, Function<CloudbreakUser, User> createUser) {
        return Optional.ofNullable(findUser.apply(cloudbreakUser)).orElseGet(() -> createUser.apply(cloudbreakUser));
    }

}
