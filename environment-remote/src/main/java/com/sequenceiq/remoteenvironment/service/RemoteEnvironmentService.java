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
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
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

    @Inject
    private GrpcUmsClient umsClient;

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
            try {
                String privateCloudAccountId = Crn.safeFromString(environmentCrn).getAccountId();
                Optional<PrivateControlPlane> privateControlPlane =
                        privateControlPlaneService.getByPrivateCloudAccountIdAndPublicCloudAccountId(privateCloudAccountId, publicCloudAccountId);
                if (privateControlPlane.isPresent()) {
                    String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
                    response = describeRemoteEnvironmentWithActor(privateControlPlane.get(), environmentCrn, userCrn);
                } else {
                    throw new BadRequestException(String.format("There is no control plane for this account with account id %s.", privateCloudAccountId));
                }
            } catch (CrnParseException crnParseException) {
                String message = String.format("The provided environment CRN('%s') is invalid", environmentCrn);
                LOGGER.info(message);
                throw new BadRequestException(message, crnParseException);
            }
        }
        return response;
    }

    private List<SimpleRemoteEnvironmentResponse> listEnvironmentsFromPrivateControlPlane(PrivateControlPlane controlPlane) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return listEnvironmentsFromPrivateControlPlaneWithActor(controlPlane, userCrn);
    }

    public List<SimpleRemoteEnvironmentResponse> listRemoteEnvironmentsInternal(PrivateControlPlane controlPlane) {
        MachineUser actor = umsClient.getOrCreateMachineUserWithoutAccessKey(Crn.Service.REMOTECLUSTER.getName(), controlPlane.getAccountId());
        return listEnvironmentsFromPrivateControlPlaneWithActor(controlPlane, actor.getCrn());
    }

    public DescribeEnvironmentResponse describeRemoteEnvironmentInternal(PrivateControlPlane controlPlane, String environmentCrn) {
        MachineUser actor = umsClient.getOrCreateMachineUserWithoutAccessKey(Crn.Service.REMOTECLUSTER.getName(), controlPlane.getAccountId());
        return describeRemoteEnvironmentWithActor(controlPlane, environmentCrn, actor.getCrn());
    }

    private List<SimpleRemoteEnvironmentResponse> listEnvironmentsFromPrivateControlPlaneWithActor(PrivateControlPlane controlPlane, String actorCrn) {
        LOGGER.debug("The listing of private environments on control plane('{}') with actor('{}') is executed by thread: {}", controlPlane.getName(),
                actorCrn, Thread.currentThread().getName());
        List<SimpleRemoteEnvironmentResponse> responses = new ArrayList<>();
        try {
            responses = measure(() -> clusterProxyHybridClient.listEnvironments(controlPlane.getResourceCrn(), actorCrn)
                    .getEnvironments()
                    .stream()
                    .filter(resp -> isCrnValidAndWithinAccount(controlPlane.getPrivateCloudAccountId(), resp.getCrn()))
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

    private DescribeEnvironmentResponse describeRemoteEnvironmentWithActor(PrivateControlPlane controlPlane, String environmentCrn, String actorCrn) {
        LOGGER.debug("The describe of remote environment('{}') with actor('{}') is executed by thread: {}", environmentCrn, actorCrn,
                Thread.currentThread().getName());
        DescribeEnvironmentResponse response = null;
        try {
            response = measure(() ->
                            clusterProxyHybridClient.getEnvironment(
                                    controlPlane.getResourceCrn(),
                                    actorCrn,
                                    environmentCrn),
                    LOGGER,
                    "Cluster proxy call took us {} ms for pvc {}", controlPlane.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Failed to query environment for crn {}", environmentCrn);
        }
        return response;
    }

    private boolean isCrnValidAndWithinAccount(String accountId, String envCrn) {
        return Crn.isCrn(envCrn)
                && accountId.equals(Crn.safeFromString(envCrn).getAccountId());
    }
}
