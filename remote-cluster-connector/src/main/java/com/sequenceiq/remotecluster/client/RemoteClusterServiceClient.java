package com.sequenceiq.remotecluster.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalGrpc;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.PvcControlPlaneConfiguration;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;

public class RemoteClusterServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteClusterServiceClient.class);

    private static final int PAGE_SIZE = 1000;

    private final ManagedChannel channel;

    private final String actorCrn;

    private final RemoteClusterServiceConfig remoteClusterConfig;

    private final StubProvider stubProvider;

    public RemoteClusterServiceClient(ManagedChannel channel, String actorCrn,
            RemoteClusterServiceConfig remoteClusterConfig, StubProvider stubProvider) {
        this.channel = channel;
        this.actorCrn = actorCrn;
        this.remoteClusterConfig = remoteClusterConfig;
        this.stubProvider = stubProvider;
    }

    public List<PvcControlPlaneConfiguration> listAllPrivateControlPlanes() {
        RemoteClusterInternalGrpc.RemoteClusterInternalBlockingStub internalBlockingStub = createRemoteClusterInternalBlockingStub();
        String nextToken = null;

        List<PvcControlPlaneConfiguration> items = new ArrayList<>();
        do {
            ListAllPvcControlPlanesRequest.Builder listAllPvcControlPlanesRequestBuilder = ListAllPvcControlPlanesRequest.newBuilder().setPageSize(PAGE_SIZE);
            if (nextToken != null) {
                listAllPvcControlPlanesRequestBuilder.setPageToken(nextToken);
            }
            ListAllPvcControlPlanesRequest listAllPvcControlPlanesRequest = listAllPvcControlPlanesRequestBuilder.build();

            ListAllPvcControlPlanesResponse listAllPvcControlPlanesResponse = internalBlockingStub.listAllPvcControlPlanes(listAllPvcControlPlanesRequest);
            if (listAllPvcControlPlanesResponse != null) {
                items.addAll(listAllPvcControlPlanesResponse.getControlPlaneConfigurationsList());
                if (!Strings.isNullOrEmpty(listAllPvcControlPlanesResponse.getNextPageToken())) {
                    nextToken = listAllPvcControlPlanesResponse.getNextPageToken();
                    LOGGER.info("There is multiple page of private control planes. Next token will be {}.", nextToken);
                } else {
                    nextToken = null;
                }
            }
        } while (!Strings.isNullOrEmpty(nextToken));

        return items;
    }

    public String registerPrivateEnvironmentBaseClusters(RegisterPvcBaseClusterRequest registerPvcBaseClusterRequest) {
        RemoteClusterInternalGrpc.RemoteClusterInternalBlockingStub internalBlockingStub = createRemoteClusterInternalBlockingStub();

        RegisterPvcBaseClusterResponse registerPvcBaseClusterResponse = internalBlockingStub.registerPvcBaseCluster(registerPvcBaseClusterRequest);
        return registerPvcBaseClusterResponse.getClusterCrn();
    }

    private RemoteClusterInternalGrpc.RemoteClusterInternalBlockingStub createRemoteClusterInternalBlockingStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return stubProvider.newInternalStub(channel, requestId,
                remoteClusterConfig.getGrpcTimeoutSec(), remoteClusterConfig.internalCrnForIamServiceAsString(), remoteClusterConfig.getCallingServiceName());
    }
}
