package com.sequenceiq.environment.environment.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class EnvironmentStopService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    public EnvironmentStopService(EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentService environmentService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
    }

    public FlowIdentifier stopByCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        return stop(environment, accountId);
    }

    public FlowIdentifier stopByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        return stop(environment, accountId);
    }

    private FlowIdentifier stop(EnvironmentDto environment, String accountId) {
        validateStoppable(environment, accountId);

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return reactorFlowManager.triggerStopFlow(environment.getId(), environment.getName(), userCrn);
    }

    private void validateStoppable(EnvironmentDto environment, String accountId) {
        validateNoChildEnvironmentIsRunning(environment, accountId);
        validaFreeIpaIsNotRunningOnSpotInstances(environment);
    }

    private void validateNoChildEnvironmentIsRunning(EnvironmentDto environment, String accountId) {
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

    private void validaFreeIpaIsNotRunningOnSpotInstances(EnvironmentDto environment) {
        Integer freeIpaSpotPercentage = Optional.ofNullable(environment.getFreeIpaCreation())
                .map(FreeIpaCreationDto::getAws)
                .map(FreeIpaCreationAwsParametersDto::getSpot)
                .map(FreeIpaCreationAwsSpotParametersDto::getPercentage)
                .orElse(0);

        if (freeIpaSpotPercentage != 0) {
            String message = String.format("Environment [%s] can not be stopped because FreeIpa is running on spot instances.", environment.getName());
            throw new BadRequestException(message);
        }
    }
}
