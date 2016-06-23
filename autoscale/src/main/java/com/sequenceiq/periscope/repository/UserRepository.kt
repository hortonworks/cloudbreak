package com.sequenceiq.periscope.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.periscope.domain.PeriscopeUser

interface UserRepository : CrudRepository<PeriscopeUser, String> {

    fun findOneByName(@Param("email") email: String): PeriscopeUser

}
