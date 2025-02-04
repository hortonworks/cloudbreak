package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class EnvironmentEnableSeLinuxService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    public EnvironmentEnableSeLinuxService(EnvironmentReactorFlowManager reactorFlowManager, EnvironmentService environmentService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
    }

    public FlowIdentifier enableSeLinuxByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        return enableSeLinux(environment);
    }

    public FlowIdentifier enableSeLinuxByCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        return enableSeLinux(environment);
    }

    private FlowIdentifier enableSeLinux(EnvironmentDto environment) {
        validateSeLinuxEnable(environment);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return reactorFlowManager.triggerSeLinuxEnableFlow(environment, userCrn);
    }

    private void validateSeLinuxEnable(EnvironmentDto environment) {
        if (!environment.getStatus().isEnableSeLinuxAllowed()) {
            throw new BadRequestException(
                    String.format("Environment '%s' is not in a enable SeLinux state. The environment state is %s", environment.getName(),
                            environment.getStatus()));
        }
    }

}
