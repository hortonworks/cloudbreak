package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentLoadBalancerUpdateResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.network.service.LoadBalancerEntitlementService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class EnvironmentLoadBalancerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentLoadBalancerService.class);

    private final EnvironmentService environmentService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EntitlementService entitlementService;

    private final LoadBalancerEntitlementService loadBalancerEntitlementService;

    public EnvironmentLoadBalancerService(
            EnvironmentService environmentService,
            EnvironmentReactorFlowManager reactorFlowManager,
            EntitlementService entitlementService,
            LoadBalancerEntitlementService loadBalancerEntitlementService) {
        this.environmentService = environmentService;
        this.reactorFlowManager = reactorFlowManager;
        this.entitlementService = entitlementService;
        this.loadBalancerEntitlementService = loadBalancerEntitlementService;
    }

    public EnvironmentLoadBalancerUpdateResponse updateLoadBalancerInEnvironmentAndStacks(EnvironmentDto environmentDto,
            EnvironmentLoadBalancerDto environmentLbDto) {
        validateUpdateRequest(environmentDto, environmentLbDto);

        Environment environment = getEnvironmentFromCrn(environmentDto);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        LOGGER.info("Initiating load balancer update flow");
        Optional<FlowIdentifier> flowIdentifier = reactorFlowManager
            .triggerLoadBalancerUpdateFlow(environmentDto, environment.getId(), environment.getName(), environment.getResourceCrn(),
                environmentLbDto.getEndpointAccessGateway(), environmentLbDto.getEndpointGatewaySubnetIds(), userCrn);

        return createUpdateResponse(environmentLbDto, flowIdentifier);
    }

    private void validateUpdateRequest(EnvironmentDto environmentDto, EnvironmentLoadBalancerDto environmentLbDto) {
        requireNonNull(environmentDto);
        requireNonNull(environmentLbDto);

        loadBalancerEntitlementService.validateNetworkForEndpointGateway(environmentDto.getCloudPlatform(), environmentDto.getName(),
            environmentLbDto.getEndpointAccessGateway());

        if (!isLoadBalancerEnabledForDatalake(ThreadBasedUserCrnProvider.getAccountId(), environmentDto.getCloudPlatform(),
            environmentLbDto.getEndpointAccessGateway())) {
            throw new BadRequestException("Neither Endpoint Gateway nor Data Lake load balancer is enabled. Nothing to do.");
        }
    }

    private Environment getEnvironmentFromCrn(EnvironmentDto environmentDto) {
        LOGGER.debug("Trying to find environment based on name {}, CRN {}", environmentDto.getName(), environmentDto.getResourceCrn());
        String accountId = Crn.safeFromString(environmentDto.getResourceCrn()).getAccountId();
        return environmentService
            .findByResourceCrnAndAccountIdAndArchivedIsFalse(environmentDto.getResourceCrn(), accountId).
                orElseThrow(() -> new NotFoundException(
                    String.format("Could not find environment '%s' using crn '%s'", environmentDto.getName(),
                        environmentDto.getResourceCrn())));
    }

    private EnvironmentLoadBalancerUpdateResponse createUpdateResponse(EnvironmentLoadBalancerDto environmentLbDto,
            Optional<FlowIdentifier> flowIdentifier) {
        EnvironmentLoadBalancerUpdateResponse response = new EnvironmentLoadBalancerUpdateResponse();
        response.setRequestedPublicEndpointGateway(environmentLbDto.getEndpointAccessGateway());
        response.setRequestedEndpointSubnetIds(environmentLbDto.getEndpointGatewaySubnetIds());
        if (flowIdentifier.isPresent()) {
            response.setFlowId(flowIdentifier.get());
            response.setStatus(LoadBalancerUpdateStatus.IN_PROGRESS);
        } else {
            throw new IllegalStateException("Unable to initiate update flow.");
        }
        return response;
    }

    private boolean isLoadBalancerEnabledForDatalake(String accountId, String cloudPlatform, PublicEndpointAccessGateway endpointEnum) {
        return !isLoadBalancerEntitlementRequiredForCloudProvider(cloudPlatform) ||
                entitlementService.datalakeLoadBalancerEnabled(accountId) ||
                PublicEndpointAccessGateway.ENABLED.equals(endpointEnum);
    }

    private boolean isLoadBalancerEntitlementRequiredForCloudProvider(String cloudPlatform) {
        return !(AWS.equalsIgnoreCase(cloudPlatform));
    }
}
