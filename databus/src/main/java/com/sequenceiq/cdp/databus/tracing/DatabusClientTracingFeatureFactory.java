package com.sequenceiq.cdp.databus.tracing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cdp.databus.model.PutRecordRequest;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.tracing.TracingUtil;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;

@Component
public class DatabusClientTracingFeatureFactory {

    private static final String OPERATION_NAME = "DataBusApi";

    private static final String ALTUS_REQUEST_ID_HEADER = "x-altus-request-id";

    private static final String ALTUS_AUTH_HEADER = "x-altus-auth";

    private final Tracer tracer;

    public DatabusClientTracingFeatureFactory(Tracer tracer) {
        this.tracer = tracer;
    }

    public ClientTracingFeature createClientTracingFeature() {
        ClientTracingFeature.Builder clientTracingFeatureBuilder = new ClientTracingFeature.Builder(tracer);
        clientTracingFeatureBuilder.withTraceSerialization(false);
        clientTracingFeatureBuilder.withDecorators(List.of(new DatabusSpanDecorator()));
        return clientTracingFeatureBuilder.build();
    }

    static class DatabusSpanDecorator implements ServerSpanDecorator, ClientSpanDecorator {

        @Override
        public void decorateRequest(ContainerRequestContext requestContext, Span span) {
            doDecorateRequest(requestContext, span);
        }

        @Override
        public void decorateResponse(ContainerResponseContext responseContext, Span span) {
            doDecorateResponse(span, responseContext.getStatusInfo(), getRequestId(new HashMap<>(responseContext.getHeaders())));
        }

        @Override
        public void decorateRequest(ClientRequestContext requestContext, Span span) {
            doDecorateRequest(requestContext, span);
        }

        @Override
        public void decorateResponse(ClientResponseContext responseContext, Span span) {
            responseContext.getHeaders().get("x-altus-request-id");
            doDecorateResponse(span, responseContext.getStatusInfo(), getRequestId(new HashMap<>(responseContext.getHeaders())));
        }

        private String getRequestId(Map<String, Object> headers) {
            String result = null;
            if (headers.containsKey(ALTUS_REQUEST_ID_HEADER)) {
                Object requestIdObj = headers.get(ALTUS_REQUEST_ID_HEADER);
                result = requestIdObj != null ? requestIdObj.toString() : null;
            }
            return result;
        }

        private void doDecorateRequest(ContainerRequestContext requestContext, Span span) {
            fillCommonTags(requestContext.getMethod(), new HashMap<>(requestContext.getHeaders()), span);
            span.setOperationName(String.format("%s - PutRecord", OPERATION_NAME));
        }

        private void doDecorateRequest(ClientRequestContext requestContext, Span span) {
            fillCommonTags(requestContext.getMethod(), new HashMap<>(requestContext.getHeaders()), span);
            String streamName = "";
            if (PutRecordRequest.class.equals(requestContext.getEntityClass())) {
                PutRecordRequest recordRequest = (PutRecordRequest) requestContext.getEntity();
                if (recordRequest.getRecord() != null && StringUtils.isNotBlank(recordRequest.getRecord().getStreamName())) {
                    streamName = recordRequest.getRecord().getStreamName();
                }
            }
            String operationName =  StringUtils.isBlank(streamName) ? String.format("%s - PutRecord", OPERATION_NAME)
                    : String.format("%s - %s - PutRecord", OPERATION_NAME, streamName);
            span.setOperationName(operationName);
            span.setTag(TracingUtil.URL, requestContext.getUri().toString());
        }

        private void fillCommonTags(String method, Map<String, Object> headers, Span span) {
            MDCBuilder.addTraceId(span.context().toTraceId());
            MDCBuilder.addSpanId(span.context().toSpanId());
            span.setTag("worker", Thread.currentThread().getName());
            span.setTag(TracingUtil.HTTP_METHOD, method);
            headers.keySet().remove(ALTUS_AUTH_HEADER);
            span.setTag(TracingUtil.HEADERS, Json.silent(headers).getValue());
        }

        private void doDecorateResponse(Span span, Response.StatusType statusInfo2, String requestId) {
            MDCBuilder.addTraceId(span.context().toTraceId());
            MDCBuilder.addSpanId(span.context().toSpanId());
            TracingUtil.setTagsFromMdc(span);
            if (StringUtils.isNotBlank(requestId)) {
                span.setTag(LoggerContextKey.REQUEST_ID.name(), requestId);
            }
            Response.StatusType statusInfo = statusInfo2;
            if (statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL) {
                span.setTag(TracingUtil.ERROR, true);
                span.log(Map.of(TracingUtil.RESPONSE_CODE, statusInfo.getStatusCode(), "reasonPhrase", statusInfo.getReasonPhrase()));
            }
        }
    }
}
