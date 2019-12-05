package com.sequenceiq.cloudbreak.logger;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

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

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MdcContext.Builder;

public class MDCContextFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCContextFilter.class);

    private final Runnable mdcAppender;

    public MDCContextFilter(Runnable mdcAppender) {
        this.mdcAppender = mdcAppender;
    }

    public MDCContextFilter() {
        this(() -> {
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        MDCBuilder.cleanupMdc();
        HttpServletRequestWrapper wrapper = new RequestIdHeaderInjectingHttpRequestWrapper(request);
        Builder builder = MdcContext.builder().requestId(wrapper.getHeader(REQUEST_ID_HEADER));
        doIfNotNull(ThreadBasedUserCrnProvider.getUserCrn(), crn -> builder.userCrn(crn).tenant(ThreadBasedUserCrnProvider.getAccountId()));
        builder.buildMdc();
        LOGGER.trace("Request id has been added to MDC context for request, method: {}, path: {}",
                request.getMethod().toUpperCase(),
                request.getRequestURI());
        if (mdcAppender != null) {
            mdcAppender.run();
        }
        filterChain.doFilter(wrapper, response);
    }

    private static class RequestIdHeaderInjectingHttpRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> headerMap = new HashMap<>();

        private RequestIdHeaderInjectingHttpRequestWrapper(HttpServletRequest request) {
            super(request);
            if (StringUtils.isEmpty(request.getHeader(REQUEST_ID_HEADER))) {
                String requestId = UUID.randomUUID().toString();
                LOGGER.trace("No requestId in request. Adding requestId: '{}'", requestId);
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
