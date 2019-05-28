package com.sequenceiq.environment.configuration.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticationService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class DisabledSecurityCrnFilter extends OncePerRequestFilter {

    private final ThreadLocalUserCrnProvider threadLocalUserCrnProvider;

    private final AuthenticationService authenticationService;

    public DisabledSecurityCrnFilter(ThreadLocalUserCrnProvider threadLocalUserCrnProvider, AuthenticationService authenticationService) {
        this.threadLocalUserCrnProvider = threadLocalUserCrnProvider;
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CloudbreakUser cloudbreakUser = authenticationService.getCloudbreakUser(null);
        threadLocalUserCrnProvider.setUserCrn(cloudbreakUser.getUserCrn());
        filterChain.doFilter(request, response);
        threadLocalUserCrnProvider.removeUserCrn();
    }
}
