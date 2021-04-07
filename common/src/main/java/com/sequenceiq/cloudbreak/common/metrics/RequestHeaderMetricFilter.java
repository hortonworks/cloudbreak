package com.sequenceiq.cloudbreak.common.metrics;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

public class RequestHeaderMetricFilter extends OncePerRequestFilter {

    public static final String CDP_CALLER_ID_HEADER = "cdp-caller-id";

    private final MetricService metricService;

    public RequestHeaderMetricFilter(MetricService metricService) {
        this.metricService = metricService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //TODO Metrics should be tagged with URL template
//        String callerIdGroup = Optional.ofNullable(request.getHeader(CDP_CALLER_ID_HEADER)).orElse("NA");
//        String method = request.getMethod();
//        String requestURI = request.getRequestURI();
//        metricService.incrementMetricCounter(MetricType.REST_OPERATION_CALLER_ID,
//                MetricTag.URI.name(), requestURI,
//                MetricTag.TARGET_METHOD.name(), method,
//                MetricTag.CALLER_ID.name(), callerIdGroup);

        filterChain.doFilter(request, response);
    }
}
