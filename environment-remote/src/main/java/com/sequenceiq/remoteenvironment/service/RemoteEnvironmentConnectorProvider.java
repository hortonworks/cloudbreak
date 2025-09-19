package com.sequenceiq.remoteenvironment.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@Component
public class RemoteEnvironmentConnectorProvider {

    private final Map<RemoteEnvironmentConnectorType, RemoteEnvironmentConnector> remoteEnvironmentConnectors;

    public RemoteEnvironmentConnectorProvider(Set<RemoteEnvironmentConnector> remoteEnvironmentConnectors) {
        if (remoteEnvironmentConnectors.size() != RemoteEnvironmentConnectorType.values().length) {
            throw new IllegalStateException("Not all RemoteEnvironmentTypes have RemoteEnvironmentConnector implementations.");
        }
        this.remoteEnvironmentConnectors = remoteEnvironmentConnectors.stream()
                .collect(Collectors.toMap(RemoteEnvironmentConnector::type, Function.identity()));
    }

    public Collection<RemoteEnvironmentConnector> all() {
        return remoteEnvironmentConnectors.values();
    }

    public RemoteEnvironmentConnector getForType(RemoteEnvironmentConnectorType remoteEnvironmentConnectorType) {
        return remoteEnvironmentConnectors.get(remoteEnvironmentConnectorType);
    }

    public RemoteEnvironmentConnector getForCrn(String crn) {
        return getForType(RemoteEnvironmentConnectorType.getByCrn(crn));
    }
}
