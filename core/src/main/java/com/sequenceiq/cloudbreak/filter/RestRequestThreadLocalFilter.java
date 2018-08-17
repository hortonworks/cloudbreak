package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;

public class RestRequestThreadLocalFilter extends OncePerRequestFilter {

    private final RestRequestThreadLocalService restRequestThreadLocalService;

    public RestRequestThreadLocalFilter(RestRequestThreadLocalService restRequestThreadLocalService) {
        this.restRequestThreadLocalService = restRequestThreadLocalService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/v3/organization/")) {
            Long orgId = Long.valueOf(StringUtils.substringBetween(requestURI, "/v3/organization/", "/"));
            restRequestThreadLocalService.setRequestedOrgId(orgId);
        } else {
            restRequestThreadLocalService.setRequestedOrgId(null);
        }
        filterChain.doFilter(request, response);
    }
}
