package com.sequenceiq.cloudbreak.exception;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class ExceptionHandlerFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerFilter.class);

    private ErrorResponseHandler errorResponseHandler;

    public ExceptionHandlerFilter(ErrorResponseHandler errorResponseHandler) {
        this.errorResponseHandler = errorResponseHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            LOGGER.warn("Request handling failed", e);
            errorResponseHandler.handleErrorResponse(response, e);
        }
    }
}
