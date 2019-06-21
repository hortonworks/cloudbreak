package com.sequenceiq.environment.environment.dto;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;

@Component
public class AuthenticationDtoConverter {

    public EnvironmentAuthentication dtoToAuthentication(AuthenticationDto authenticationDto) {
        EnvironmentAuthentication environmentAuthentication = new EnvironmentAuthentication();
        environmentAuthentication.setLoginUserName(authenticationDto.getLoginUserName());
        environmentAuthentication.setPublicKey(authenticationDto.getPublicKey());
        environmentAuthentication.setPublicKeyId(authenticationDto.getPublicKeyId());
        return environmentAuthentication;
    }

    public AuthenticationDto authenticationToDto(EnvironmentAuthentication authentication) {
        AuthenticationDto authenticationDto = AuthenticationDto.builder()
                .withLoginUserName(authentication.getLoginUserName())
                .withPublicKey(authentication.getPublicKey())
                .withPublicKeyId(authentication.getPublicKeyId())
                .build();
        return authenticationDto;
    }
}
