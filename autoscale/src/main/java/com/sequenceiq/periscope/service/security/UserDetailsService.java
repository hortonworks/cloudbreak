package com.sequenceiq.periscope.service.security;

import org.springframework.cache.annotation.Cacheable;

import com.sequenceiq.periscope.domain.PeriscopeUser;

public interface UserDetailsService {

    @Cacheable("userCache")
    PeriscopeUser getDetails(String fieldValue, UserFilterField filterField);

}
