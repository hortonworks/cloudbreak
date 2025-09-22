package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
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

    @Override
    public RemoteEnvironmentConnectorType type() {
        return RemoteEnvironmentConnectorType.CLASSIC_CLUSTER;
    }

    @Override
    public DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(String publicCloudAccountId, String environmentCrn) {
        OnPremisesApiProto.Cluster cluster = getCluster(environmentCrn);
        return remoteDataContextProvider.getRemoteDataContext(cluster);
    }

    private OnPremisesApiProto.Cluster getCluster(String crn) {
        return remoteClusterServiceClient.describeClassicCluster(crn);
    }
}
