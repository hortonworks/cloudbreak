package com.sequenceiq.cloudbreak.wiam.client;

import static com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest;
import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudera.thunderhead.service.workloadiam.WorkloadIamGrpc;
import com.cloudera.thunderhead.service.workloadiam.WorkloadIamGrpc.WorkloadIamBlockingStub;
import com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

public class WiamClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    private final Tracer tracer;

    public WiamClient(ManagedChannel channel, String actorCrn, Tracer tracer) {
        this.channel = channel;
        this.actorCrn = actorCrn;
        this.tracer = tracer;
    }

    public SyncUsersResponse syncUsersInEnvironment(String accountId, String environmentCrn, String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        SyncUsersRequest syncUsersRequest = SyncUsersRequest.newBuilder()
                .setAccountId(accountId)
                .setEnvironmentCrn(environmentCrn)
                .build();
        return newStub(requestId).syncUsers(syncUsersRequest);
    }

    private WorkloadIamBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return WorkloadIamGrpc.newBlockingStub(channel)
                .withInterceptors(GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
