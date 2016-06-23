package com.sequenceiq.periscope.service

import com.sequenceiq.periscope.service.security.UserDetailsService
import com.sequenceiq.periscope.service.security.UserFilterField
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.stereotype.Service

import com.sequenceiq.periscope.domain.PeriscopeUser

@Service
class AuthenticatedUserService {

    @Autowired
    private val userDetailsService: UserDetailsService? = null

    val periscopeUser: PeriscopeUser?
        get() {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null) {
                val oauth = authentication as OAuth2Authentication?
                if (oauth.getUserAuthentication() != null) {
                    val username = authentication.principal as String
                    return userDetailsService!!.getDetails(username, UserFilterField.USERNAME)
                }
            }
            return null
        }
}
