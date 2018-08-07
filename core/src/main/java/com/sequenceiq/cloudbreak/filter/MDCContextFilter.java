package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;

public class MDCContextFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCContextFilter.class);

    private static final String TRACKING_ID_HEADER = "trackingId";

    private final AuthenticatedUserService authenticatedUserService;

    public MDCContextFilter(AuthenticatedUserService authenticatedUserService) {
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        MDCBuilder.cleanupMdc();
        HttpServletRequestWrapper wrapper = new TrackingIdHeaderInjectingHttpRequestWrapper(request);
        MDCBuilder.addTrackingIdToMdcContext(wrapper.getHeader(TRACKING_ID_HEADER));
        LOGGER.debug("Tracking id has been added to MDC context for request, method: {}, path: {}",
                request.getMethod().toUpperCase(),
                request.getRequestURI());
        MDCBuilder.buildUserMdcContext(authenticatedUserService.getCbUser());
        filterChain.doFilter(wrapper, response);
    }

    private static class TrackingIdHeaderInjectingHttpRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> headerMap = new HashMap<>();

        private TrackingIdHeaderInjectingHttpRequestWrapper(HttpServletRequest request) {
            super(request);
            if (StringUtils.isEmpty(request.getHeader(TRACKING_ID_HEADER))) {
                String trackingId = UUID.randomUUID().toString();
                LOGGER.info("No trackingId in request. Adding trackingId: '{}'", trackingId);
                addHeader(TRACKING_ID_HEADER, trackingId);
            }
        }

        private void addHeader(String name, String value) {
            headerMap.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = super.getHeader(name);
            if (headerMap.containsKey(name)) {
                headerValue = headerMap.get(name);
            }
            return headerValue;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(headerMap.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> values = Collections.list(super.getHeaders(name));
            if (headerMap.containsKey(name)) {
                values.add(headerMap.get(name));
            }
            return Collections.enumeration(values);
        }
    }
}
