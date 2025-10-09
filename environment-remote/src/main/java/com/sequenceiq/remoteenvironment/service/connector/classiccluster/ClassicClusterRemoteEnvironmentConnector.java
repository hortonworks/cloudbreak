package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.util.Collection;

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
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;
import com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneRemoteEnvironmentConnector;

@Component
public class ClassicClusterRemoteEnvironmentConnector implements RemoteEnvironmentConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterRemoteEnvironmentConnector.class);

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
    public Collection<SimpleRemoteEnvironmentResponse> list(String publicCloudAccountId) {
        return listService.list();
    }

    @Override
    public DescribeEnvironmentResponse describeV1(String publicCloudAccountId, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(environmentCrn);
        if (StringUtils.isNotEmpty(cluster.getEnvironmentCrn())) {
            try {
                LOGGER.info("Forwarding describeV1 to connected Private Control Plane");
                return privateControlPlaneRemoteEnvironmentConnector.describeV1(publicCloudAccountId, cluster.getEnvironmentCrn());
            } catch (Exception e) {
                LOGGER.warn("Failed to forward describeV1 to connected Private Control Plane", e);
                throw e;
            }
        }
        return describeService.describe(cluster).toV1Response();
    }

    @Override
    public DescribeEnvironmentV2Response describeV2(String publicCloudAccountId, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(environmentCrn);
        if (StringUtils.isNotEmpty(cluster.getEnvironmentCrn())) {
            try {
                LOGGER.info("Forwarding describeV2 to connected Private Control Plane");
                return privateControlPlaneRemoteEnvironmentConnector.describeV2(publicCloudAccountId, cluster.getEnvironmentCrn());
            } catch (Exception e) {
                LOGGER.warn("Failed to forward describeV2 to connected Private Control Plane", e);
                throw e;
            }
        }
        return describeService.describe(cluster);
    }

    @Override
    public DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(String publicCloudAccountId, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(environmentCrn);
        return remoteDataContextProvider.getRemoteDataContext(cluster);
    }

    @Override
    public DescribeDatalakeServicesResponse getDatalakeServices(String publicCloudAccountId, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(environmentCrn);
        return datalakeServicesProvider.getDatalakeServices(cluster);
    }

    @Override
    public GetRootCertificateResponse getRootCertificate(String publicCloudAccountId, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(environmentCrn);
        if (StringUtils.isNotEmpty(cluster.getEnvironmentCrn())) {
            try {
                LOGGER.info("Forwarding getRootCertificate to connected Private Control Plane");
                return privateControlPlaneRemoteEnvironmentConnector.getRootCertificate(publicCloudAccountId, cluster.getEnvironmentCrn());
            } catch (Exception e) {
                LOGGER.warn("Failed to forward getRootCertificate to connected Private Control Plane", e);
                throw e;
            }
        }
        return rootCertificateProvider.getRootCertificate(cluster);
    }

    private OnPremisesApiProto.Cluster getCluster(String crn) {
        return remoteClusterServiceClient.describeClassicCluster(crn);
    }
}
