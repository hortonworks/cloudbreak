package com.sequenceiq.cloudbreak.grpc.altus;

import static org.glassfish.jersey.internal.guava.Preconditions.checkArgument;
import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;

public class CallingServiceNameInterceptor implements ClientInterceptor {

    private static final Key<String> CALLING_SERVICE_NAME_METADATA_KEY =
            Key.of("callingServiceName", Metadata.ASCII_STRING_MARSHALLER);

    private final String callingServiceName;

    public CallingServiceNameInterceptor(String callingServiceName) {
        this.callingServiceName = checkNotNull(callingServiceName, "callingServiceName should not be null.");
        checkArgument(!callingServiceName.isBlank(), "callingServiceName should not be blank.");
    }

    @Override
    public <R, S> ClientCall<R, S> interceptCall(MethodDescriptor<R, S> method, CallOptions callOptions, Channel next) {
        return new SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(
                    Listener<S> responseListener,
                    Metadata headers) {
                Metadata metadata = new Metadata();
                metadata.put(CALLING_SERVICE_NAME_METADATA_KEY, callingServiceName);
                headers.merge(metadata);
                super.start(responseListener, headers);
            }
        };
    }
}
