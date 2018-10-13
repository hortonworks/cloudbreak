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
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

@Service
public class ScimAccountGroupReaderFilter extends OncePerRequestFilter {

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException,
            IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            OAuth2Authentication oauth = (OAuth2Authentication) authentication;
            if (oauth.getUserAuthentication() != null) {
                String username = (String) authentication.getPrincipal();
                String tenant = AuthenticatedUserService.getTenant(oauth);
                CloudbreakUser user = cachedUserDetailsService.getDetails(username, tenant, UserFilterField.USERNAME);
                request.setAttribute("user", user);
            }
        }
        filterChain.doFilter(request, response);
    }
}
