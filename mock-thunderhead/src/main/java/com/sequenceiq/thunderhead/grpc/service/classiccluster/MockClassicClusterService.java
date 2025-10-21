package com.sequenceiq.thunderhead.grpc.service.classiccluster;

import static com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider.API_V_51;
import static com.sequenceiq.thunderhead.service.ClassicClusterService.CLUSTER_PROXY_CONFIG_SERVICE_NAME;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiCluster;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiGrpc;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.thunderhead.entity.ClassicCluster;
import com.sequenceiq.thunderhead.grpc.GrpcActorContext;
import com.sequenceiq.thunderhead.service.ClassicClusterService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Component
public class MockClassicClusterService extends OnPremisesApiGrpc.OnPremisesApiImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockClassicClusterService.class);

    private static final Map<String, OnPremisesApiProto.Cluster> CLUSTER_CACHE = new HashMap<>();

    @Inject
    private ClassicClusterService classicClusterService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Override
    public void listClusters(OnPremisesApiProto.ListClustersRequest request, StreamObserver<OnPremisesApiProto.ListClustersResponse> responseObserver) {
        String accountId = Crn.fromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn()).getAccountId();
        List<OnPremisesApiProto.Cluster> clusters = classicClusterService.findAllByAccountId(accountId).stream()
                .map(this::convertWithCache)
                .toList();
        OnPremisesApiProto.ListClustersResponse response = OnPremisesApiProto.ListClustersResponse.newBuilder()
                .addAllClusters(clusters)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void describeCluster(OnPremisesApiProto.DescribeClusterRequest request, StreamObserver<OnPremisesApiProto.DescribeClusterResponse> responseObserver) {
        classicClusterService.findByCrn(request.getClusterCrn()).ifPresentOrElse(classicCluster -> {
            OnPremisesApiProto.DescribeClusterResponse response = OnPremisesApiProto.DescribeClusterResponse.newBuilder()
                    .setCluster(convertWithCache(classicCluster))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, () -> responseObserver.onError(Status.NOT_FOUND.withDescription("Classic Cluster not found with crn " + request.getClusterCrn()).asException()));
    }

    private OnPremisesApiProto.Cluster convertWithCache(ClassicCluster classicCluster) {
        return CLUSTER_CACHE.computeIfAbsent(classicCluster.getCrn(), crn -> convert(classicCluster));
    }

    private OnPremisesApiProto.Cluster convert(ClassicCluster classicCluster) {
        try {
            ApiClient apiClient =
                    clouderaManagerApiClientProvider.getClouderaManagerClient(getHttpClientConfig(classicCluster), null, null, null, API_V_51);
            ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
            ApiCluster apiCluster = clustersResourceApi.readCluster(classicCluster.getName());

            return OnPremisesApiProto.Cluster.newBuilder()
                    .setName(classicCluster.getName())
                    .setDatacenterName(classicCluster.getDatacenterName())
                    .setClusterCrn(classicCluster.getCrn())
                    .setManagerUri(classicCluster.getUrl())
                    .setData(OnPremisesApiProto.ClusterData.newBuilder()
                            .setVersion(apiCluster.getFullVersion())
                            .setClusterDetails(OnPremisesApiProto.ClusterDetails.newBuilder()
                                    .setEntityStatus(OnPremisesApiProto.EntityStatus.Value.GOOD_HEALTH)
                                    .setClusterUrl(classicCluster.getUrl())))
                    .setLastCreateTime(new Date().getTime())
                    .setPvcCrn(getPvcCrn(classicCluster))
                    .setEnvironmentCrn(Objects.requireNonNullElse(classicCluster.getPvcCpEnvironmentCrn(), ""))
                    .setCmClusterUuid(apiCluster.getUuid())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get additional cluster details from CM", e);
        }
    }

    private HttpClientConfig getHttpClientConfig(ClassicCluster classicCluster) {
        return new HttpClientConfig(classicCluster.getUrl())
                .withClusterProxy(clusterProxyConfiguration.getClusterProxyUrl(), classicCluster.getCrn(), CLUSTER_PROXY_CONFIG_SERVICE_NAME);
    }

    private String getPvcCrn(ClassicCluster classicCluster) {
        if (StringUtils.isEmpty(classicCluster.getPvcCpEnvironmentCrn())) {
            return "";
        }
        String resourceId = Crn.safeFromString(classicCluster.getPvcCpEnvironmentCrn()).getAccountId();
        String accountId = classicCluster.getAccountId();
        return regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.HYBRID, resourceId, accountId);
    }
}
