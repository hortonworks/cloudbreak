package com.sequenceiq.remoteenvironment.service;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_HYBRID_CLOUD;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;
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

    public SimpleRemoteEnvironmentResponses list(List<String> types) {
        MDCBuilder.buildMdcContext();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (entitlementService.hybridCloudEnabled(accountId)) {
            Set<SimpleRemoteEnvironmentResponse> responseList = getConnectorTypes(types).parallelStream()
                    .flatMap(ct -> remoteEnvironmentConnectorProvider.getForType(ct).list(accountId).stream())
                    .collect(Collectors.toSet());
            return new SimpleRemoteEnvironmentResponses(responseList);
        } else {
            return new SimpleRemoteEnvironmentResponses();
        }
    }

    public DescribeEnvironmentResponse describeV1(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        throwExceptionIfNotEntitled(accountId);
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .describeV1(accountId, request);
    }

    public DescribeEnvironmentV2Response describeV2(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        throwExceptionIfNotEntitled(accountId);
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .describeV2(accountId, request);
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
            throw new RemoteEnvironmentException(String.format("Entitlement %s is required for this operation", CDP_HYBRID_CLOUD));
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
