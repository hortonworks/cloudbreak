package com.sequenceiq.cloudbreak.logger;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        Builder builder = MdcContext.builder();
        doIfNotNull(ThreadBasedUserCrnProvider.getUserCrn(), crn -> builder.userCrn(crn).tenant(ThreadBasedUserCrnProvider.getAccountId()));
        builder.buildMdc();
        if (mdcAppender != null) {
            mdcAppender.run();
        }
        filterChain.doFilter(request, response);
    }
}
