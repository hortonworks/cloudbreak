package com.sequenceiq.remoteenvironment.service;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyHybridClient;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
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

    public Set<SimpleRemoteEnvironmentResponse> listRemoteEnvironments(String publicCloudAccountId) {
        Set<SimpleRemoteEnvironmentResponse> responses = new HashSet<>();
        if (entitlementService.hybridCloudEnabled(publicCloudAccountId)) {
            Set<PrivateControlPlane> privateControlPlanes = privateControlPlaneService.listByAccountId(publicCloudAccountId);
            privateControlPlanes.stream()
                    .parallel()
                    .forEach(item -> responses.addAll(listEnvironmentsFromPrivateControlPlane(item)));
        }
        return responses;
    }

    public DescribeEnvironmentResponse getRemoteEnvironment(String publicCloudAccountId, String environmentCrn) {
        DescribeEnvironmentResponse response = null;
        if (entitlementService.hybridCloudEnabled(publicCloudAccountId)) {
            String privateCloudAccountId = Crn.fromString(environmentCrn).getAccountId();
            Optional<PrivateControlPlane> privateControlPlanes =
                    privateControlPlaneService.getByPrivateCloudAccountIdAndPublicCloudAccountId(privateCloudAccountId, publicCloudAccountId);
            if (privateControlPlanes.isPresent()) {
                response = describeRemoteEnvironment(privateControlPlanes.get(), environmentCrn);
            } else {
                throw new BadRequestException(String.format("There is no control plane for this account with account id %s.", privateCloudAccountId));
            }
        }
        return response;
    }

    private List<SimpleRemoteEnvironmentResponse> listEnvironmentsFromPrivateControlPlane(PrivateControlPlane controlPlane) {
        LOGGER.debug("The processing of private control plane('{}') is executed by thread: {}", controlPlane.getName(), Thread.currentThread().getName());
        List<SimpleRemoteEnvironmentResponse> responses = new ArrayList<>();
        try {
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            responses = measure(() -> clusterProxyHybridClient.listEnvironments(controlPlane.getResourceCrn(), userCrn)
                    .getEnvironments()
                    .stream()
                    .parallel()
                    .map(environment -> {
                        LOGGER.debug("Remote environment list on private control plane: {} will be executed by thread: {}", controlPlane.getName(),
                                Thread.currentThread().getName());
                        return privateControlPlaneEnvironmentToRemoteEnvironmentConverter.convert(environment, controlPlane);
                    })
                    .collect(Collectors.toList()), LOGGER, "Cluster proxy call took us {} ms for pvc {}", controlPlane.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Failed to query environments from url {}", controlPlane.getUrl());
        }
        return responses;
    }

    private DescribeEnvironmentResponse describeRemoteEnvironment(PrivateControlPlane controlPlane, String environmentCrn) {
        LOGGER.debug("The processing of remote environment('{}') is executed by thread: {}", environmentCrn, Thread.currentThread().getName());
        DescribeEnvironmentResponse response = null;
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            response = measure(() ->
                            clusterProxyHybridClient.getEnvironment(
                                    controlPlane.getResourceCrn(),
                                    userCrn,
                                    environmentCrn),
                    LOGGER,
                    "Cluster proxy call took us {} ms for pvc {}", controlPlane.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Failed to query environment for crn {}", environmentCrn);
        }
        return response;
    }
}
