package com.sequenceiq.cloudbreak.filter;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class MDCContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCContextFilter.class);

    private static final String TRACKING_ID_HEADER = "trackingId";

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        if (headers.containsKey(TRACKING_ID_HEADER)) {
            MDCBuilder.addTrackingIdToMdcContext(headers.getFirst(TRACKING_ID_HEADER));
            LOGGER.debug("Tracking id has been added to MDC context for request, method: {}, path: {}",
                    requestContext.getMethod().toUpperCase(),
                    requestContext.getUriInfo().getPath());
        }
        MDCBuilder.buildUserMdcContext(authenticatedUserService.getCbUser());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MDCBuilder.cleanupMdc();
    }
}
