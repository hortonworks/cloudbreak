package com.sequenceiq.environment.environment.dto;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.validation.validators.PublicKeyValidator;

@Component
public class AuthenticationDtoConverter {

    private final PublicKeyValidator publicKeyValidator;

    public AuthenticationDtoConverter(PublicKeyValidator publicKeyValidator) {
        this.publicKeyValidator = publicKeyValidator;
    }

    public EnvironmentAuthentication dtoToAuthentication(AuthenticationDto authenticationDto) {
        EnvironmentAuthentication environmentAuthentication = new EnvironmentAuthentication();
        if (isValidSshKey(authenticationDto.getPublicKey())) {
            List<String> parts = Arrays.asList(StringUtils.split(authenticationDto.getPublicKey(), " "));
            environmentAuthentication.setPublicKey(String.format("%s %s %s", parts.get(0), parts.get(1), authenticationDto.getLoginUserName()));
        }
        environmentAuthentication.setLoginUserName(authenticationDto.getLoginUserName());
        environmentAuthentication.setPublicKeyId(authenticationDto.getPublicKeyId());
        environmentAuthentication.setManagedKey(authenticationDto.isManagedKey());
        return environmentAuthentication;
    }

    public AuthenticationDto authenticationToDto(EnvironmentAuthentication authentication) {
        AuthenticationDto authenticationDto = AuthenticationDto.builder()
                .withLoginUserName(authentication.getLoginUserName())
                .withPublicKey(authentication.getPublicKey())
                .withPublicKeyId(authentication.getPublicKeyId())
                .withManagedKey(authentication.isManagedKey())
                .build();
        return authenticationDto;
    }

    public EnvironmentAuthentication dtoToSshUpdatedAuthentication(AuthenticationDto authenticationDto) {
        EnvironmentAuthentication environmentAuthentication = new EnvironmentAuthentication();
        if (isValidSshKey(authenticationDto.getPublicKey())) {
            List<String> parts = Arrays.asList(StringUtils.split(authenticationDto.getPublicKey(), " "));
            environmentAuthentication.setPublicKey(String.format("%s %s %s", parts.get(0), parts.get(1), authenticationDto.getLoginUserName()));
        }
        return environmentAuthentication;
    }

    private boolean isValidSshKey(String publicKey) {
        boolean ret = false;
        if (StringUtils.isNotEmpty(publicKey)) {
            ValidationResult validationResult = publicKeyValidator.validatePublicKey(publicKey);
            ret = !validationResult.hasError();
        }
        return ret;
    }
}
