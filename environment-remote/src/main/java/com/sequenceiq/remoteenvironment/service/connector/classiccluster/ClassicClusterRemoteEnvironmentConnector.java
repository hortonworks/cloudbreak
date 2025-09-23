package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@Component
public class ClassicClusterRemoteEnvironmentConnector implements RemoteEnvironmentConnector {

    @Inject
    private RemoteClusterServiceClient remoteClusterServiceClient;

    @Inject
    private ClassicClusterRemoteDataContextProvider remoteDataContextProvider;

    @Inject
    private ClassicClusterDatalakeServicesProvider datalakeServicesProvider;

    @Override
    public RemoteEnvironmentConnectorType type() {
        return RemoteEnvironmentConnectorType.CLASSIC_CLUSTER;
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

    private OnPremisesApiProto.Cluster getCluster(String crn) {
        return remoteClusterServiceClient.describeClassicCluster(crn);
    }
}
