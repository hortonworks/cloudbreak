package com.sequenceiq.cloudbreak.service.user;

import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Service
public class CachedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedUserService.class);

    @Cacheable(cacheNames = "userCache", key = "#identityUser")
    public User getUser(IdentityUser identityUser, Function<String, User> findByIdentityUser, Function<IdentityUser, User> createUser) {
        return Optional.ofNullable(findByIdentityUser.apply(identityUser.getUsername())).orElseGet(() -> createUser.apply(identityUser));
    }

    @CacheEvict(cacheNames = "userCache", key = "#identityUser")
    public void evictByIdentityUser(IdentityUser identityUser) {
        LOGGER.debug("Remove userid: {} / username: {} from user cache", identityUser.getUserId(), identityUser.getUsername());
    }

    @CacheEvict(value = "userCache", allEntries = true)
    public void evictUser(User user) {
        LOGGER.debug("Remove all from user cache");
    }
}
