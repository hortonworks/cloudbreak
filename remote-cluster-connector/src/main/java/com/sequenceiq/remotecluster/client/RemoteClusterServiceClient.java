package com.sequenceiq.remotecluster.client;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.onpremises.OnPremisesApiGrpc;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto.Cluster;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalGrpc;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesRequest;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.ListAllPvcControlPlanesResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.PvcControlPlaneConfiguration;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class RemoteClusterServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteClusterServiceClient.class);

    private static final int LIST_CONTROL_PLANES_PAGE_SIZE = 1000;

    private static final int LIST_CLUSTERS_PAGE_SIZE = 20;

    @Qualifier("remoteClusterManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private RemoteClusterServiceConfig remoteClusterConfig;

    @Inject
    private StubProvider stubProvider;

    public List<PvcControlPlaneConfiguration> listAllPrivateControlPlanes() {
        RemoteClusterInternalGrpc.RemoteClusterInternalBlockingStub internalBlockingStub = createRemoteClusterInternalBlockingStub();
        String nextToken = "";

        List<PvcControlPlaneConfiguration> items = new ArrayList<>();
        do {
            ListAllPvcControlPlanesRequest listAllPvcControlPlanesRequest = ListAllPvcControlPlanesRequest.newBuilder()
                    .setPageSize(LIST_CONTROL_PLANES_PAGE_SIZE)
                    .setPageToken(nextToken)
                    .build();

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

    public String registerPrivateEnvironmentBaseCluster(RegisterPvcBaseClusterRequest registerPvcBaseClusterRequest) {
        RemoteClusterInternalGrpc.RemoteClusterInternalBlockingStub internalBlockingStub = createRemoteClusterInternalBlockingStub();

        RegisterPvcBaseClusterResponse registerPvcBaseClusterResponse = internalBlockingStub.registerPvcBaseCluster(registerPvcBaseClusterRequest);
        return registerPvcBaseClusterResponse.getClusterCrn();
    }

    public List<Cluster> listClassicClusters(String userCrn) {
        OnPremisesApiGrpc.OnPremisesApiBlockingStub blockingStub = createOnPremisesApiBlockingStub(userCrn);
        List<Cluster> clusters = new ArrayList<>();
        String nextPageToken = "";

        do {
            OnPremisesApiProto.ListClustersRequest request = OnPremisesApiProto.ListClustersRequest.newBuilder()
                    .setPageSize(LIST_CLUSTERS_PAGE_SIZE)
                    .setPageToken(nextPageToken)
                    .build();
            LOGGER.info("Created request to list clusters: {}, with actor('{}')", request, userCrn);
            OnPremisesApiProto.ListClustersResponse response = blockingStub.listClusters(request);
            clusters.addAll(response.getClustersList());
            nextPageToken = response.getNextPageToken();
        } while (StringUtils.isNotEmpty(nextPageToken));

        return clusters;
    }

    public Cluster describeClassicCluster(String userCrn, String clusterCrn) {
        return describeClassicCluster(userCrn, clusterCrn, false);
    }

    public Cluster describeClassicCluster(String userCrn, String clusterCrn, boolean withDetails) {
        OnPremisesApiProto.DescribeClusterRequest request = OnPremisesApiProto.DescribeClusterRequest.newBuilder()
                .setClusterCrn(clusterCrn)
                .setShowOnPremiseEnvironmentDetails(withDetails)
                .build();
        LOGGER.info("Created request to describe cluster {}: {}", clusterCrn, request);
        return createOnPremisesApiBlockingStub(userCrn).describeCluster(request).getCluster();
    }

    private RemoteClusterInternalGrpc.RemoteClusterInternalBlockingStub createRemoteClusterInternalBlockingStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return stubProvider.newRemoteClusterInternalStub(channelWrapper.getChannel(), requestId,
                remoteClusterConfig.getGrpcTimeoutSec(), remoteClusterConfig.internalCrnForIamServiceAsString(), remoteClusterConfig.getCallingServiceName());
    }

    private OnPremisesApiGrpc.OnPremisesApiBlockingStub createOnPremisesApiBlockingStub(String userCrn) {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return stubProvider.newOnPremisesStub(channelWrapper.getChannel(), requestId, remoteClusterConfig.getGrpcTimeoutSec(), userCrn);
    }
}
