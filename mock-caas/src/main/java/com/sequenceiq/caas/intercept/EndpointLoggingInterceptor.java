package com.sequenceiq.caas.intercept;

import static java.lang.String.format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

public class EndpointLoggingInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointLoggingInterceptor.class);

    private static final String ENDPOINT_CALL_LOG_FORMAT = "Endpoint called: %s";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LOGGER.info(format(ENDPOINT_CALL_LOG_FORMAT, request.getRequestURI()));
        return true;
    }

}
