package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;

@Service
public class EnvironmentUpgradeCcmService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    public EnvironmentUpgradeCcmService(EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentService environmentService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
    }

    public void upgradeCcmByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        upgradeCcm(environment);
    }

    public void upgradeCcmByCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        upgradeCcm(environment);
    }

    private void upgradeCcm(EnvironmentDto environment) {
        validateUpgrade(environment);
        reactorFlowManager.triggerCcmUpgradeFlow(environment);
    }

    private void validateUpgrade(EnvironmentDto environment) {
        if (environment.getStatus() != EnvironmentStatus.AVAILABLE) {
            throw new BadRequestException(
                String.format("Environment '%s' must be AVAILABLE to start Cluster Connectivity Manager upgrade.", environment.getName()));
        }
    }
}
