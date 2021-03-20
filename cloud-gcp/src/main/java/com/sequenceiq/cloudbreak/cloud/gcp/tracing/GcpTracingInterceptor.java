package com.sequenceiq.cloudbreak.cloud.gcp.tracing;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseInterceptor;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpServiceFactory;
import com.sequenceiq.cloudbreak.tracing.TracingUtil;
import com.sequenceiq.cloudbreak.util.Benchmark;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

@Component
public class GcpTracingInterceptor implements HttpExecuteInterceptor, HttpResponseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpTracingInterceptor.class);

    private static final String JAVA_GCP_SDK = "java-gcp-sdk";

    private static final int MIN_OK_STATUS_CODE = 200;

    private static final int MAX_OK_STATUS_CODE = 300;

    private static final ThreadLocal<Pair<Scope, Span>> DATA = new ThreadLocal<>();

    @Inject
    private Tracer tracer;

    @Override
    public void intercept(HttpRequest request)  throws IOException {
        StackWalker stackWalker = StackWalker.getInstance();
        StringBuilder apiOperation = new StringBuilder();
        Benchmark.measure(() -> getApiOperationFromStackTrace(stackWalker, apiOperation),
                LOGGER, "STACK WALKING TOOK: {}ms");
        activateNewTracingSpan(request, apiOperation);
    }

    @Override
    public void interceptResponse(HttpResponse response) throws IOException {
        Scope scope = DATA.get().getLeft();
        Span span = DATA.get().getRight();
        int statusCode = response.getStatusCode();
        if (requestSucceeded(response.getStatusCode())) {
            span.setTag(TracingUtil.ERROR, false);
        } else {
            span.setTag(TracingUtil.ERROR, true);
            span.log(Map.of(TracingUtil.RESPONSE_CODE, statusCode, TracingUtil.MESSAGE, response.getStatusMessage()));
        }
        span.finish();
        scope.close();
    }

    private void getApiOperationFromStackTrace(StackWalker stackWalker, StringBuilder apiOperation) {
        stackWalker.forEach(stackFrame -> {
            try {
                if (isFirstGcpApiMethod(apiOperation, stackFrame)) {
                    String className = extractClassName(stackFrame);
                    apiOperation.append(className).append(" # ").append(stackFrame.getMethodName());
                }
            } catch (Exception e) {
                LOGGER.warn("Exception occurred during stack walking.", e);
            }
        });
    }

    private boolean isFirstGcpApiMethod(StringBuilder apiOperation, StackWalker.StackFrame stackFrame) {
        return stackFrame.getClassName().startsWith("com.sequenceiq.cloudbreak.cloud.gcp")
                && !stackFrame.getClassName().equals(GcpTracingInterceptor.class.getName())
                && !stackFrame.getClassName().equals(GcpServiceFactory.class.getName())
                && apiOperation.length() == 0;
    }

    private String extractClassName(StackWalker.StackFrame stackFrame) {
        return StringUtils.substringBefore(
                StringUtils.substringAfterLast(stackFrame.getClassName(), "."),
                "$");
    }

    private void activateNewTracingSpan(HttpRequest request, StringBuilder apiOperation) {
        String operationName = createOperationName(apiOperation, request);
        Span span = initSpan(request, operationName);
        Scope scope = tracer.activateSpan(span);
        DATA.set(Pair.of(scope, span));
    }

    private String createOperationName(StringBuilder apiOperation, HttpRequest request) {
        String hostName = request.getUrl().getHost();
        return "GCP - [" + request.getRequestMethod().toUpperCase() + "] " +
                apiOperation +
                " (" + hostName + ')';
    }

    private Span initSpan(HttpRequest request, String operationName) {
        Span span = tracer.buildSpan(operationName)
                .addReference(References.FOLLOWS_FROM, tracer.activeSpan() != null ? tracer.activeSpan().context() : null)
                .start();
        span.setTag(TracingUtil.COMPONENT, JAVA_GCP_SDK);
        span.setTag(TracingUtil.URL, request.getUrl().getHost());
        span.setTag(TracingUtil.HTTP_METHOD, request.getRequestMethod().toUpperCase());
        return span;
    }

    private boolean requestSucceeded(int statusCode) {
        return statusCode >= MIN_OK_STATUS_CODE && statusCode < MAX_OK_STATUS_CODE;
    }
}