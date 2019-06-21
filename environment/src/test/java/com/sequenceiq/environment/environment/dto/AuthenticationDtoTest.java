package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.environment.dto.AuthenticationDto.Builder;

class AuthenticationDtoTest {

    public static final String LOGIN = "login";

    public static final String PUBLIC_KEY = "key";

    public static final String PUBLIC_KEY_ID = "id";

    @Test
    void builderCreatesFilledPojo() {
        Set<String> builderFields = Arrays.stream(Builder.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
        Set<String> pojoFields = Arrays.stream(AuthenticationDto.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
        Set<String> diff = new HashSet<>(pojoFields);
        diff.removeAll(builderFields);

        assertThat(builderFields.size())
                .withFailMessage("Builder does not propagate fields: [%s]", String.join(", ", diff))
                .isEqualTo(pojoFields.size());

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
