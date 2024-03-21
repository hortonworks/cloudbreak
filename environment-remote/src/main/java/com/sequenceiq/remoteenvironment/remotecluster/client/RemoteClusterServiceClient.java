package com.sequenceiq.remoteenvironment.remotecluster.client;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterGrpc;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterGrpc.RemoteClusterBlockingStub;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneRequest;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesRequest;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;

public class RemoteClusterServiceClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    private final RemoteClusterServiceConfig remoteClusterConfig;

    public RemoteClusterServiceClient(ManagedChannel channel, String actorCrn, RemoteClusterServiceConfig remoteClusterConfig) {
        this.channel = channel;
        this.actorCrn = actorCrn;
        this.remoteClusterConfig = remoteClusterConfig;
    }

    public ListPvcControlPlanesResponse listRemoteClusters() {
        String requestId = MDCBuilder.getOrGenerateRequestId();

        return newStub(requestId)
                .listPvcControlPlanes(ListPvcControlPlanesRequest.newBuilder().build());
    }

    public DescribePvcControlPlaneResponse describeRemoteClusters(String pvc) {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        checkNotNull(pvc, "pvc should not be null.");

        DescribePvcControlPlaneRequest.Builder builder = DescribePvcControlPlaneRequest.newBuilder();
        builder.setPvc(pvc);
        return newStub(requestId)
                .describePvcControlPlane(builder.build());
    }

    private RemoteClusterBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");

        return RemoteClusterGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(remoteClusterConfig.getGrpcTimeoutSec()),
                        new AltusMetadataInterceptor(requestId, remoteClusterConfig.internalCrnForServiceAsString()),
                        new CallingServiceNameInterceptor(remoteClusterConfig.getCallingServiceName()));
    }
}
