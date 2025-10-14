package com.sequenceiq.remoteenvironment.service;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_HYBRID_CLOUD;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@Service
public class RemoteEnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteEnvironmentService.class);

    @Inject
    private RemoteEnvironmentConnectorProvider remoteEnvironmentConnectorProvider;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    public SimpleRemoteEnvironmentResponses list(List<String> types) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.hybridCloudEnabled(accountId)) {
            return new SimpleRemoteEnvironmentResponses();
        }

        Collection<RemoteEnvironmentConnectorType> connectorTypes = getConnectorTypes(types);

        boolean wantClassic = connectorTypes.contains(RemoteEnvironmentConnectorType.CLASSIC_CLUSTER);
        boolean wantPrivate = connectorTypes.contains(RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE);

        Collection<SimpleRemoteEnvironmentResponse> result = new HashSet<>();

        if (wantPrivate && !wantClassic) {
            LOGGER.info("Only PRIVATE_CONTROL_PLANE or empty type: list private environments only");
            result.addAll(remoteEnvironmentConnectorProvider.getForType(RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE)
                    .list(accountId));
        } else if (wantClassic && !wantPrivate) {
            result.addAll(filterClassicClustersWithoutPrivateEnvironments(accountId));
        } else {
            LOGGER.info("Both: list all classic clusters and private environments, no filtering");
            Set<SimpleRemoteEnvironmentResponse> remoteEnvironments = getRemoteEnvironmentsByConnectorType(accountId).values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            result.addAll(remoteEnvironments);
        }

        return new SimpleRemoteEnvironmentResponses(result);
    }

    private Collection<SimpleRemoteEnvironmentResponse> filterClassicClustersWithoutPrivateEnvironments(String accountId) {
        LOGGER.info("Only CLASSIC_CLUSTER: list classic clusters, filter out those with private envs");
        Map<RemoteEnvironmentConnectorType, Collection<SimpleRemoteEnvironmentResponse>> remoteEnvsByType = getRemoteEnvironmentsByConnectorType(accountId);

        Collection<SimpleRemoteEnvironmentResponse> classicClusters = remoteEnvsByType.get(RemoteEnvironmentConnectorType.CLASSIC_CLUSTER);
        LOGGER.debug("Classic clusters found: {}", classicClusters);

        Collection<String> privateEnvCrns = remoteEnvsByType
                .get(RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE)
                .stream()
                .map(SimpleRemoteEnvironmentResponse::getEnvironmentCrn)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LOGGER.debug("Private environment CRNs found: {}", privateEnvCrns);

        Collection<SimpleRemoteEnvironmentResponse> result = classicClusters.stream()
                .filter(cluster -> privateEnvCrns.contains(cluster.getEnvironmentCrn())
                        || !StringUtils.hasText(cluster.getEnvironmentCrn()))
                .collect(Collectors.toSet());
        LOGGER.debug("Classic clusters after filtering: {}", result);
        return result;
    }

    private Map<RemoteEnvironmentConnectorType, Collection<SimpleRemoteEnvironmentResponse>> getRemoteEnvironmentsByConnectorType(String accountId) {
        Map<RemoteEnvironmentConnectorType, Future<Collection<SimpleRemoteEnvironmentResponse>>> remoteEnvironmentFutures = new HashMap<>();
        List<RemoteEnvironmentConnectorType> remoteEnvTypes =
                List.of(RemoteEnvironmentConnectorType.CLASSIC_CLUSTER, RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE);
        for (RemoteEnvironmentConnectorType type : remoteEnvTypes) {
            remoteEnvironmentFutures.put(type,
                    intermediateBuilderExecutor.submit(() -> remoteEnvironmentConnectorProvider.getForType(type).list(accountId)));
        }

        Map<RemoteEnvironmentConnectorType, Collection<SimpleRemoteEnvironmentResponse>> remoteEnvsByType = remoteEnvironmentFutures.entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    try {
                        return entry.getValue().get();
                    } catch (Exception e) {
                        LOGGER.error("Cannot list remote environments for type {}", entry.getKey(), e);
                        return Set.of();
                    }
                }));
        return remoteEnvsByType;
    }

    public DescribeEnvironmentResponse describeV1(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        throwExceptionIfNotEntitled(accountId);
        DescribeEnvironmentResponse describeEnvironmentResponse = remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .describeV1(accountId, request.getCrn());
        Optional.ofNullable(describeEnvironmentResponse.getEnvironment()).ifPresent(environment -> environment.setCrn(request.getCrn()));
        return describeEnvironmentResponse;
    }

    public DescribeEnvironmentV2Response describeV2(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        throwExceptionIfNotEntitled(accountId);
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .describeV2(accountId, request.getCrn());
    }

    public DescribeDatalakeAsApiRemoteDataContextResponse getRdcByCrn(DescribeRemoteEnvironment request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        throwExceptionIfNotEntitled(accountId);
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .getRemoteDataContext(accountId, request.getCrn());
    }

    public DescribeDatalakeServicesResponse getDatalakeServicesByCrn(DescribeDatalakeServicesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        throwExceptionIfNotEntitled(accountId);
        return remoteEnvironmentConnectorProvider.getForCrn(request.getClusterid())
                .getDatalakeServices(accountId, request.getClusterid());
    }

    public GetRootCertificateResponse getRootCertificateByCrn(String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        throwExceptionIfNotEntitled(accountId);
        return remoteEnvironmentConnectorProvider.getForCrn(environmentCrn)
                .getRootCertificate(accountId, environmentCrn);
    }

    private void throwExceptionIfNotEntitled(String accountId) {
        if (!entitlementService.hybridCloudEnabled(accountId)) {
            throw new BadRequestException(String.format("Entitlement %s is required for this operation", CDP_HYBRID_CLOUD));
        }
    }

    private Set<RemoteEnvironmentConnectorType> getConnectorTypes(List<String> types) {
        Set<RemoteEnvironmentConnectorType> connectorTypes = types.stream()
                .map(RemoteEnvironmentConnectorType::safeValueOf)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (connectorTypes.isEmpty()) {
            LOGGER.debug("The given connector types {} cannot be converted, falling back to PRIVATE_CONTROL_PLANE", types);
            return Set.of(RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE);
        } else {
            LOGGER.debug("Connector types are converted from {} to {}", types, connectorTypes);
            return connectorTypes;
        }
    }
}
