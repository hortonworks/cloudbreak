package com.sequenceiq.cloudbreak.service.environment;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

@Service
public class EnvironmentService {

    @Inject
    private EnvironmentClientService environmentClientService;

    public void checkEnvironmentStatus(Stack stack, Set<EnvironmentStatus> desiredStatuses) {
        if (stack.getEnvironmentCrn() != null) {
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            if (!desiredStatuses.contains(environmentResponse.getEnvironmentStatus())) {
                throw new BadRequestException("This action requires the Environment to be available, but the status is "
                        + environmentResponse.getEnvironmentStatus().getDescription());
            }
        }
    }

    public boolean environmentStatusInDesiredState(Stack stack, Set<EnvironmentStatus> desiredStatuses) {
        if (stack.getEnvironmentCrn() != null) {
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            return desiredStatuses.contains(environmentResponse.getEnvironmentStatus());
        }
        return false;
    }
}
