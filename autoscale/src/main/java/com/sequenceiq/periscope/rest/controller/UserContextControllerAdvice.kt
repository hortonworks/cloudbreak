package com.sequenceiq.periscope.rest.controller

import javax.servlet.http.HttpServletRequest

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

import com.sequenceiq.periscope.domain.PeriscopeUser

@ControllerAdvice
class UserContextControllerAdvice {

    @ModelAttribute("user")
    fun getRoles(request: HttpServletRequest): PeriscopeUser {
        return request.getAttribute("user") as PeriscopeUser
    }
}
