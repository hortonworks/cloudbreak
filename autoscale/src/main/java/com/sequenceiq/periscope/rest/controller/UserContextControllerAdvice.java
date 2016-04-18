package com.sequenceiq.periscope.rest.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.sequenceiq.periscope.domain.PeriscopeUser;

@ControllerAdvice
public class UserContextControllerAdvice {

    @ModelAttribute("user")
    public PeriscopeUser getRoles(HttpServletRequest request) {
        return (PeriscopeUser) request.getAttribute("user");
    }
}
