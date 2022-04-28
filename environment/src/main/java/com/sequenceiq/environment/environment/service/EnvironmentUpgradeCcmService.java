package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class EnvironmentUpgradeCcmService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    private final EntitlementService entitlementService;

    public EnvironmentUpgradeCcmService(EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentService environmentService, EntitlementService entitlementService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
        this.entitlementService = entitlementService;
    }

    public FlowIdentifier upgradeCcmByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        return upgradeCcm(environment);
    }

    public FlowIdentifier upgradeCcmByCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        return upgradeCcm(environment);
    }

    private FlowIdentifier upgradeCcm(EnvironmentDto environment) {
        validateEntitlements(environment);
        validateUpgrade(environment);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return reactorFlowManager.triggerCcmUpgradeFlow(environment, userCrn);
    }

    private void validateEntitlements(EnvironmentDto environment) {
        if (environment.getTunnel().useCcmV1() && !entitlementService.ccmV1ToV2JumpgateUpgradeEnabled(environment.getAccountId()) ||
                environment.getTunnel().useCcmV2() && !entitlementService.ccmV2ToV2JumpgateUpgradeEnabled(environment.getAccountId())) {

            throw new BadRequestException(
                    String.format("Environment '%s' has a tunnel type '%s' but the account is not entitled for Cluster Connectivity Manager upgrade.",
                            environment.getName(), environment.getTunnel()));
        }
    }

    private void validateUpgrade(EnvironmentDto environment) {
        if (!environment.getStatus().isCcmUpgradeablePhase()) {
            throw new BadRequestException(
                String.format("Environment '%s' must be AVAILABLE to start Cluster Connectivity Manager upgrade.", environment.getName()));
        }
    }
}
