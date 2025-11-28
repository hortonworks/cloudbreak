package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;
import com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneRemoteEnvironmentConnector;

@Component
public class ClassicClusterRemoteEnvironmentConnector implements RemoteEnvironmentConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterRemoteEnvironmentConnector.class);

    // also accept UNSET for older registrations
    private static final Set<OnPremisesApiProto.ClouderaManagerClusterType.Value> BASE_CLUSTER_TYPES =
            Set.of(OnPremisesApiProto.ClouderaManagerClusterType.Value.UNSET, OnPremisesApiProto.ClouderaManagerClusterType.Value.BASE_CLUSTER);

    @Inject
    private RemoteClusterServiceClient remoteClusterServiceClient;

    @Inject
    private PrivateControlPlaneRemoteEnvironmentConnector privateControlPlaneRemoteEnvironmentConnector;

    @Inject
    private ClassicClusterListService listService;

    @Inject
    private ClassicClusterDescribeService describeService;

    @Inject
    private ClassicClusterRemoteDataContextProvider remoteDataContextProvider;

    @Inject
    private ClassicClusterDatalakeServicesProvider datalakeServicesProvider;

    @Inject
    private ClassicClusterRootCertificateProvider rootCertificateProvider;

    @Override
    public RemoteEnvironmentConnectorType type() {
        return RemoteEnvironmentConnectorType.CLASSIC_CLUSTER;
    }

    @Override
    public Collection<SimpleRemoteEnvironmentResponse> list(String userCrn) {
        List<OnPremisesApiProto.Cluster> clusters = remoteClusterServiceClient.listClassicClusters(userCrn).stream()
                .filter(this::isBaseCluster)
                .toList();
        return listService.list(clusters);
    }

    @Override
    public DescribeEnvironmentResponse describeV1(String userCrn, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(userCrn, environmentCrn);
        if (StringUtils.isNotEmpty(cluster.getEnvironmentCrn())) {
            try {
                LOGGER.info("Forwarding describeV1 to connected Private Control Plane");
                return privateControlPlaneRemoteEnvironmentConnector.describeV1(userCrn, cluster.getEnvironmentCrn());
            } catch (Exception e) {
                LOGGER.warn("Failed to forward describeV1 to connected Private Control Plane", e);
                throw e;
            }
        }
        return describeService.describe(getCluster(userCrn, environmentCrn, true)).toV1Response();
    }

    @Override
    public DescribeEnvironmentV2Response describeV2(String userCrn, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(userCrn, environmentCrn);
        if (StringUtils.isNotEmpty(cluster.getEnvironmentCrn())) {
            try {
                LOGGER.info("Forwarding describeV2 to connected Private Control Plane");
                return privateControlPlaneRemoteEnvironmentConnector.describeV2(userCrn, cluster.getEnvironmentCrn());
            } catch (Exception e) {
                LOGGER.warn("Failed to forward describeV2 to connected Private Control Plane", e);
                throw e;
            }
        }
        return describeService.describe(getCluster(userCrn, environmentCrn, true));
    }

    @Override
    public DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(String userCrn, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(userCrn, environmentCrn);
        return remoteDataContextProvider.getRemoteDataContext(cluster);
    }

    @Override
    public DescribeDatalakeServicesResponse getDatalakeServices(String userCrn, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(userCrn, environmentCrn);
        return datalakeServicesProvider.getDatalakeServices(cluster);
    }

    @Override
    public GetRootCertificateResponse getRootCertificate(String userCrn, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(userCrn, environmentCrn);
        if (StringUtils.isNotEmpty(cluster.getEnvironmentCrn())) {
            try {
                LOGGER.info("Forwarding getRootCertificate to connected Private Control Plane");
                return privateControlPlaneRemoteEnvironmentConnector.getRootCertificate(userCrn, cluster.getEnvironmentCrn());
            } catch (Exception e) {
                LOGGER.warn("Failed to forward getRootCertificate to connected Private Control Plane", e);
                throw e;
            }
        }
        return rootCertificateProvider.getRootCertificate(cluster);
    }

    private OnPremisesApiProto.Cluster getCluster(String userCrn, String crn) {
        return getCluster(userCrn, crn, false);
    }

    private OnPremisesApiProto.Cluster getCluster(String userCrn, String crn, boolean withDetails) {
        OnPremisesApiProto.Cluster cluster = remoteClusterServiceClient.describeClassicCluster(userCrn, crn, withDetails);
        if (!isBaseCluster(cluster)) {
            throw new BadRequestException("Only Classic Clusters with BASE_CLUSTER cluster type can be used as environment.");
        }
        return cluster;
    }

    @VisibleForTesting
    boolean isBaseCluster(OnPremisesApiProto.Cluster cluster) {
        return BASE_CLUSTER_TYPES.contains(cluster.getData().getClusterDetails().getClouderaManagerClusterType());
    }
}
