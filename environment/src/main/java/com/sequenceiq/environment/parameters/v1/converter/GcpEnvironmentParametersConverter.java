package com.sequenceiq.environment.parameters.v1.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.domain.GcpParameters;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto.Builder;

@Component
public class GcpEnvironmentParametersConverter extends BaseEnvironmentParametersConverter {

    @Override
    protected BaseParameters createInstance() {
        return new GcpParameters();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    protected void postConvert(BaseParameters baseParameters, Environment environment, ParametersDto parametersDto) {
        super.postConvert(baseParameters, environment, parametersDto);
        GcpParameters gcpParameters = (GcpParameters) baseParameters;
        Optional<GcpParametersDto> gcpParametersDto = Optional.of(parametersDto)
                .map(ParametersDto::getGcpParametersDto);
        gcpParameters.setEncryptionKey(gcpParametersDto
                .map(GcpParametersDto::getGcpResourceEncryptionParametersDto)
                .map(GcpResourceEncryptionParametersDto::getEncryptionKey)
                .orElse(null));
    }

    @Override
    protected void postConvertToDto(Builder builder, BaseParameters source) {
        super.postConvertToDto(builder, source);
        GcpParameters gcpParameters = (GcpParameters) source;
        builder.withGcpParameters(GcpParametersDto.builder()
                .withEncryptionParameters(
                        GcpResourceEncryptionParametersDto.builder()
                                .withEncryptionKey(gcpParameters.getEncryptionKey())
                                .build())
                .build());
    }
}