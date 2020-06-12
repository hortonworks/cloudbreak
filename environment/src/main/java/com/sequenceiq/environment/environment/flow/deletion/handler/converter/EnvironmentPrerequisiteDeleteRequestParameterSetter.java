package com.sequenceiq.environment.environment.flow.deletion.handler.converter;

import com.sequenceiq.cloudbreak.cloud.model.prerequisite.EnvironmentPrerequisiteDeleteRequest;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;

public interface EnvironmentPrerequisiteDeleteRequestParameterSetter {

    EnvironmentPrerequisiteDeleteRequest setParameters(EnvironmentPrerequisiteDeleteRequest environmentPrerequisiteDeleteRequest, EnvironmentDto environmentDto);

    String getCloudPlatform();
}
