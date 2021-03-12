package com.sequenceiq.cloudbreak.cloud.gcp.tracing;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.tracing.TracingUtil;
import com.sequenceiq.cloudbreak.util.Benchmark;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

@Component
public class GcpTracingInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpTracingInterceptor.class);

    private static final String JAVA_GCP_SDK = "java-gcp-sdk";

    private static final int MIN_OK_STATUS_CODE = 200;

    private static final int MAX_OK_STATUS_CODE = 300;

    private static final ThreadLocal<Pair<Scope, Span>> DATA = new ThreadLocal<>();

    @Inject
    private Tracer tracer;

    @Override
    public void process(HttpRequest request, HttpContext context) {
        StackWalker stackWalker = StackWalker.getInstance();
        StringBuilder apiOperation = new StringBuilder();
        Benchmark.measure(() -> getApiOperationFromStackTrace(stackWalker, apiOperation),
                LOGGER, "STACK WALKING TOOK: {}ms");
        activateNewTracingSpan(request, context, apiOperation);
    }

    @Override
    public void process(HttpResponse response, HttpContext context) {
        Scope scope = DATA.get().getLeft();
        Span span = DATA.get().getRight();
        StatusLine statusLine = response.getStatusLine();
        if (requestSucceeded(statusLine)) {
            span.setTag(TracingUtil.ERROR, false);
        } else {
            span.setTag(TracingUtil.ERROR, true);
            span.log(Map.of(TracingUtil.RESPONSE_CODE, statusLine.getStatusCode(), TracingUtil.MESSAGE, statusLine.getReasonPhrase()));
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
                && !stackFrame.getClassName().contains("tracing") && apiOperation.length() == 0;
    }

    private String extractClassName(StackWalker.StackFrame stackFrame) {
        return StringUtils.substringBefore(
                StringUtils.substringAfterLast(stackFrame.getClassName(), "."),
                "$");
    }

    private void activateNewTracingSpan(HttpRequest request, HttpContext context, StringBuilder apiOperation) {
        RequestLine requestLine = request.getRequestLine();
        String operationName = createOperationName(context, apiOperation, requestLine);
        Span span = initSpan(requestLine, operationName);
        Scope scope = tracer.activateSpan(span);
        DATA.set(Pair.of(scope, span));
    }

    private String createOperationName(HttpContext context, StringBuilder apiOperation, RequestLine requestLine) {
        String hostName = ((HttpHost) context.getAttribute("http.target_host")).getHostName();
        return "GCP - [" + requestLine.getMethod().toUpperCase() + "] " +
                apiOperation +
                " (" + hostName + ')';
    }

    private Span initSpan(RequestLine requestLine, String operationName) {
        Span span = tracer.buildSpan(operationName)
                .addReference(References.FOLLOWS_FROM, tracer.activeSpan() != null ? tracer.activeSpan().context() : null)
                .start();
        span.setTag(TracingUtil.COMPONENT, JAVA_GCP_SDK);
        span.setTag(TracingUtil.URL, requestLine.getUri());
        span.setTag(TracingUtil.HTTP_METHOD, requestLine.getMethod());
        return span;
    }

    private boolean requestSucceeded(StatusLine statusLine) {
        return statusLine.getStatusCode() >= MIN_OK_STATUS_CODE && statusLine.getStatusCode() < MAX_OK_STATUS_CODE;
    }
}
