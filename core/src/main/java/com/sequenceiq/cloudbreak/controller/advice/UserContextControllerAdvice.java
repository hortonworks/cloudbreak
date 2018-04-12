package com.sequenceiq.cloudbreak.controller.advice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

@ControllerAdvice
public class UserContextControllerAdvice {

    @ModelAttribute("user")
    public IdentityUser getRoles(HttpServletRequest request) {
        return (IdentityUser) request.getAttribute("user");
    }
}
