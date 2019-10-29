package com.sequenceiq.environment.parameters.validation.validators;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.LocationAwareCredential;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.NoSqlTableCreationModeDeterminerService;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@Component
public class AwsParameterValidator {

    private final NoSqlTableCreationModeDeterminerService noSqlTableCreationModeDeterminerService;

    private final ParametersService parametersService;

    public AwsParameterValidator(NoSqlTableCreationModeDeterminerService noSqlTableCreationModeDeterminerService,
            ParametersService parametersService) {
        this.noSqlTableCreationModeDeterminerService = noSqlTableCreationModeDeterminerService;
        this.parametersService = parametersService;
    }

    public ValidationResult validateAndDetermineAwsParameters(EnvironmentDto environmentDto, AwsParametersDto awsParameters) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        if (StringUtils.isNotBlank(awsParameters.getS3GuardTableName())) {
            boolean tableAlreadyAttached = isTableAlreadyAttached(environmentDto, awsParameters);
            if (tableAlreadyAttached) {
                validationResultBuilder.error(String.format("S3Guard table '%s' is already attached to another active environment. "
                        + "Please select another unattached table or specify a non-existing name to create it.", awsParameters.getS3GuardTableName()));
            } else {
                determineAwsParameters(environmentDto, awsParameters);
            }
        }
        return validationResultBuilder.build();
    }

    private boolean isTableAlreadyAttached(EnvironmentDto environment, AwsParametersDto awsParameters) {
        return parametersService.isS3GuardTableUsed(environment.getAccountId(), environment.getCredential().getCloudPlatform(),
                environment.getLocation().getName(), awsParameters.getS3GuardTableName());
    }

    private void determineAwsParameters(EnvironmentDto environment, AwsParametersDto awsParameters) {
        LocationAwareCredential locationAwareCredential = getLocationAwareCredential(environment);
        S3GuardTableCreation dynamoDbTableCreation = noSqlTableCreationModeDeterminerService
                .determineCreationMode(locationAwareCredential, awsParameters.getS3GuardTableName());
        awsParameters.setDynamoDbTableCreation(dynamoDbTableCreation);
    }

    private LocationAwareCredential getLocationAwareCredential(EnvironmentDto environment) {
        return LocationAwareCredential.builder()
                .withCredential(environment.getCredential())
                .withLocation(environment.getLocation().getName())
                .build();
    }
}
