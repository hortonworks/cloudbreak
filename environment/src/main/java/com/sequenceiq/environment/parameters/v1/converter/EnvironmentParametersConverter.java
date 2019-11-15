package com.sequenceiq.environment.parameters.v1.converter;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

public interface EnvironmentParametersConverter {

    CloudPlatform getCloudPlatform();

    BaseParameters convert(Environment environment, ParametersDto parametersDto);

    ParametersDto convertToDto(BaseParameters source);
}
