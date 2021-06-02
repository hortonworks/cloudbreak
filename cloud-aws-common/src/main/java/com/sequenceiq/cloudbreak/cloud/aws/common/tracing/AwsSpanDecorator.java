package com.sequenceiq.cloudbreak.cloud.aws.common.tracing;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.Request;
import com.amazonaws.Response;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

class AwsSpanDecorator {

    static final String COMPONENT_NAME = "java-aws-sdk";

    private AwsSpanDecorator() {
    }

    static void onRequest(Request request, Span span) {
        Tags.COMPONENT.set(span, COMPONENT_NAME);
        Tags.HTTP_METHOD.set(span, request.getHttpMethod().name());
        Tags.HTTP_URL.set(span, request.getEndpoint().toString());
        Tags.PEER_SERVICE.set(span, request.getServiceName());
    }

    static void onResponse(Response response, Span span) {
        Tags.HTTP_STATUS.set(span, response.getHttpResponse().getStatusCode());
    }

    static void onError(Throwable throwable, Span span) {
        Tags.ERROR.set(span, Boolean.TRUE);
        span.log(errorLogs(throwable));
    }

    private static Map<String, Object> errorLogs(Throwable throwable) {
        Map<String, Object> errorLogs = new HashMap<>();
        errorLogs.put("event", Tags.ERROR.getKey());
        errorLogs.put("error.kind", throwable.getClass().getName());
        errorLogs.put("error.object", throwable);

        errorLogs.put("message", throwable.getMessage());

        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        errorLogs.put("stack", sw.toString());

        return errorLogs;
    }
}
