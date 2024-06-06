package com.sequenceiq.liftie.client;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;

public class EnvCrnMetadataInterceptor extends AltusMetadataInterceptor {

    @VisibleForTesting
    static final Key<String> ENV_CRN_METADATA_KEY =
            Key.of("x-cdp-env-crn", Metadata.ASCII_STRING_MARSHALLER);

    private final String envCrn;

    /**
     * Constructor.
     *
     * @param requestId the request ID
     * @param actorCrn  the actor CRN
     * @param envCrn  the env CRN
     */
    public EnvCrnMetadataInterceptor(String requestId, String actorCrn, String envCrn) {
        super(requestId, actorCrn);
        this.envCrn = checkNotNull(envCrn, "envCrn should not be null.");
    }

    @Override
    public <R, S> ClientCall<R, S> interceptCall(
            MethodDescriptor<R, S> method,
            CallOptions callOptions,
            Channel next) {
        return new SimpleForwardingClientCall<R, S>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(
                    Listener<S> responseListener,
                    Metadata headers) {
                Metadata metadata = EnvCrnMetadataInterceptor.super.getMetadata();
                metadata.put(ENV_CRN_METADATA_KEY, envCrn);
                headers.merge(metadata);
                super.start(responseListener, headers);
            }
        };
    }

}
