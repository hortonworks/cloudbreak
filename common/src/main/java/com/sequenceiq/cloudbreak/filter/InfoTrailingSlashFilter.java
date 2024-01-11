package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

public class InfoTrailingSlashFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        if (request.getRequestURI().equals(contextPath + "/info/")) {
            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return contextPath + "/info";
                }
            };
        }
        filterChain.doFilter(request, response);
    }

}
