package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;

@Service
public class EnvironmentLoadBalancerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentLoadBalancerService.class);

    private final EnvironmentService environmentService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EntitlementService entitlementService;

    public EnvironmentLoadBalancerService(
            EnvironmentService environmentService,
            EnvironmentReactorFlowManager reactorFlowManager,
            EntitlementService entitlementService) {
        this.environmentService = environmentService;
        this.reactorFlowManager = reactorFlowManager;
        this.entitlementService = entitlementService;
    }

    public void updateLoadBalancerInEnvironmentAndStacks(EnvironmentDto environmentDto, EnvironmentLoadBalancerDto environmentLbDto) {
        requireNonNull(environmentDto);
        requireNonNull(environmentLbDto);
        if (!isLoadBalancerEnabledForDatalake(ThreadBasedUserCrnProvider.getAccountId(), environmentDto.getCloudPlatform(),
            environmentLbDto.getEndpointAccessGateway())) {
            throw new BadRequestException("Neither Endpoint Gateway nor Data Lake load balancer is enabled. Nothing to do.");
        }

        LOGGER.debug("Trying to find environment based on name {}, CRN {}", environmentDto.getName(), environmentDto.getResourceCrn());
        String accountId = Crn.safeFromString(environmentDto.getResourceCrn()).getAccountId();
        Environment environment = environmentService
            .findByResourceCrnAndAccountIdAndArchivedIsFalse(environmentDto.getResourceCrn(), accountId).
                orElseThrow(() -> new NotFoundException(
                    String.format("Could not find environment '%s' using crn '%s'", environmentDto.getName(),
                        environmentDto.getResourceCrn())));

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        reactorFlowManager
            .triggerLoadBalancerUpdateFlow(environmentDto, environment.getId(), environment.getName(), environment.getResourceCrn(),
                environmentLbDto.getEndpointAccessGateway(), environmentLbDto.getEndpointGatewaySubnetIds(), userCrn);
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
