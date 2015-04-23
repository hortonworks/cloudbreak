package com.sequenceiq.cloudbreak.service.user;

import org.springframework.cache.annotation.Cacheable;

import com.sequenceiq.cloudbreak.domain.CbUser;

public interface UserDetailsService {

    @Cacheable("userCache")
    CbUser getDetails(String fieldValue, UserFilterField filterField);

}
