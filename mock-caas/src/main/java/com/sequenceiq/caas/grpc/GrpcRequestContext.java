package com.sequenceiq.caas.grpc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import io.grpc.Context;
import io.grpc.Context.Key;

/**
 * This object captures the request context available within a GRPC-based
 * backend service. This object is accessible to code running in a GRPC-based
 * service from the Context via the member key.
 */
public class GrpcRequestContext {

    public static final Key<GrpcRequestContext> REQUEST_CONTEXT = Context.key("requestContext");

    private final String requestId;

    @JsonCreator
    public GrpcRequestContext(@JsonProperty("requestId") String requestId) {
        this.requestId = checkNotNull(requestId);
    }

    @JsonProperty("requestId")
    public String getRequestId() {
        return requestId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(requestId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GrpcRequestContext other = (GrpcRequestContext) obj;
        return Objects.equal(requestId, other.requestId);
    }
}
