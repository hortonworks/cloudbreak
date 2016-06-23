package com.sequenceiq.cloudbreak.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.UserEndpoint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.api.model.UserRequest
import com.sequenceiq.cloudbreak.service.user.UserDetailsService

@Component
class UserController : UserEndpoint {

    @Autowired
    private val userDetailsService: UserDetailsService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun evictUserDetails(id: String, userRequest: UserRequest): String {
        userDetailsService!!.evictUserDetails(id, userRequest.username)
        return userRequest.username
    }

    override fun hasResources(id: String): Boolean? {
        val user = authenticatedUserService!!.cbUser
        val hasResources = userDetailsService!!.hasResources(user, id)
        return hasResources
    }

}
