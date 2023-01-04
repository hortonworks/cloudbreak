package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

@Service
public class EnvironmentVerticalScaleService {

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentService environmentService;

    public EnvironmentVerticalScaleService(EnvironmentReactorFlowManager reactorFlowManager, EnvironmentService environmentService) {
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
    }

    public FlowIdentifier verticalScaleByName(String name, VerticalScaleRequest updateRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByNameAndAccountId(name, accountId);
        return verticalScale(environment, updateRequest);
    }

    public FlowIdentifier verticalScaleByCrn(String crn, VerticalScaleRequest updateRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environment = environmentService.getByCrnAndAccountId(crn, accountId);
        return verticalScale(environment, updateRequest);
    }

    private FlowIdentifier verticalScale(EnvironmentDto environment, VerticalScaleRequest updateRequest) {
        validateUpgrade(environment);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return reactorFlowManager.triggerVerticalScaleFlow(environment, userCrn, updateRequest);
    }

    private void validateUpgrade(EnvironmentDto environment) {
        if (!environment.getStatus().isVerticalScaleAllowed()) {
            throw new BadRequestException(
                    String.format("Environment '%s' is not in a vertical scalable state. The environment state is %s", environment.getName(),
                            environment.getStatus()));
        }
    }

}
