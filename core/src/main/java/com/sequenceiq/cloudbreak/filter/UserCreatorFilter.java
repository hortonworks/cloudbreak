package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.UserService;

public class UserCreatorFilter extends OncePerRequestFilter {

    private final UserService userService;

    private final AuthenticatedUserService authenticatedUserService;

    public UserCreatorFilter(UserService userService, AuthenticatedUserService authenticatedUserService) {
        this.userService = userService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        if (cloudbreakUser != null) {
            userService.getOrCreate(cloudbreakUser);
        }
        filterChain.doFilter(request, response);
    }
}
