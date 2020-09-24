package com.sequenceiq.environment.parameters.v1.converter;


import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.domain.YarnParameters;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto.Builder;
import com.sequenceiq.environment.parameters.dto.YarnParametersDto;

@Component
public class YarnEnvironmentParametersConverter extends BaseEnvironmentParametersConverter {

    @Override
    protected BaseParameters createInstance() {
        return new YarnParameters();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }

    @Override
    protected void postConvert(BaseParameters baseParameters, Environment environment, ParametersDto parametersDto) {
        super.postConvert(baseParameters, environment, parametersDto);
    }

    @Override
    protected void postConvertToDto(Builder builder, BaseParameters source) {
        super.postConvertToDto(builder, source);
        builder.withYarnParameters(YarnParametersDto.builder().build());
    }
}
