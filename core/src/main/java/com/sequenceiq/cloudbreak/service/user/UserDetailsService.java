package com.sequenceiq.cloudbreak.service.user;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.sequenceiq.cloudbreak.domain.CbUser;

public interface UserDetailsService {

    @Cacheable(value = "userCache", key = "#filterValue")
    CbUser getDetails(String filterValue, UserFilterField filterField);

    @CacheEvict(value = "userCache", key = "#filterValue")
    void evictUserDetails(String updatedUserId, String filterValue);

    boolean hasResources(CbUser adminUser, String userId);

}
