package com.sequenceiq.cloudbreak.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField

@Service
class AuthenticatedUserService {

    @Autowired
    private val userDetailsService: UserDetailsService? = null

    val cbUser: CbUser?
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
