package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.REQUEST_DETAILS;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.REQUEST_TIME;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.RESPONSE_DETAILS;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.RESPONSE_LOGGING_STREAM;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.lang3.BooleanUtils;
import org.glassfish.jersey.message.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterOrder;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser;
import com.sequenceiq.cloudbreak.structuredevent.util.LoggingStream;
import com.sequenceiq.cloudbreak.structuredevent.util.RestFilterPropertyUtil;
import com.sequenceiq.cloudbreak.structuredevent.util.RestFilterRequestBodyLogger;

/**
 * Inspects all requests to a service and determines if it should mark the request or response up with additional Structured Event metadata.
 * <p>
 * Uses implementations of {@code CDPRestUrlParser} to determine if additional actions should be taken on a request.
 * <p>
 * Activation of this filter is controlled by the {@code contentLogging} configuration value.
 * <p>
 * To use this class, it should be registered in an {@code EndpointConfig}.
 */
@Component
@Priority(CDPJaxRsFilterOrder.CDP_STRUCTURED_EVENT_FILTER_ORDER)
public class CDPStructuredEventFilter implements WriterInterceptor, ContainerRequestFilter, ContainerResponseFilter {

    private static final String LOGGING_ENABLED_PROPERTY = "structuredevent.loggingEnabled";

    private static final String REST_PARAMS = "REST_PARAMS";

    @Value("${cdp.structuredevent.rest.contentlogging}")
    private Boolean contentLogging;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CdpOperationDetailsFactory restEventFilterRelatedObjectFactory;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private StructuredEventFilterUtil structuredEventFilterUtil;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private List<CDPRestUrlParser> cdpRestUrlParsers;

    /**
     * Add structured event parameters to the properties to the request context.
     *
     * @param requestContext the context on which to attach additional properties.
     * @throws IOException if the request entity stream cannot be read correctly.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean loggingEnabled = isLoggingEnabled(requestContext);
        requestContext.setProperty(LOGGING_ENABLED_PROPERTY, loggingEnabled);
        if (loggingEnabled) {
            requestContext.setProperty(REQUEST_TIME, System.currentTimeMillis());
            StringBuilder body = new StringBuilder();
            requestContext.setEntityStream(RestFilterRequestBodyLogger.logInboundEntity(body, requestContext.getEntityStream(),
                    MessageUtils.getCharset(requestContext.getMediaType()), contentLogging));
            requestContext.setProperty(REST_PARAMS, getRequestUrlParameters(requestContext));
            requestContext.setProperty(REQUEST_DETAILS, RestFilterPropertyUtil.createRequestDetails(requestContext, body.toString()));
        }
    }

    /**
     * On response, attaches a logging stream to the request context, or sends a structured event.
     * <p>
     * The filter essentially helps send a structured event or sends it itself.
     * <ul>
     *     <li>When there is a response entity, {@code LoggingStream} is attached and used in {@code aroundWriteTo} to send a structured event.</li>
     *     <li>When there isn't a response entity, send a structured event directly.</li>
     * </ul>
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (BooleanUtils.isTrue((Boolean) requestContext.getProperty(LOGGING_ENABLED_PROPERTY))) {
            RestResponseDetails restResponse = RestFilterPropertyUtil.createResponseDetails(responseContext);
            if (responseContext.hasEntity()) {
                RestFilterPropertyUtil.extractAndSetResponseEntityStreamIfAbsent(requestContext, responseContext, contentLogging);
                RestFilterPropertyUtil.setPropertyIfAbsent(requestContext, RESPONSE_DETAILS, restResponse);
            } else {
                Long requestTime = (Long) requestContext.getProperty(REQUEST_TIME);
                RestRequestDetails restRequest = (RestRequestDetails) requestContext.getProperty(REQUEST_DETAILS);
                Map<String, String> restParams = (Map<String, String>) requestContext.getProperty(REST_PARAMS);
                structuredEventFilterUtil.sendStructuredEvent(restRequest, restResponse, restParams, requestTime, "");
            }
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.proceed();
        if (BooleanUtils.isTrue((Boolean) context.getProperty(LOGGING_ENABLED_PROPERTY))) {
            sendStructuredEvent(context);
        }
    }

    private void sendStructuredEvent(WriterInterceptorContext context) {
        Long requestTime = (Long) context.getProperty(REQUEST_TIME);
        RestRequestDetails restRequest = (RestRequestDetails) context.getProperty(REQUEST_DETAILS);
        RestResponseDetails restResponse = (RestResponseDetails) context.getProperty(RESPONSE_DETAILS);
        String responseBody = ((LoggingStream) context.getProperty(RESPONSE_LOGGING_STREAM)).getStringBuilder(
                MessageUtils.getCharset(context.getMediaType())).toString();
        Map<String, String> restParams = (Map<String, String>) context.getProperty(REST_PARAMS);
        if (restParams == null) {
            restParams = new HashMap<>();
        }
        structuredEventFilterUtil.sendStructuredEvent(restRequest, restResponse, restParams, requestTime, responseBody);
    }

    /**
     * Iterates through implementations of {@code CDPRestUrlParser} to extract URL parameters if possible.
     * <p>
     * The list of URL parsers is autowired by Spring.
     *
     * @param requestContext the source of URL parameters, parseable by one of the REST URL parsers
     * @return a Map of constant names from {@code CDPRestUrlParser} to their values in the URL
     */
    private Map<String, String> getRequestUrlParameters(ContainerRequestContext requestContext) {
        Map<String, String> params = Maps.newHashMap();
        for (CDPRestUrlParser cdpRestUrlParser : cdpRestUrlParsers) {
            if (cdpRestUrlParser.fillParams(requestContext, params)) {
                break;
            }
        }
        return params;
    }

    private boolean isLoggingEnabled(ContainerRequestContext requestContext) {
        return !"GET".equals(requestContext.getMethod());
    }
}
