package com.sequenceiq.environment.parameters.v1.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto.Builder;

@Component
public class AwsEnvironmentParametersConverter extends BaseEnvironmentParametersConverter {

    @Override
    protected BaseParameters createInstance() {
        return new AwsParameters();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    protected void postConvert(BaseParameters baseParameters, Environment environment, ParametersDto parametersDto) {
        super.postConvert(baseParameters, environment, parametersDto);
        AwsParameters awsParameters = (AwsParameters) baseParameters;
        Optional<AwsParametersDto> awsParametersDto = Optional.of(parametersDto)
                .map(ParametersDto::getAwsParametersDto);
        awsParameters.setS3guardTableName(awsParametersDto
                .map(AwsParametersDto::getS3GuardTableName)
                .orElse(null));
        awsParameters.setS3guardTableCreation(awsParametersDto
                .map(AwsParametersDto::getDynamoDbTableCreation)
                .orElse(null));
        awsParameters.setFreeIpaSpotPercentage(awsParametersDto
                .map(AwsParametersDto::getFreeIpaSpotPercentage)
                .orElse(0));
        awsParameters.setFreeIpaSpotMaxPrice(awsParametersDto
                .map(AwsParametersDto::getFreeIpaSpotMaxPrice)
                .orElse(null));
        awsParameters.setEncryptionKeyArn(awsParametersDto
                .map(AwsParametersDto::getAwsDiskEncryptionParametersDto)
                .map(AwsDiskEncryptionParametersDto::getEncryptionKeyArn)
                .orElse(null));
    }

    @Override
    protected void postConvertToDto(Builder builder, BaseParameters source) {
        super.postConvertToDto(builder, source);
        AwsParameters awsParameters = (AwsParameters) source;
        builder.withAwsParameters(AwsParametersDto.builder()
                .withDynamoDbTableName(awsParameters.getS3guardTableName())
                .withDynamoDbTableCreation(awsParameters.getS3guardTableCreation())
                .withFreeIpaSpotPercentage(awsParameters.getFreeIpaSpotPercentage())
                .withFreeIpaSpotMaxPrice(awsParameters.getFreeIpaSpotMaxPrice())
                .withAwsDiskEncryptionParameters(AwsDiskEncryptionParametersDto.builder()
                        .withEncryptionKeyArn(awsParameters.getEncryptionKeyArn())
                        .build())
                .build());
    }
}