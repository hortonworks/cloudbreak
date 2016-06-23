package com.sequenceiq.cloudbreak.controller.advice

import javax.servlet.http.HttpServletRequest

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

import com.sequenceiq.cloudbreak.domain.CbUser

@ControllerAdvice
class UserContextControllerAdvice {

    @ModelAttribute("user")
    fun getRoles(request: HttpServletRequest): CbUser {
        return request.getAttribute("user") as CbUser
    }
}
