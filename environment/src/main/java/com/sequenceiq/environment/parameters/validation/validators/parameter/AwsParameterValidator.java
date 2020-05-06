package com.sequenceiq.environment.parameters.validation.validators.parameter;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.LocationAwareCredential;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.NoSqlTableCreationModeDeterminerService;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@Component
public class AwsParameterValidator implements ParameterValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsParameterValidator.class);

    private static final int PERCENTAGE_MIN = 0;

    private static final int PERCENTAGE_MAX = 100;

    private final NoSqlTableCreationModeDeterminerService noSqlTableCreationModeDeterminerService;

    private final ParametersService parametersService;

    public AwsParameterValidator(NoSqlTableCreationModeDeterminerService noSqlTableCreationModeDeterminerService,
            ParametersService parametersService) {
        this.noSqlTableCreationModeDeterminerService = noSqlTableCreationModeDeterminerService;
        this.parametersService = parametersService;
    }

    @Override
    public ValidationResult validate(EnvironmentDto environmentDto, ParametersDto parametersDto, ValidationResultBuilder validationResultBuilder) {
        LOGGER.debug("ParametersDto: {}", parametersDto);
        AwsParametersDto awsParametersDto = parametersDto.getAwsParametersDto();
        if (Objects.isNull(awsParametersDto)) {
            LOGGER.debug("No aws parameters defined.");
            return validationResultBuilder.build();
        }
        if (StringUtils.isNotBlank(awsParametersDto.getS3GuardTableName())) {
            LOGGER.debug("S3Guard table name defined: {}", awsParametersDto.getS3GuardTableName());
            boolean tableAlreadyAttached = isTableAlreadyAttached(environmentDto, awsParametersDto);
            if (tableAlreadyAttached) {
                validationResultBuilder.error(String.format("S3Guard table '%s' is already attached to another active environment. "
                        + "Please select another unattached table or specify a non-existing name to create it.", awsParametersDto.getS3GuardTableName()));
            } else {
                determineAwsParameters(environmentDto, parametersDto);
            }
        }
        if (awsParametersDto.getFreeIpaSpotPercentage() < PERCENTAGE_MIN || awsParametersDto.getFreeIpaSpotPercentage() > PERCENTAGE_MAX) {
            validationResultBuilder.error(String.format("FreeIpa spot percentage must be between %d and %d.", PERCENTAGE_MIN, PERCENTAGE_MAX));
        }
        return validationResultBuilder.build();
    }

    @Override
    public CloudPlatform getcloudPlatform() {
        return CloudPlatform.AWS;
    }

    private boolean isTableAlreadyAttached(EnvironmentDto environment, AwsParametersDto awsParameters) {
        return parametersService.isS3GuardTableUsed(environment.getAccountId(), environment.getCredential().getCloudPlatform(),
                environment.getLocation().getName(), awsParameters.getS3GuardTableName());
    }

    private void determineAwsParameters(EnvironmentDto environment, ParametersDto parametersDto) {
        LocationAwareCredential locationAwareCredential = getLocationAwareCredential(environment);
        S3GuardTableCreation dynamoDbTableCreation = noSqlTableCreationModeDeterminerService
                .determineCreationMode(locationAwareCredential, parametersDto.getAwsParametersDto().getS3GuardTableName());
        LOGGER.debug("S3Guard table name: {}, creation: {}", parametersDto.getAwsParametersDto().getS3GuardTableName(), dynamoDbTableCreation);
        parametersDto.getAwsParametersDto().setDynamoDbTableCreation(dynamoDbTableCreation);
        parametersService.saveParameters(environment.getId(), parametersDto);
    }

    private LocationAwareCredential getLocationAwareCredential(EnvironmentDto environment) {
        return LocationAwareCredential.builder()
                .withCredential(environment.getCredential())
                .withLocation(environment.getLocation().getName())
                .build();
    }
}
