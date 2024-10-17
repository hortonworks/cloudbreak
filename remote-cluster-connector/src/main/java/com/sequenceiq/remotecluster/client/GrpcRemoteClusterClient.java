package com.sequenceiq.remotecluster.client;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.PvcControlPlaneConfiguration;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;

@Component
public class GrpcRemoteClusterClient {

    @Qualifier("remoteClusterManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private RemoteClusterServiceConfig remoteClusterConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private StubProvider stubProvider;

    public List<PvcControlPlaneConfiguration> listAllPrivateControlPlanes() {
        RemoteClusterServiceClient client = makeClient(
                channelWrapper.getChannel(),
                remoteClusterConfig.internalCrnForIamServiceAsString());
        return client.listAllPrivateControlPlanes();
    }

    public String registerPrivateEnvironmentBaseCluster(RegisterPvcBaseClusterRequest registerPvcBaseClusterRequest) {
        RemoteClusterServiceClient client = makeClient(
                channelWrapper.getChannel(),
                remoteClusterConfig.internalCrnForIamServiceAsString());
        return client.registerPrivateEnvironmentBaseClusters(registerPvcBaseClusterRequest);
    }

    private RemoteClusterServiceClient makeClient(ManagedChannel channel, String actorCrn) {
        return new RemoteClusterServiceClient(
                channel,
                actorCrn,
                remoteClusterConfig,
                stubProvider);
    }
}
