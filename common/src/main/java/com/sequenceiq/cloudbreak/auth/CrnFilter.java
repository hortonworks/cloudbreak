package com.sequenceiq.cloudbreak.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class CrnFilter extends OncePerRequestFilter {

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public CrnFilter(ThreadBasedUserCrnProvider threadBasedUserCrnProvider) {
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userCrn = request.getHeader("x-cdp-actor-crn");
        threadBasedUserCrnProvider.setUserCrn(userCrn);
        filterChain.doFilter(request, response);
        threadBasedUserCrnProvider.removeUserCrn();
    }
}
