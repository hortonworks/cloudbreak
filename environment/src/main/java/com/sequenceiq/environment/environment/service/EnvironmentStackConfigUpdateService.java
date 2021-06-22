package com.sequenceiq.environment.environment.service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentStackConfigUpdateService {
    private final EnvironmentService environmentService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    public EnvironmentStackConfigUpdateService(
        EnvironmentService environmentService,
        EnvironmentReactorFlowManager reactorFlowManager) {
        this.environmentService = environmentService;
        this.reactorFlowManager = reactorFlowManager;
    }

    public void updateAllStackConfigsByCrn(String envCrn) {
        String accountId = Crn.safeFromString(envCrn).getAccountId();
        Environment environment = environmentService
            .findByResourceCrnAndAccountIdAndArchivedIsFalse(envCrn, accountId).
                orElseThrow(() -> new NotFoundException(
                    String.format("No environment found with crn '%s'", envCrn)));

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        reactorFlowManager
            .triggerStackConfigUpdatesFlow(environment, userCrn);
    }
}
