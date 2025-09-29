package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.util.Collection;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@Component
public class ClassicClusterRemoteEnvironmentConnector implements RemoteEnvironmentConnector {
    @Inject
    private ClassicClusterListService listService;

    @Inject
    private ClassicClusterDescribeService describeService;

    @Inject
    private RemoteClusterServiceClient remoteClusterServiceClient;

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
    public DescribeEnvironmentResponse describeV1(String publicCloudAccountId, DescribeRemoteEnvironment environment) {
        DescribeEnvironmentResponse describeEnvironmentResponse = new DescribeEnvironmentResponse();
        describeEnvironmentResponse.setEnvironment(describeV2(publicCloudAccountId, environment).getEnvironment());
        return describeEnvironmentResponse;
    }

    @Override
    public DescribeEnvironmentV2Response describeV2(String publicCloudAccountId, DescribeRemoteEnvironment environment) {
        OnPremisesApiProto.Cluster cluster = getCluster(environment.getCrn());
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
        return rootCertificateProvider.getRootCertificate(cluster);
    }

    private OnPremisesApiProto.Cluster getCluster(String crn) {
        return remoteClusterServiceClient.describeClassicCluster(crn);
    }
}
