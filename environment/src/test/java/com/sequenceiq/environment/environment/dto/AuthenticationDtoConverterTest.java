package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;

class AuthenticationDtoConverterTest {

    public static final String LOGIN = "login";

    public static final String PUBLIC_KEY = "key";

    public static final String PUBLIC_KEY_ID = "id";

    private AuthenticationDtoConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new AuthenticationDtoConverter();
    }

    @Test
    void dtoToAuthentication() {
        AuthenticationDto dto = AuthenticationDto.builder()
                .withLoginUserName(LOGIN)
                .withPublicKey(PUBLIC_KEY)
                .withPublicKeyId(PUBLIC_KEY_ID)
                .withManagedKey(true)
                .build();

        EnvironmentAuthentication result = underTest.dtoToAuthentication(dto);

        assertThat(result)
                .matches(m -> Objects.equals(m.getLoginUserName(), dto.getLoginUserName()))
                .matches(m -> Objects.equals(m.getPublicKey(), dto.getPublicKey()))
                .matches(m -> Objects.equals(m.getPublicKeyId(), dto.getPublicKeyId()))
                .matches(m -> Objects.equals(m.isManagedKey(), dto.isManagedKey()));
    }

    @Test
    void authenticationToDto() {
        EnvironmentAuthentication environment = new EnvironmentAuthentication();
        environment.setId(123L);
        environment.setLoginUserName(LOGIN);
        environment.setPublicKey(PUBLIC_KEY);
        environment.setPublicKeyId(PUBLIC_KEY_ID);
        environment.setManagedKey(true);

        AuthenticationDto result = underTest.authenticationToDto(environment);

        assertThat(result)
                .matches(m -> Objects.equals(m.getLoginUserName(), environment.getLoginUserName()))
                .matches(m -> Objects.equals(m.getPublicKey(), environment.getPublicKey()))
                .matches(m -> Objects.equals(m.getPublicKeyId(), environment.getPublicKeyId()))
                .matches(m -> Objects.equals(m.isManagedKey(), environment.isManagedKey()));
    }
}
