package com.sequenceiq.cloudbreak.logger;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.logger.MdcContext.Builder;

public class MDCRequestIdOnlyFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCRequestIdOnlyFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        MDCBuilder.cleanupMdc();
        HttpServletRequestWrapper wrapper = new RequestIdHeaderInjectingHttpRequestWrapper(request);
        Builder builder = MdcContext.builder().requestId(wrapper.getHeader(REQUEST_ID_HEADER));
        builder.buildMdc();
        LOGGER.trace("Request id has been added to MDC context for request, method: {}, path: {}",
                request.getMethod().toUpperCase(Locale.ROOT),
                request.getRequestURI());
        try {
            filterChain.doFilter(wrapper, response);
        } finally {
            MDCBuilder.cleanupMdc();
        }
    }

    protected static class RequestIdHeaderInjectingHttpRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> headerMap = new HashMap<>();

        private RequestIdHeaderInjectingHttpRequestWrapper(HttpServletRequest request) {
            super(request);
            if (StringUtils.isEmpty(request.getHeader(REQUEST_ID_HEADER))) {
                String requestId = UUID.randomUUID().toString();
                LOGGER.debug("No requestId in request. Adding requestId: '{}'", requestId);
                addHeader(REQUEST_ID_HEADER, requestId);
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
