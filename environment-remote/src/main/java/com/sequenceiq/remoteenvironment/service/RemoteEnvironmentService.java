package com.sequenceiq.remoteenvironment.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyHybridClient;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@Service
public class RemoteEnvironmentService implements PayloadContextProvider {

    @Inject
    private PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter privateControlPlaneEnvironmentToRemoteEnvironmentConverter;

    @Inject
    private PrivateControlPlaneService privateControlPlaneService;

    @Inject
    private ClusterProxyHybridClient clusterProxyHybridClient;

    public Set<SimpleRemoteEnvironmentResponse> listRemoteEnvironment(String accountId) {
        Set<SimpleRemoteEnvironmentResponse> responses = new HashSet<>();
        privateControlPlaneService.listByAccountId(accountId)
                .stream()
                .parallel()
                .forEach(item -> responses.addAll(getEnvironmentsFromPrivateControlPlane(item)));
        return responses;
    }

    private List<SimpleRemoteEnvironmentResponse> getEnvironmentsFromPrivateControlPlane(PrivateControlPlane item) {
        return clusterProxyHybridClient.readConfig(item.getResourceCrn())
            .getEnvironments()
            .stream()
            .parallel()
            .map(environment -> privateControlPlaneEnvironmentToRemoteEnvironmentConverter.convert(environment, item))
            .collect(Collectors.toList());
    }
}
