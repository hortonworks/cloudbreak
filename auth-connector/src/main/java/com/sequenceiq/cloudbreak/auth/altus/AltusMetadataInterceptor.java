package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;

/**
 * A GRPC client interceptor that inserts Altus-required metatata into GRPC
 * requests. The Altus-required metadata is:
 * <p>
 * requestId: This is expected to be the UUID uniquely identifying the logical
 * request being processed and is used for request tracing. Altus internally
 * propagates this value across thread and process boundaries, logs it as
 * part of the log format of all messages, and returns it to a customer on
 * any request error as a response header. This is generally set as one of
 * the first steps of processing a public API request or by the root of
 * some background processing not associated directly with a customer-request.
 * <p>
 * actorCrn: This is expected to be the CRN of the actor for which the request
 * is being made. A special internal actor CRN should be used to indicate
 * that a call is not being made by the service itself and not in the
 * context of any specific actor. This will generally result in
 * authorization checks being skipped.
 * <p>
 * Note that this is a super simplistic implementation for demonstration
 * purposes that ties the metadata values to the lifecycle of the interceptor.
 * This works for a pattern of a single request per GRPC stub, but is not that
 * workable for making different logical calls through one stub. In Altus our
 * real interceptor implementation tends to get these values from thread local
 * context objects.
 */
public class AltusMetadataInterceptor implements ClientInterceptor {

    @VisibleForTesting
    static final Key<String> REQUEST_ID_METADATA_KEY =
            Key.of("requestId", Metadata.ASCII_STRING_MARSHALLER);

    @VisibleForTesting
    static final Key<String> ACTOR_CRN_METADATA_KEY =
            Key.of("actorCrn", Metadata.ASCII_STRING_MARSHALLER);

    private final String requestId;

    private final String actorCrn;

    /**
     * Constructor.
     *
     * @param requestId the request ID
     * @param actorCrn  the actor CRN
     */
    AltusMetadataInterceptor(String requestId, String actorCrn) {
        this.requestId = checkNotNull(requestId);
        this.actorCrn = checkNotNull(actorCrn);
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
                Metadata metadata = new Metadata();
                metadata.put(REQUEST_ID_METADATA_KEY, requestId);
                metadata.put(ACTOR_CRN_METADATA_KEY, actorCrn);
                headers.merge(metadata);
                super.start(responseListener, headers);
            }
        };
    }
}
