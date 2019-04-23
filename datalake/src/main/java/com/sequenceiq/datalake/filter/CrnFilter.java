package com.sequenceiq.datalake.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.datalake.util.RestRequestThreadLocalService;

public class CrnFilter extends OncePerRequestFilter {

    private final RestRequestThreadLocalService restRequestThreadLocalService;

    public CrnFilter(RestRequestThreadLocalService restRequestThreadLocalService) {
        this.restRequestThreadLocalService = restRequestThreadLocalService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userCrn = request.getHeader("x-cdp-actor-crn");
        restRequestThreadLocalService.setUserCrn(userCrn);
        filterChain.doFilter(request, response);
        restRequestThreadLocalService.removeUserCrn();
    }
}
