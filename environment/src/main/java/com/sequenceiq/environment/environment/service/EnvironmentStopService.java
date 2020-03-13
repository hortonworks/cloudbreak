package com.sequenceiq.environment.environment.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;

@Service
public class EnvironmentStopService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    public EnvironmentStopService(EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentService environmentService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
    }

    public void stopByCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        stop(environment, accountId);
    }

    public void stopByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        stop(environment, accountId);
    }

    private void stop(EnvironmentDto environment, String accountId) {
        validateStoppable(environment, accountId);

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        reactorFlowManager.triggerStopFlow(environment.getId(), environment.getName(), userCrn);
    }

    private void validateStoppable(EnvironmentDto environment, String accountId) {
        List<String> runningChildEnvironmentNames = environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(accountId, environment.getId())
                .stream()
                .filter(childEnvironment -> EnvironmentStatus.ENV_STOPPED != childEnvironment.getStatus())
                .map(Environment::getName)
                .collect(Collectors.toList());
        if (!runningChildEnvironmentNames.isEmpty()) {
            String message = String.format(
                    "The following child Environment(s) have to be stopped before Environment stop: [%s]",
                    String.join(", ", runningChildEnvironmentNames));
            throw new BadRequestException(message);
        }
    }
}
