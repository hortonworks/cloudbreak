package com.sequenceiq.distrox.v1.distrox.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

@Service
public class DistroxService {

    private final StackOperations stackOperations;

    private final WorkspaceService workspaceService;

    private final EnvironmentClientService environmentClientService;

    private final DistroXV1RequestToStackV4RequestConverter stackRequestConverter;

    public DistroxService(EnvironmentClientService environmentClientService, StackOperations stackOperations, WorkspaceService workspaceService,
            DistroXV1RequestToStackV4RequestConverter stackRequestConverter) {
        this.environmentClientService = environmentClientService;
        this.stackRequestConverter = stackRequestConverter;
        this.workspaceService = workspaceService;
        this.stackOperations = stackOperations;
    }

    public StackV4Response post(DistroXV1Request request) {
        validate(request);
        return stackOperations.post(
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(request),
                true);
    }

    private void validate(DistroXV1Request request) {
        DetailedEnvironmentResponse environment = Optional.ofNullable(environmentClientService.getByName(request.getEnvironmentName()))
                .orElseThrow(() -> new BadRequestException("No environment name provided hence unable to obtain some important data"));
        if (environment != null && environment.getEnvironmentStatus() != EnvironmentStatus.AVAILABLE) {
            throw new BadRequestException(String.format("Environment state is %s instead of AVAILABLE", environment.getEnvironmentStatus()));
        }
    }

}
