package com.sequenceiq.environment.parameters.v1.converter;

import jakarta.inject.Inject;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto.Builder;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;

public abstract class BaseEnvironmentParametersConverter implements EnvironmentParametersConverter {

    @Inject
    private EnvironmentViewConverter environmentViewConverter;

    @Override
    public BaseParameters convert(Environment environment, ParametersDto parametersDto) {
        if (parametersDto == null) {
            return null;
        }
        BaseParameters baseParameters = createInstance();
        baseParameters.setId(parametersDto.getId());
        baseParameters.setName(environment.getName());
        baseParameters.setAccountId(environment.getAccountId());
        baseParameters.setEnvironment(environmentViewConverter.convert(environment));
        postConvert(baseParameters, environment, parametersDto);
        return baseParameters;
    }

    @Override
    public ParametersDto convertToDto(BaseParameters source) {
        if (source == null) {
            return null;
        }
        Builder builder = ParametersDto.builder()
                .withId(source.getId())
                .withAccountId(source.getAccountId())
                .withDistributionList(source.getDistributionList())
                .withName(source.getName());

        postConvertToDto(builder, source);
        return builder.build();
    }

    protected abstract BaseParameters createInstance();

    protected void postConvert(BaseParameters baseParameters, Environment environment, ParametersDto parametersDto) {
    }

    protected void postConvertToDto(Builder parametersDtoBuilder, BaseParameters baseParameters) {
    }
}
