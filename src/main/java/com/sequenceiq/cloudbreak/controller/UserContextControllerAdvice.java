package com.sequenceiq.cloudbreak.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.sequenceiq.cloudbreak.domain.CbUser;

@ControllerAdvice
public class UserContextControllerAdvice {

    @ModelAttribute("user")
    public CbUser getRoles(HttpServletRequest request) {
        return (CbUser) request.getAttribute("user");
    }
}
