package com.sequenceiq.redbeams.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.service.ThreadBasedRequestIdProvider;

/**
 * A filter that establishes a fresh MDC for each request. The MDC contains the
 * request ID and user CRN information.
 */
public class MDCFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCFilter.class);

    private final ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    private final ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

    public MDCFilter(ThreadBasedRequestIdProvider threadBasedRequestIdProvider, ThreadBasedUserCrnProvider threadBaseUserCrnProvider) {
        this.threadBasedRequestIdProvider = threadBasedRequestIdProvider;
        this.threadBaseUserCrnProvider = threadBaseUserCrnProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        MDCBuilder.cleanupMdc();

        String requestId = threadBasedRequestIdProvider.getRequestId();
        MDCBuilder.addRequestIdToMdcContext(requestId);
        LOGGER.info("Added request ID {} to MDC", requestId);

        String userCrnString = threadBaseUserCrnProvider.getUserCrn();
        try {
            MDCBuilder.buildMdcContextFromCrn(Crn.safeFromString(userCrnString));
            LOGGER.info("Added user CRN information from {} to MDC", userCrnString);
        } catch (CrnParseException e) {
            LOGGER.warn("Failed to parse user CRN, not including its information in MDC", e);
        }

        filterChain.doFilter(request, response);
    }
}
