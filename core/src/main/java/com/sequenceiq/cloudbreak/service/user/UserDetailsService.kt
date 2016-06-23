package com.sequenceiq.cloudbreak.service.user

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable

import com.sequenceiq.cloudbreak.domain.CbUser

interface UserDetailsService {

    @Cacheable(value = "userCache", key = "#filterValue")
    fun getDetails(filterValue: String, filterField: UserFilterField): CbUser

    @CacheEvict(value = "userCache", key = "#filterValue")
    fun evictUserDetails(updatedUserId: String, filterValue: String)

    fun hasResources(adminUser: CbUser, userId: String): Boolean

}
