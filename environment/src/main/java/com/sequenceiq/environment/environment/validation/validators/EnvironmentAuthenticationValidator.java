package com.sequenceiq.environment.environment.validation.validators;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;

@Component
public class EnvironmentAuthenticationValidator {

    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        AuthenticationDto authentication = environmentDto.getAuthentication();
        if (!StringUtils.hasText(authentication.getPublicKeyId())) {
            validate(authentication.getPublicKey(), environmentDto.getCloudPlatform(), environmentDto.getCredential().getGovCloud(), resultBuilder);
        }
        return resultBuilder.build();
    }

    private void validate(String publicKey, String cloudPlatform, Boolean govCloud, ValidationResultBuilder resultBuilder) {
        try {
            // FIPS mode if and only if using AWS GovCloud
            boolean fipsEnabled = govCloud != null && govCloud && CloudPlatform.AWS.name().equals(cloudPlatform);
            PublicKeyReaderUtil.load(publicKey, fipsEnabled);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to parse public key. Detailed message: %s", e.getMessage());
            resultBuilder.error(errorMessage);
        }
    }

}
