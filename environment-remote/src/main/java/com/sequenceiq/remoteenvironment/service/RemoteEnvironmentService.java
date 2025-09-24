package com.sequenceiq.remoteenvironment.service;

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
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.controller.v1.controller.RemoteEnvironmentController;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@Service
public class RemoteEnvironmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteEnvironmentController.class);

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

    public DescribeEnvironmentResponse getByCrn(DescribeRemoteEnvironment request) {
        MDCBuilder.buildMdcContext();
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .describeV1(ThreadBasedUserCrnProvider.getAccountId(), request);
    }

    public DescribeDatalakeAsApiRemoteDataContextResponse getRdcByCrn(DescribeRemoteEnvironment request) {
        return remoteEnvironmentConnectorProvider.getForCrn(request.getCrn())
                .getRemoteDataContext(ThreadBasedUserCrnProvider.getAccountId(), request.getCrn());
    }

    public DescribeDatalakeServicesResponse getDatalakeServicesByCrn(DescribeDatalakeServicesRequest request) {
        return remoteEnvironmentConnectorProvider.getForCrn(request.getClusterid())
                .getDatalakeServices(ThreadBasedUserCrnProvider.getAccountId(), request.getClusterid());
    }

    public GetRootCertificateResponse getRootCertificateByCrn(String environmentCrn) {
        return remoteEnvironmentConnectorProvider.getForCrn(environmentCrn)
                .getRootCertificate(ThreadBasedUserCrnProvider.getAccountId(), environmentCrn);
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
