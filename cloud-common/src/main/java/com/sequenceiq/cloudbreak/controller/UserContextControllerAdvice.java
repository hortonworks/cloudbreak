package com.sequenceiq.cloudbreak.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@ControllerAdvice
public class UserContextControllerAdvice {

    @ModelAttribute("user")
    public CloudbreakUser getRoles(HttpServletRequest request) {
        return (CloudbreakUser) request.getAttribute("user");
    }
}
