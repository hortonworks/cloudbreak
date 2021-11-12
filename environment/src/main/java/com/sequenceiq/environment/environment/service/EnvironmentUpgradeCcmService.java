package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class EnvironmentUpgradeCcmService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    public EnvironmentUpgradeCcmService(EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentService environmentService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
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
        validateUpgrade(environment);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return reactorFlowManager.triggerCcmUpgradeFlow(environment, userCrn);
    }

    private void validateUpgrade(EnvironmentDto environment) {
        if (environment.getStatus() != EnvironmentStatus.AVAILABLE) {
            throw new BadRequestException(
                String.format("Environment '%s' must be AVAILABLE to start Cluster Connectivity Manager upgrade.", environment.getName()));
        }
    }
}
