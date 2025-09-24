package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.util.Collection;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@Component
public class ClassicClusterRemoteEnvironmentConnector implements RemoteEnvironmentConnector {
    @Inject
    private ClassicClusterListService listService;

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
