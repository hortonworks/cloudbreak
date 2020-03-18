package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;

@Service
public class EnvironmentStartService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    public EnvironmentStartService(EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentService environmentService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
    }

    public void startByCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        start(environment, accountId);
    }

    public void startByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        start(environment, accountId);
    }

    private void start(EnvironmentDto environment, String accountId) {
        validateStartable(environment, accountId);

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        reactorFlowManager.triggerStartFlow(environment.getId(), environment.getName(), userCrn);
    }

    private void validateStartable(EnvironmentDto environment, String accountId) {
        if (Strings.isNullOrEmpty(environment.getParentEnvironmentCrn())) {
            return;
        }
        EnvironmentDto parentEnvironment = environmentService.getByCrnAndAccountId(environment.getParentEnvironmentCrn(), accountId);
        if (parentEnvironment.getStatus() != EnvironmentStatus.AVAILABLE) {
            throw new BadRequestException(String.format("Parent Environment [%s] must be available to start Environment.", parentEnvironment.getName()));
        }
    }
}
