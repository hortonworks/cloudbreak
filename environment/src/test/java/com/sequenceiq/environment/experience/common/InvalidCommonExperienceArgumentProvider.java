package com.sequenceiq.environment.experience.common;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

public class InvalidCommonExperienceArgumentProvider implements ArgumentsProvider {

    private static final String XP_PORT = "1234";

    private static final String XP_HOST_ADDRESS = "/some/xp/host/address";

    private static final String XP_INTERNAL_ENV_ENDPOINT = "/some/internal/endpoint";

    private static final String VALUE_NOT_SET = "${somexp}";

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                Arguments.of(new CommonExperience()),
                Arguments.of(new CommonExperience("", "", "", "")),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, XP_INTERNAL_ENV_ENDPOINT, "")),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, XP_INTERNAL_ENV_ENDPOINT, null)),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, "", XP_PORT)),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, null, XP_PORT)),
                Arguments.of(new CommonExperience("", "", XP_INTERNAL_ENV_ENDPOINT, XP_PORT)),
                Arguments.of(new CommonExperience("", null, XP_INTERNAL_ENV_ENDPOINT, XP_PORT)),
                Arguments.of(new CommonExperience("", null, "", XP_PORT)),
                Arguments.of(new CommonExperience("", null, null, XP_PORT)),
                Arguments.of(new CommonExperience("", "", "", XP_PORT)),
                Arguments.of(new CommonExperience("", "", null, XP_PORT)),
                Arguments.of(new CommonExperience("", null, "", XP_PORT)),
                Arguments.of(new CommonExperience("", null, XP_INTERNAL_ENV_ENDPOINT, null)),
                Arguments.of(new CommonExperience("", null, XP_INTERNAL_ENV_ENDPOINT, "")),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, null, "")),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, "", null)),
                Arguments.of(new CommonExperience("", "", XP_INTERNAL_ENV_ENDPOINT, null)),
                Arguments.of(new CommonExperience("", null, XP_INTERNAL_ENV_ENDPOINT, null)),
                Arguments.of(new CommonExperience("", "", XP_INTERNAL_ENV_ENDPOINT, "")),
                Arguments.of(new CommonExperience("", null, XP_INTERNAL_ENV_ENDPOINT, "")),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, XP_INTERNAL_ENV_ENDPOINT, VALUE_NOT_SET)),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, VALUE_NOT_SET, XP_PORT)),
                Arguments.of(new CommonExperience("", VALUE_NOT_SET, XP_INTERNAL_ENV_ENDPOINT, VALUE_NOT_SET)),
                Arguments.of(new CommonExperience("", XP_HOST_ADDRESS, VALUE_NOT_SET, VALUE_NOT_SET)),
                Arguments.of(new CommonExperience("", VALUE_NOT_SET, XP_INTERNAL_ENV_ENDPOINT, VALUE_NOT_SET)),
                Arguments.of(new CommonExperience("", VALUE_NOT_SET, VALUE_NOT_SET, XP_PORT))
        );
    }

}
