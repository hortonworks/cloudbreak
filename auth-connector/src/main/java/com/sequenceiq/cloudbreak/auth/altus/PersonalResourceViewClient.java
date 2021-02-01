package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewGrpc;
import com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class PersonalResourceViewClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    private final Tracer tracer;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     * @param tracer   tracer
     */
    PersonalResourceViewClient(ManagedChannel channel, String actorCrn, Tracer tracer) {
        this.channel = checkNotNull(channel);
        this.actorCrn = checkNotNull(actorCrn);
        this.tracer = tracer;
    }

    public List<Boolean> hasRightOnResources(String requestId, String actorCrn, String right, Iterable<String> resources) {
        checkNotNull(requestId);
        checkNotNull(actorCrn);
        checkNotNull(resources);
        return newStub(requestId)
                .hasResourcesByRight(
                        PersonalResourceViewProto.HasResourcesByRightRequest
                                .newBuilder()
                                .setUserCrn(actorCrn)
                                .setRight(right)
                                .addAllResource(resources)
                                .build())
                .getResultList();
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private PersonalResourceViewGrpc.PersonalResourceViewBlockingStub newStub(String requestId) {
        checkNotNull(requestId);
        return PersonalResourceViewGrpc.newBlockingStub(channel).withInterceptors(
                GrpcUtil.getTracingInterceptor(tracer),
                new AltusMetadataInterceptor(requestId, actorCrn)
        );
    }
}
