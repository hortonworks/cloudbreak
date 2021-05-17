package com.sequenceiq.environment.environment.validation.validators;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;

@Component
public class EnvironmentAuthenticationValidator {

    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        AuthenticationDto authentication = environmentDto.getAuthentication();
        if (!StringUtils.hasText(authentication.getPublicKeyId())) {
            validate(authentication.getPublicKey(), resultBuilder);
        }
        return resultBuilder.build();
    }

    private void validate(String publicKey, ValidationResult.ValidationResultBuilder resultBuilder) {
        try {
            PublicKeyReaderUtil.load(publicKey);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to parse public key. Detailed message: %s", e.getMessage());
            resultBuilder.error(errorMessage);
        }
    }
}
