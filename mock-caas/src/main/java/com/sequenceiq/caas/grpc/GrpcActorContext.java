package com.sequenceiq.caas.grpc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import io.grpc.Context;
import io.grpc.Context.Key;

/**
 * A context for grpc backend services. It contains information about the actor
 * is making the grpc call. Note that the actor who is making the call can be in
 * a different account than the resource the grpc call is operating on. The
 * context is used to make authorization decisions in the backend.
 * <p>
 * An actor is identified by its CRN. The CRN holds enough information to
 * uniquely identify the actor and the type of actor.
 */
public class GrpcActorContext {

    /**
     * The key to look up this object in the Context.
     */
    public static final Key<GrpcActorContext> ACTOR_CONTEXT = Context.key("actorContext");

    private final String actorCrn;

    @JsonCreator
    public GrpcActorContext(
            @JsonProperty("actorCrn") String actorCrn) {
        this.actorCrn = checkNotNull(actorCrn);
    }

    @JsonProperty("actorCrn")
    public String getActorCrn() {
        return actorCrn;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(actorCrn);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GrpcActorContext other = (GrpcActorContext) obj;
        return Objects.equal(actorCrn, other.actorCrn);
    }
}
