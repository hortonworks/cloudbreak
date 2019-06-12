package com.sequenceiq.redbeams.filter;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A filter that generates a new request ID and sets it as a request header,
 * but only if a request ID is not already present.
 */
public class RequestIdGeneratingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdGeneratingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequestWrapper wrapper = new RequestIdHeaderInjectingHttpRequestWrapper(request);
        filterChain.doFilter(wrapper, response);
    }

    @VisibleForTesting
    static class RequestIdHeaderInjectingHttpRequestWrapper extends HttpServletRequestWrapper {

        @VisibleForTesting
        static final String REQUEST_ID_HEADER = "x-cdp-request-id";

        private final String requestId;

        private final boolean generatedRequestId;

        RequestIdHeaderInjectingHttpRequestWrapper(HttpServletRequest request) {
            super(request);

            String requestIdHeader = request.getHeader(REQUEST_ID_HEADER);
            generatedRequestId = StringUtils.isEmpty(requestIdHeader);
            if (generatedRequestId) {
                requestId = UUID.randomUUID().toString();
                LOGGER.info("No request ID in request, created one: {}", requestId);
            } else {
                requestId = requestIdHeader;
            }
        }

        @Override
        public String getHeader(String name) {
            return REQUEST_ID_HEADER.equals(name) ? requestId : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            if (generatedRequestId) {
                names.add(REQUEST_ID_HEADER);
            }
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return REQUEST_ID_HEADER.equals(name) ? Collections.enumeration(List.of(requestId)) : super.getHeaders(name);
        }
    }
}
