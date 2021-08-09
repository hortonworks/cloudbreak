package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.lang3.BooleanUtils;
import org.glassfish.jersey.message.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser;

/**
 * Inspects all requests to a service and determines if it should mark the request or response up with additional Structured Event metadata.
 *
 * Uses implementations of {@code CDPRestUrlParser} to determine if additional actions should be taken on a request.
 *
 * Activation of this filter is controlled by the {@code contentLogging} configuration value.
 *
 * To use this class, it should be registered in an {@code EndpointConfig}.
 */
@Component
public class CDPStructuredEventFilter implements WriterInterceptor, ContainerRequestFilter, ContainerResponseFilter {

    private static final String LOGGING_ENABLED_PROPERTY = "structuredevent.loggingEnabled";

    private static final String LOGGINGSTREAM_PROPERTY = "structuredevent.entityLogger";

    private static final String REST_PARAMS = "REST_PARAMS";

    private static final String REQUEST_TIME = "REQUEST_TIME";

    private static final String REQUEST_DETAILS = "REQUEST_DETAIS";

    private static final String RESPONSE_DETAILS = "RESPONSE_DETAIS";

    @Value("${cdp.structuredevent.rest.contentlogging}")
    private Boolean contentLogging;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private RestEventFilterRelatedObjectFactory restEventFilterRelatedObjectFactory;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private StructuredEventFilterUtil structuredEventFilterUtil;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private List<CDPRestUrlParser> cdpRestUrlParsers;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean loggingEnabled = isLoggingEnabled(requestContext);
        requestContext.setProperty(LOGGING_ENABLED_PROPERTY, loggingEnabled);
        if (loggingEnabled) {
            requestContext.setProperty(REQUEST_TIME, System.currentTimeMillis());
            StringBuilder body = new StringBuilder();
            requestContext.setEntityStream(structuredEventFilterUtil.logInboundEntity(body, requestContext.getEntityStream(),
                    MessageUtils.getCharset(requestContext.getMediaType())));
            requestContext.setProperty(REST_PARAMS, getRequestUrlParameters(requestContext));
            requestContext.setProperty(REQUEST_DETAILS, restEventFilterRelatedObjectFactory.createRequestDetails(requestContext, body.toString()));
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (BooleanUtils.isTrue((Boolean) requestContext.getProperty(LOGGING_ENABLED_PROPERTY))) {
            RestResponseDetails restResponse = restEventFilterRelatedObjectFactory.createResponseDetails(responseContext);
            if (responseContext.hasEntity()) {
                OutputStream stream = new LoggingStream(responseContext.getEntityStream(), contentLogging);
                responseContext.setEntityStream(stream);
                requestContext.setProperty(LOGGINGSTREAM_PROPERTY, stream);
                requestContext.setProperty(RESPONSE_DETAILS, restResponse);
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
        String responseBody = ((LoggingStream) context.getProperty(LOGGINGSTREAM_PROPERTY)).getStringBuilder(
                MessageUtils.getCharset(context.getMediaType())).toString();
        Map<String, String> restParams = (Map<String, String>) context.getProperty(REST_PARAMS);
        if (restParams == null) {
            restParams = new HashMap<>();
        }
        structuredEventFilterUtil.sendStructuredEvent(restRequest, restResponse, restParams, requestTime, responseBody);
    }

    /**
     * Iterates through implementations of {@code CDPRestUrlParser} to extract URL parameters if possible.
     *
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
