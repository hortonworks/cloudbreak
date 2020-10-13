package com.sequenceiq.cloudbreak.cm.client.tracing;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.tracing.TracingUtil;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

@Component
public class CmOkHttpTracingInterceptor implements Interceptor {

    private static final String CLOUDERA_MANAGER = "Cloudera Manager";

    @Inject
    private Tracer tracer;

    @Inject
    private StackBasedCmApiNameExtractor stackBasedCmApiNameExtractor;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Span span = createSpan(chain);
        try (Scope ignored = tracer.activateSpan(span)) {
            Response response = chain.proceed(chain.request());
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

    private Span createSpan(Chain chain) {
        Request request = chain.request();
        StackWalker stackWalker = StackWalker.getInstance(Set.of(StackWalker.Option.SHOW_HIDDEN_FRAMES));
        Optional<String> cmApiNameOptional = stackBasedCmApiNameExtractor.getCmApiName(stackWalker);
        String cmApiName = cmApiNameOptional.orElse(request.url().getPath());
        Span span = tracer.buildSpan("CM - [" + cmApiName + "] ")
                .addReference(References.FOLLOWS_FROM, tracer.activeSpan() != null ? tracer.activeSpan().context() : null)
                .start();
        span.setTag(TracingUtil.COMPONENT, CLOUDERA_MANAGER);
        span.setTag(TracingUtil.URL, request.url().toString());
        span.setTag(TracingUtil.HTTP_METHOD, request.method());
        span.setTag(TracingUtil.HEADERS, Json.silent(chain.request().headers().toMultimap()).getValue());
        TracingUtil.setTagsFromMdc(span);
        return span;
    }
}
