package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;

@Service
public class EnvironmentStartService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public EnvironmentStartService(EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentService environmentService,
            ThreadBasedUserCrnProvider threadBasedUserCrnProvider) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
    }

    public void startByCrn(String crn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        reactorFlowManager.triggerStartFlow(environment.getId(), environment.getName(), userCrn);
    }

    public void startByName(String name) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        reactorFlowManager.triggerStartFlow(environment.getId(), environment.getName(), userCrn);
    }
}
