package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest;

@Service
public class EnvironmentVerticalScaleService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    private final EntitlementService entitlementService;

    public EnvironmentVerticalScaleService(EnvironmentReactorFlowManager reactorFlowManager,
        EnvironmentService environmentService, EntitlementService entitlementService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
        this.entitlementService = entitlementService;
    }

    public FlowIdentifier verticalScaleByName(String name, FreeIPAVerticalScaleRequest updateRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        return verticalScale(environment, updateRequest);
    }

    public FlowIdentifier verticalScaleByCrn(String crn, FreeIPAVerticalScaleRequest updateRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        return verticalScale(environment, updateRequest);
    }

    private FlowIdentifier verticalScale(EnvironmentDto environment, FreeIPAVerticalScaleRequest updateRequest) {
        validateEntitlements(environment);
        validateUpgrade(environment);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return reactorFlowManager.triggerVerticalScaleFlow(environment, userCrn, updateRequest);
    }

    private void validateEntitlements(EnvironmentDto environment) {
        if (!entitlementService.verticalScaleEnabled(environment.getAccountId())) {
            throw new BadRequestException(
                    String.format("The account is not entitled for Vertical Scaling.",
                            environment.getName(), environment.getTunnel()));
        }
    }

    private void validateUpgrade(EnvironmentDto environment) {
        if (!environment.getStatus().isVerticallyScalablePhase()) {
            throw new BadRequestException(
                String.format("Environment '%s' must be STOPPED to start Vertical Scaling.", environment.getName()));
        }
    }
}
