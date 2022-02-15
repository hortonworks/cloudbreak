package com.sequenceiq.cloudbreak.grpc.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.tracing.TracingUtil;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.OperationNameConstructor;
import io.opentracing.contrib.grpc.TracingClientInterceptor;

/**
 * Provides GRPC-related utilities.
 */
public class GrpcUtil {

    /**
     * Those statuse codes that indicate a transient problem.
     */
    private static final Set<Status.Code> RETRYABLE_STATUS_CODES =
            Sets.immutableEnumSet(Status.Code.UNAVAILABLE, Status.Code.NOT_FOUND);

    /**
     * Private constructor to prevent instantiation.
     */
    private GrpcUtil() {
    }

    /**
     * Returns whether the specified status code indicates a transient problem.
     *
     * @param statusCode the status code
     * @return whether the specified status code indicates a transient problem
     */
    public static boolean isRetryable(Status.Code statusCode) {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }

    public static TracingClientInterceptor getTracingInterceptor(Tracer tracer) {
        return TracingClientInterceptor.newBuilder()
                .withTracer(tracer)
                .withOperationName(new OperationNameConstructor() {
                    @Override
                    // CHECKSTYLE:OFF
                    public <ReqT, RespT> String constructOperationName(MethodDescriptor<ReqT, RespT> method) {
                        return "gRPC - " + method.getFullMethodName();
                    }
                    // CHECKSTYLE:ON
                })
                .withTracedAttributes(TracingClientInterceptor.ClientRequestAttribute.HEADERS)
                .withClientSpanDecorator((span, method, callOptions) -> {
                    TracingUtil.setTagsFromMdc(span);
                })
                .build();
    }

    public static ClientInterceptor getTimeoutInterceptor(long timeoutSec) {
        return new ClientInterceptor() {
            @Override
            public <R, T> ClientCall<R, T> interceptCall(MethodDescriptor<R, T> method, CallOptions callOptions, Channel next) {
                callOptions = callOptions.withDeadlineAfter(timeoutSec, TimeUnit.SECONDS);
                return next.newCall(method, callOptions);
            }
        };
    }
}
