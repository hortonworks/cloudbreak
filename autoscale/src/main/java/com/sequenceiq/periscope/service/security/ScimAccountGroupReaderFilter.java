package com.sequenceiq.periscope.service.security;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.periscope.domain.PeriscopeUser;

@Service
public class ScimAccountGroupReaderFilter extends OncePerRequestFilter {

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    @SuppressWarnings("unchecked")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException,
            IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            PeriscopeUser user = cachedUserDetailsService.getDetails(username, "tenant", UserFilterField.USERNAME);
            request.setAttribute("user", user);
        }
        filterChain.doFilter(request, response);
    }
}