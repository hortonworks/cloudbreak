package com.sequenceiq.cloudbreak.wiam.client;

import static com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersRequest;
import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudera.thunderhead.service.workloadiam.WorkloadIamGrpc;
import com.cloudera.thunderhead.service.workloadiam.WorkloadIamGrpc.WorkloadIamBlockingStub;
import com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;

public class WiamClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    public WiamClient(ManagedChannel channel, String actorCrn) {
        this.channel = channel;
        this.actorCrn = actorCrn;
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
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
