package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.tracing.TracingUtil;

import io.opentracing.Span;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;

public class TracingClientSpanDecorator implements ClientSpanDecorator {

    private static final Map<String, SaltEndpoint> SALT_ENDPOINTS = new HashMap<>();

    private static final Pattern PATTERN = Pattern.compile(".*\\/(saltapi.*|saltboot.*)");

    private static final int GROUP = 1;

    static {
        Stream.of(SaltEndpoint.values())
                .forEach(saltEndpoint -> SALT_ENDPOINTS
                        .put(saltEndpoint.getContextPath(), saltEndpoint));
    }

    @Override
    public void decorateRequest(ClientRequestContext requestContext, Span span) {
        setOperationName(requestContext, span);
        setTags(requestContext, span);
    }

    private void setOperationName(ClientRequestContext requestContext, Span span) {
        String uri = requestContext.getUri().toString();
        Matcher matcher = PATTERN.matcher(uri);
        StringBuilder operationName = new StringBuilder("Salt - ");
        if (matcher.find()) {
            String saltPath = matcher.group(GROUP);
            SaltEndpoint saltEndpoint = SALT_ENDPOINTS.get(saltPath);
            operationName.append(saltEndpoint != null ? saltEndpoint.name() : saltPath);
        } else {
            operationName.append("UNKNOWN_OPERATION");
        }
        span.setOperationName(operationName.toString());
    }

    private void setTags(ClientRequestContext requestContext, Span span) {
        TracingUtil.setTagsFromMdc(span);
        Object entity = requestContext.getEntity();
        if (entity instanceof Pillar) {
            String resourcePath = ((Pillar) entity).getPath();
            span.setTag("SALT_PATH", resourcePath);
        }
    }

    @Override
    public void decorateResponse(ClientResponseContext responseContext, Span span) {
    }
}
