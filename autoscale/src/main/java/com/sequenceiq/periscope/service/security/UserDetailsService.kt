package com.sequenceiq.periscope.service.security

import org.springframework.cache.annotation.Cacheable

import com.sequenceiq.periscope.domain.PeriscopeUser

interface UserDetailsService {

    @Cacheable("userCache")
    fun getDetails(fieldValue: String, filterField: UserFilterField): PeriscopeUser

}
