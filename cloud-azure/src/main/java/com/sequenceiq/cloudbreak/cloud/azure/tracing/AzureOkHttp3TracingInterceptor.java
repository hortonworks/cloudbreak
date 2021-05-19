package com.sequenceiq.cloudbreak.cloud.azure.tracing;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.tracing.TracingUtil;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class AzureOkHttp3TracingInterceptor implements Interceptor {

    private static final String X_MS_LOGGING_CONTEXT = "x-ms-logging-context";

    private static final String JAVA_AZURE_SDK = "java-azure-sdk";

    @Inject
    private Tracer tracer;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String loggingContext = request.header(X_MS_LOGGING_CONTEXT).split(" ")[0];
        Span span = tracer.buildSpan("Azure - [" + request.method() + "] " + loggingContext)
                .addReference(References.FOLLOWS_FROM, tracer.activeSpan() != null ? tracer.activeSpan().context() : null)
                .start();
        span.setTag(TracingUtil.COMPONENT, JAVA_AZURE_SDK);
        span.setTag(TracingUtil.HTTP_METHOD, request.method());
        TracingUtil.setTagsFromMdc(span);
        try (Scope ignored = tracer.activateSpan(span)) {
            Response response = chain.proceed(request);
            if (response.isSuccessful()) {
                span.setTag(TracingUtil.ERROR, false);
            } else {
                span.setTag(TracingUtil.ERROR, true);
                span.log(Map.of(TracingUtil.RESPONSE_CODE, response.code(), TracingUtil.MESSAGE, response.message()));
            }
            return response;
        } finally {
            span.finish();
        }
    }
}
