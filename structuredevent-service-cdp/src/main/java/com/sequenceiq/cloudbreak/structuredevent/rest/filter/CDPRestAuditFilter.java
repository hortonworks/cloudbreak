package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.REQUEST_DETAILS;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.REQUEST_TIME;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.RESPONSE_DETAILS;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.RESPONSE_LOGGING_STREAM;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestAuditUrlParser;
import com.sequenceiq.cloudbreak.structuredevent.util.LoggingStream;
import com.sequenceiq.cloudbreak.structuredevent.util.RestFilterPropertyUtil;
import com.sequenceiq.cloudbreak.structuredevent.util.RestFilterRequestBodyLogger;

@Component
@Priority(CDPJaxRsFilterOrder.CDP_REST_AUDIT_FILTER_ORDER)
public class CDPRestAuditFilter implements WriterInterceptor, ContainerRequestFilter, ContainerResponseFilter {

    private static final String REST_STRICT_AUDIT_ENABLED = "rest.strict.audit.enabled";

    private static final String AUDIT_REST_PARAMS = "AUDIT_REST_PARAMS";

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CdpOperationDetailsFactory restEventFilterRelatedObjectFactory;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private StructuredEventFilterUtil structuredEventFilterUtil;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CDPRestAuditUrlParser commonUrlParser;

    @Value("${cb.audit.allEndpoints.enabled:false}")
    private boolean auditOnAllEndpointsEnabled;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (auditOnAllEndpointsEnabled) {
            RestFilterPropertyUtil.setPropertyIfAbsent(requestContext, REST_STRICT_AUDIT_ENABLED, Boolean.TRUE);
            RestFilterPropertyUtil.setPropertyIfAbsent(requestContext, REQUEST_TIME, System.currentTimeMillis());
            Map<String, String> restParams = Maps.newHashMap();
            commonUrlParser.fillParams(requestContext, restParams);
            requestContext.setProperty(AUDIT_REST_PARAMS, restParams);
            extractBodyAndSetRequestDetailsIfAbsent(requestContext);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (BooleanUtils.isTrue((Boolean) requestContext.getProperty(REST_STRICT_AUDIT_ENABLED))) {
            RestResponseDetails restResponse = RestFilterPropertyUtil.createResponseDetails(responseContext);
            if (responseContext.hasEntity()) {
                RestFilterPropertyUtil.extractAndSetResponseEntityStreamIfAbsent(requestContext, responseContext, Boolean.TRUE);
                RestFilterPropertyUtil.setPropertyIfAbsent(requestContext, RESPONSE_DETAILS, restResponse);
            } else {
                Long requestTime = (Long) requestContext.getProperty(REQUEST_TIME);
                RestRequestDetails restRequest = (RestRequestDetails) requestContext.getProperty(REQUEST_DETAILS);
                Map<String, String> restParams = (Map<String, String>) requestContext.getProperty(AUDIT_REST_PARAMS);
                structuredEventFilterUtil.sendRestAuditEvent(restRequest, restResponse, restParams, requestTime, "");
            }
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.proceed();
        if (BooleanUtils.isTrue((Boolean) context.getProperty(REST_STRICT_AUDIT_ENABLED))) {
            sendStructuredEvent(context);
        }
    }

    private void extractBodyAndSetRequestDetailsIfAbsent(ContainerRequestContext requestContext) throws IOException {
        if (Objects.isNull(requestContext.getProperty(REQUEST_DETAILS))) {
            StringBuilder body = new StringBuilder();
            requestContext.setEntityStream(RestFilterRequestBodyLogger.logInboundEntity(body, requestContext.getEntityStream(),
                    MessageUtils.getCharset(requestContext.getMediaType()), Boolean.TRUE));
            requestContext.setProperty(REQUEST_DETAILS, RestFilterPropertyUtil.createRequestDetails(requestContext, body.toString()));
        }
    }

    private void sendStructuredEvent(WriterInterceptorContext context) {
        Long requestTime = (Long) context.getProperty(REQUEST_TIME);
        RestRequestDetails restRequest = (RestRequestDetails) context.getProperty(REQUEST_DETAILS);
        RestResponseDetails restResponse = (RestResponseDetails) context.getProperty(RESPONSE_DETAILS);
        String responseBody = ((LoggingStream) context.getProperty(RESPONSE_LOGGING_STREAM)).getStringBuilder(
                MessageUtils.getCharset(context.getMediaType())).toString();
        Map<String, String> restParams = (Map<String, String>) context.getProperty(AUDIT_REST_PARAMS);
        if (restParams == null) {
            restParams = new HashMap<>();
        }
        structuredEventFilterUtil.sendRestAuditEvent(restRequest, restResponse, restParams, requestTime, responseBody);
    }
}
