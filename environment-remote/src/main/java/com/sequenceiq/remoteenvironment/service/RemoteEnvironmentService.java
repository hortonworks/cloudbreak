package com.sequenceiq.remoteenvironment.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyHybridClient;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@Service
public class RemoteEnvironmentService implements PayloadContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteEnvironmentService.class);

    @Inject
    private PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter privateControlPlaneEnvironmentToRemoteEnvironmentConverter;

    @Inject
    private PrivateControlPlaneService privateControlPlaneService;

    @Inject
    private ClusterProxyHybridClient clusterProxyHybridClient;

    @Inject
    private EntitlementService entitlementService;

    public Set<SimpleRemoteEnvironmentResponse> listRemoteEnvironment(String accountId) {
        Set<SimpleRemoteEnvironmentResponse> responses = new HashSet<>();
        if (entitlementService.hybridCloudEnabled(accountId)) {
            Set<PrivateControlPlane> privateControlPlanes = privateControlPlaneService.listByAccountId(accountId);
            privateControlPlanes.stream()
                .parallel()
                .forEach(item -> responses.addAll(getEnvironmentsFromPrivateControlPlane(item)));
        }
        return responses;
    }

    private List<SimpleRemoteEnvironmentResponse> getEnvironmentsFromPrivateControlPlane(PrivateControlPlane item) {
        List<SimpleRemoteEnvironmentResponse> responses = new ArrayList<>();
        try {
            responses = clusterProxyHybridClient.readConfig(item.getResourceCrn())
                    .getEnvironments()
                    .stream()
                    .parallel()
                    .map(environment -> privateControlPlaneEnvironmentToRemoteEnvironmentConverter.convert(environment, item))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.warn("Failed to query environments from url {}", item.getUrl());
        }
        return responses;
    }
}
