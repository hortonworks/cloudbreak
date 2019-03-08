package com.sequenceiq.cloudbreak.service.security;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;

@Service
public class ScimAccountGroupReaderFilter extends OncePerRequestFilter {

    @Inject
    private AuthUserService authUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            OAuth2Authentication oauth = (OAuth2Authentication) authentication;
            if (oauth.getUserAuthentication() != null) {
                CloudbreakUser user = authUserService.getUserWithCaasFallback(oauth);
                request.setAttribute("user", user);
            }
        }
        filterChain.doFilter(request, response);
    }
}
