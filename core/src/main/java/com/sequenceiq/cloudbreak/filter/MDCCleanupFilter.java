package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class MDCCleanupFilter implements ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCCleanupFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MDCBuilder.cleanupMdc();
    }
}
