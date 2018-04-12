package com.sequenceiq.cloudbreak.controller.advice;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class UserContextControllerAdvice {

    @ModelAttribute("user")
    public IdentityUser getRoles(HttpServletRequest request) {
        return (IdentityUser) request.getAttribute("user");
    }
}
