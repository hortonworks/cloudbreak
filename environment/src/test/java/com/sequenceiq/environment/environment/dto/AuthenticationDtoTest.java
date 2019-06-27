package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.environment.dto.AuthenticationDto.Builder;
import com.sequenceiq.environment.testing.BuilderFieldValidator;

class AuthenticationDtoTest {

    private static final String LOGIN = "login";

    private static final String PUBLIC_KEY = "key";

    private static final String PUBLIC_KEY_ID = "id";

    private final BuilderFieldValidator builderFieldValidator = new BuilderFieldValidator();

    @Test
    void builderCreatesFilledPojo() {
        builderFieldValidator.assertBuilderFields(AuthenticationDto.class, Builder.class);

        AuthenticationDto result = AuthenticationDto.builder()
                .withLoginUserName(LOGIN)
                .withPublicKey(PUBLIC_KEY)
                .withPublicKeyId(PUBLIC_KEY_ID)
                .build();

        assertThat(result)
                .matches(m -> Objects.equals(m.getLoginUserName(), LOGIN))
                .matches(m -> Objects.equals(m.getPublicKey(), PUBLIC_KEY))
                .matches(m -> Objects.equals(m.getPublicKeyId(), PUBLIC_KEY_ID));
    }
}
