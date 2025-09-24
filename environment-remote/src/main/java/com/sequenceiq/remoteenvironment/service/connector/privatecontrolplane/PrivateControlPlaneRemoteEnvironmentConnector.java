package com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_HYBRID_CLOUD;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyHybridClient;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentPropertiesV2Response;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@Service
public class PrivateControlPlaneRemoteEnvironmentConnector implements PayloadContextProvider, RemoteEnvironmentConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateControlPlaneRemoteEnvironmentConnector.class);

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

    @Override
    public RemoteEnvironmentConnectorType type() {
        return RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE;
    }

    @Override
    public Collection<SimpleRemoteEnvironmentResponse> list(String publicCloudAccountId) {
        Set<SimpleRemoteEnvironmentResponse> responses = new HashSet<>();
        if (entitlementService.hybridCloudEnabled(publicCloudAccountId)) {
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            Set<PrivateControlPlane> privateControlPlanes = privateControlPlaneService.listByAccountId(publicCloudAccountId);
            LOGGER.info("Starting to list environments from '{}' control planes with actor('{}')", privateControlPlanes.size(), userCrn);
            String controlPlaneNames = privateControlPlanes.stream().map(PrivateControlPlane::getName).collect(Collectors.joining(","));
            LOGGER.debug("Starting to list environments from control planes with name '{}' actor('{}')", controlPlaneNames, userCrn);
            privateControlPlanes
                    .forEach(item -> responses.addAll(listEnvironmentsFromPrivateControlPlaneWithActor(item, userCrn)));
        }
        return responses;
    }

    @Override
    public DescribeEnvironmentResponse describeV1(String publicCloudAccountId, DescribeRemoteEnvironment environment) {
        DescribeEnvironmentResponse describeEnvironmentResponse = callRemoteEnvironment(
                publicCloudAccountId,
                environment.getCrn(),
                this::describeRemoteEnvironmentWithActor);

        return describeEnvironmentResponse;
    }

    @Override
    public DescribeEnvironmentV2Response describeV2(String publicCloudAccountId, DescribeRemoteEnvironment environment) {
        String privateCloudAccountId = Crn.safeFromString(environment.getCrn()).getAccountId();
        DescribeEnvironmentV2Response describeEnvironmentResponse = callRemoteEnvironment(
                publicCloudAccountId,
                environment.getCrn(),
                this::describeRemoteEnvironmentWithActor);

        Optional<PrivateControlPlane> privateControlPlane =
                privateControlPlaneService.getByPrivateCloudAccountIdAndPublicCloudAccountId(
                        privateCloudAccountId,
                        publicCloudAccountId);

        DescribeEnvironmentV2Response describeEnvironmentV2Response =
                getDescribeRemoteEnvironmentV2Response(
                        describeEnvironmentResponse,
                        privateControlPlane);

        return describeEnvironmentV2Response;
    }

    private DescribeEnvironmentV2Response getDescribeRemoteEnvironmentV2Response(
            DescribeEnvironmentV2Response describeEnvironmentResponse,
            Optional<PrivateControlPlane> privateControlPlane) {

        DescribeEnvironmentPropertiesV2Response describeEnvironmentPropertiesV2Response =
                new DescribeEnvironmentPropertiesV2Response();
        if (privateControlPlane.isPresent()) {
            describeEnvironmentPropertiesV2Response.setRemoteEnvironmentUrl(
                    String.format("%s/environments/#/environments/%s",
                            privateControlPlane.get().getUrl(),
                            describeEnvironmentResponse.getEnvironment().getEnvironmentName()));
        }
        describeEnvironmentResponse.setAdditionalProperties(describeEnvironmentPropertiesV2Response);
        return describeEnvironmentResponse;
    }

    public List<SimpleRemoteEnvironmentResponse> listRemoteEnvironmentsInternal(PrivateControlPlane controlPlane) {
        MachineUser actor = umsClient.getOrCreateMachineUserWithoutAccessKey(Crn.Service.REMOTECLUSTER.getName(), controlPlane.getAccountId());
        return listEnvironmentsFromPrivateControlPlaneWithActor(controlPlane, actor.getCrn());
    }

    public DescribeEnvironmentResponse describeRemoteEnvironmentInternal(PrivateControlPlane controlPlane, String environmentCrn) {
        MachineUser actor = umsClient.getOrCreateMachineUserWithoutAccessKey(Crn.Service.REMOTECLUSTER.getName(), controlPlane.getAccountId());
        return describeRemoteEnvironmentWithActor(controlPlane, environmentCrn, actor.getCrn());
    }

    @Override
    public GetRootCertificateResponse getRootCertificate(String publicCloudAccountId, String environmentCrn) {
        return callRemoteEnvironment(publicCloudAccountId, environmentCrn, this::getRootCertificateWithActor);
    }

    @Override
    public DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(String publicCloudAccountId, String environmentCrn) {
        return callRemoteEnvironment(publicCloudAccountId, environmentCrn, this::getRdcForEnvironmentWithActor);
    }

    @Override
    public DescribeDatalakeServicesResponse getDatalakeServices(String publicCloudAccountId, String environmentCrn) {
        return callRemoteEnvironment(publicCloudAccountId, environmentCrn, this::getDatalakeServicesForEnvironmentWithActor);
    }

    private <T> T callRemoteEnvironment(String publicCloudAccountId, String environmentCrn, PrivateControlPlaneCaller<T> function) {
        if (entitlementService.hybridCloudEnabled(publicCloudAccountId)) {
            try {
                String privateCloudAccountId = Crn.safeFromString(environmentCrn).getAccountId();
                Optional<PrivateControlPlane> privateControlPlane =
                        privateControlPlaneService.getByPrivateCloudAccountIdAndPublicCloudAccountId(privateCloudAccountId, publicCloudAccountId);
                if (privateControlPlane.isPresent()) {
                    String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
                    return function.call(privateControlPlane.get(), environmentCrn, userCrn);
                } else {
                    throw new BadRequestException(String.format("There is no control plane for this account with account id %s.", privateCloudAccountId));
                }
            } catch (CrnParseException crnParseException) {
                String message = String.format("The provided environment CRN('%s') is invalid", environmentCrn);
                LOGGER.warn(message, crnParseException);
                throw new BadRequestException(message, crnParseException);
            }
        } else {
            throw new BadRequestException(String.format("Unable to fetch from remote environment since entitlement %s is not assigned", CDP_HYBRID_CLOUD));
        }
    }

    private List<SimpleRemoteEnvironmentResponse> listEnvironmentsFromPrivateControlPlaneWithActor(PrivateControlPlane controlPlane, String actorCrn) {
        String cpName = controlPlane.getName();
        String cpCrn = controlPlane.getResourceCrn();
        LOGGER.debug("The listing of private environments on control plane('{}/{}') with actor('{}') is executed by thread: {}", cpName, cpCrn,
                actorCrn, Thread.currentThread().getName());
        List<SimpleRemoteEnvironmentResponse> responses = new ArrayList<>();
        try {
            responses = measure(() -> clusterProxyHybridClient.listEnvironments(cpCrn, actorCrn)
                    .getEnvironments()
                    .stream()
                    .filter(resp -> isCrnValidAndWithinAccount(controlPlane.getPrivateCloudAccountId(), resp.getCrn()))
                    .map(environment -> {
                        LOGGER.debug("Remote environment list on private control plane: {}/{} will be executed by thread: {}", cpName, cpCrn,
                                Thread.currentThread().getName());
                        return privateControlPlaneEnvironmentToRemoteEnvironmentConverter.convert(environment, controlPlane);
                    })
                    .collect(Collectors.toList()), LOGGER, "Cluster proxy call took us {} ms for pvc {}/{}", cpName, cpCrn);
        } catch (Exception e) {
            LOGGER.warn("Failed to query environments from url '{}' of control plane '{}/{}'", controlPlane.getUrl(), cpName, cpCrn, e);
        }
        return responses;
    }

    private DescribeEnvironmentV2Response describeRemoteEnvironmentWithActor(PrivateControlPlane controlPlane, String environmentCrn, String actorCrn) {
        LOGGER.debug("The describe of remote environment('{}') with actor('{}') is executed by thread: {}", environmentCrn, actorCrn,
                Thread.currentThread().getName());
        try {
            return measure(() -> clusterProxyHybridClient.getEnvironment(controlPlane.getResourceCrn(), actorCrn, environmentCrn),
                    LOGGER, "Cluster proxy call took us {} ms for pvc {}", controlPlane.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Failed to query environment for crn {}", environmentCrn, e);
            throw new CloudbreakServiceException(String.format("Unable to fetch environment for crn %s", environmentCrn), e);
        }
    }

    private GetRootCertificateResponse getRootCertificateWithActor(PrivateControlPlane controlPlane, String environmentCrn, String actorCrn) {
        LOGGER.debug("Fetching root certificate of remote environment('{}') with actor('{}') is executed by thread: {}", environmentCrn, actorCrn,
                Thread.currentThread().getName());
        try {
            return measure(() -> clusterProxyHybridClient.getRootCertificate(controlPlane.getResourceCrn(), actorCrn, environmentCrn),
                    LOGGER, "Cluster proxy call took us {} ms for pvc {}", controlPlane.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch root certificate for crn {}", environmentCrn, e);
            throw new CloudbreakServiceException(String.format("Unable to fetch root certificate for crn %s", environmentCrn), e);
        }
    }

    private DescribeDatalakeAsApiRemoteDataContextResponse getRdcForEnvironmentWithActor(PrivateControlPlane controlPlane, String environmentCrn,
            String actorCrn) {
        LOGGER.debug("The getRdcForEnvironmentWithActor of remote environment('{}') with actor('{}') is executed by thread: {}", environmentCrn, actorCrn,
                Thread.currentThread().getName());
        try {
            return measure(() -> clusterProxyHybridClient.getRemoteDataContext(controlPlane.getResourceCrn(), actorCrn, environmentCrn),
                    LOGGER, "Cluster proxy call took us {} ms for pvc {}", controlPlane.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Failed to query environment for crn {}", environmentCrn, e);
            throw new CloudbreakServiceException(String.format("Unable to fetch remote data context for crn %s", environmentCrn), e);
        }
    }

    private DescribeDatalakeServicesResponse getDatalakeServicesForEnvironmentWithActor(PrivateControlPlane controlPlane, String environmentCrn,
            String actorCrn) {
        LOGGER.debug("The getDatalakeServicesForEnvironmentWithActor of remote environment('{}') with actor('{}') is executed by thread: {}",
                environmentCrn, actorCrn, Thread.currentThread().getName());
        try {
            return measure(() -> clusterProxyHybridClient.getDatalakeServices(controlPlane.getResourceCrn(), actorCrn, environmentCrn),
                    LOGGER, "Cluster proxy call took us {} ms for pvc {}", controlPlane.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Failed to query environment for crn {}", environmentCrn, e);
            throw new CloudbreakServiceException(String.format("Unable to fetch data lake services for crn %s", environmentCrn), e);
        }
    }

    private boolean isCrnValidAndWithinAccount(String accountId, String envCrn) {
        return Crn.isCrn(envCrn)
                && accountId.equals(Crn.safeFromString(envCrn).getAccountId());
    }
}
