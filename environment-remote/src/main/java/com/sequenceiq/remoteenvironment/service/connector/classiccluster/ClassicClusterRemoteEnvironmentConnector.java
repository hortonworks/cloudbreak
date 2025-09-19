package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@Component
public class ClassicClusterRemoteEnvironmentConnector implements RemoteEnvironmentConnector {

    @Override
    public RemoteEnvironmentConnectorType type() {
        return RemoteEnvironmentConnectorType.CLASSIC_CLUSTER;
    }
}
