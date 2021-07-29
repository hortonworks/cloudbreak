package com.sequenceiq.environment.parameters.validation.validators.parameter;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

@Component
public class GcpParameterValidator implements ParameterValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpParameterValidator.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto, ParametersDto parametersDto,
            ValidationResultBuilder validationResultBuilder) {

        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        LOGGER.debug("ParametersDto: {}", parametersDto);
        GcpParametersDto gcpParametersDto = parametersDto.getGcpParametersDto();
        if (Objects.isNull(gcpParametersDto)) {
            return validationResultBuilder.build();
        }

        ValidationResult validationResult;
        GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto = gcpParametersDto.getGcpResourceEncryptionParametersDto();
        if (gcpResourceEncryptionParametersDto != null) {
            validationResult = validateGcpEncryptionParameters(validationResultBuilder, gcpParametersDto,
                    environmentDto.getAccountId());
            if (validationResult.hasError()) {
                return validationResult;
            }
        }
        //TODO:: do we need to validate entitlement
        return validationResultBuilder.build();
    }

    private ValidationResult validateGcpEncryptionParameters(ValidationResultBuilder validationResultBuilder,
            GcpParametersDto gcpParametersDto, String accountId) {

        GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto = gcpParametersDto.getGcpResourceEncryptionParametersDto();
        String encryptionKey = gcpResourceEncryptionParametersDto.getEncryptionKey();

        if (encryptionKey != null && !entitlementService.isGcpDiskEncryptionWithCMEKEnabled(accountId)) {
            LOGGER.info("Invalid request, CDP_CB_GCP_DISK_ENCRYPTION_WITH_CMEK entitlement turned off for account {}", accountId);
            return validationResultBuilder.error(
                    "You specified encryptionKey to encrypt resources with CMEK, "
                            + "but that feature is currently disabled."
                            + "Get 'CDP_CB_GCP_DISK_ENCRYPTION_WITH_CMEK' enabled for your account to use resource encryption with CMEK.").
                    build();
        }

        LOGGER.debug("Validation of encryption parameters is successful.");
        return validationResultBuilder.build();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }
}