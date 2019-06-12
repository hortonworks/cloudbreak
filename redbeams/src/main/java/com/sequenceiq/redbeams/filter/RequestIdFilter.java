package com.sequenceiq.redbeams.filter;

import com.sequenceiq.redbeams.service.ThreadBasedRequestIdProvider;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A filter that sets the value of the request ID header in a thread-local.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    private final ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    public RequestIdFilter(ThreadBasedRequestIdProvider threadBasedRequestIdProvider) {
        this.threadBasedRequestIdProvider = threadBasedRequestIdProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String requestId = request.getHeader("x-cdp-request-id");
        threadBasedRequestIdProvider.setRequestId(requestId);
        filterChain.doFilter(request, response);
        threadBasedRequestIdProvider.removeRequestId();
    }
}
