package com.sequenceiq.remoteenvironment.remotecluster.client;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.DescribePvcControlPlaneResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.ListPvcControlPlanesResponse;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;

@Component
public class GrpcRemoteClusterClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRemoteClusterClient.class);

    @Qualifier("remoteClusterManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private RemoteClusterServiceConfig remoteClusterConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public ListPvcControlPlanesResponse listPrivateControlPlanes() {
        RemoteClusterServiceClient client = makeClient(
                channelWrapper.getChannel(),
                remoteClusterConfig.internalCrnForServiceAsString());
        return client.listRemoteClusters();
    }

    public DescribePvcControlPlaneResponse describePrivateControlPlanes(String pvc) {
        RemoteClusterServiceClient client = makeClient(
                channelWrapper.getChannel(),
                remoteClusterConfig.internalCrnForServiceAsString());
        return client.describeRemoteClusters(pvc);
    }

    private RemoteClusterServiceClient makeClient(ManagedChannel channel, String accountId) {
        return new RemoteClusterServiceClient(
                channel,
                accountId,
                remoteClusterConfig);
    }
}
