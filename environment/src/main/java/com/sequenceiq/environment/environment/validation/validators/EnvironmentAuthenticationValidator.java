package com.sequenceiq.environment.environment.validation.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentAuthenticationValidator.class);

    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        AuthenticationDto authentication = environmentDto.getAuthentication();
        if (!StringUtils.hasText(authentication.getPublicKeyId())) {
            Boolean govCloud = environmentDto.getCredential() == null ? null : environmentDto.getCredential().getGovCloud();
            validate(authentication.getPublicKey(), environmentDto.getCloudPlatform(), govCloud, resultBuilder);
        }
        return resultBuilder.build();
    }

    private void validate(String publicKey, String cloudPlatform, Boolean govCloud, ValidationResultBuilder resultBuilder) {
        try {
            // FIPS mode if and only if using AWS GovCloud
            boolean fipsEnabled = Boolean.TRUE.equals(govCloud) && CloudPlatform.AWS.name().equals(cloudPlatform);
            PublicKeyReaderUtil.load(publicKey, fipsEnabled);
        } catch (PublicKeyReaderUtil.PublicKeyParseException e) {
            if (e.getErrorCode() == PublicKeyReaderUtil.PublicKeyParseException.ErrorCode.RSA_KEY_TOO_SHORT) {
                // only RHEL 9 images reject short RSA keys and the target OS is not known here. Environment creation rejects them when
                // FreeIPA will run on RHEL 9 and key updates reject them in EnvironmentValidatorService, while FreeIPA/cluster creation,
                // image changes and upgrades validate them against the selected image
                LOGGER.info("Ignoring too short RSA public key on environment level: {}", e.getMessage());
            } else {
                resultBuilder.error(String.format("Failed to parse public key. Detailed message: %s", e.getMessage()));
            }
        } catch (Exception e) {
            String errorMessage = String.format("Failed to parse public key. Detailed message: %s", e.getMessage());
            resultBuilder.error(errorMessage);
        }
    }

}
