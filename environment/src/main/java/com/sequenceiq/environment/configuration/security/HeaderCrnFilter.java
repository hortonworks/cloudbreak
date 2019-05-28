package com.sequenceiq.environment.configuration.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class HeaderCrnFilter extends OncePerRequestFilter {

    private static final String INFO_SERVLET_PATH = "/info";

    private final ThreadLocalUserCrnProvider threadLocalUserCrnProvider;

    public HeaderCrnFilter(ThreadLocalUserCrnProvider threadLocalUserCrnProvider) {
        this.threadLocalUserCrnProvider = threadLocalUserCrnProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userCrn = request.getHeader("x-cdp-actor-crn");
        if (isNotInfoEndpoint(request)) {
            if (!Crn.isCrn(userCrn)) {
                throw new AccessDeniedException("Provided user CRN is invalid!");
            }
        }
        threadLocalUserCrnProvider.setUserCrn(userCrn);
        filterChain.doFilter(request, response);
        threadLocalUserCrnProvider.removeUserCrn();
    }

    private boolean isNotInfoEndpoint(HttpServletRequest request) {
        return !request.getServletPath().equalsIgnoreCase(INFO_SERVLET_PATH);
    }
}
