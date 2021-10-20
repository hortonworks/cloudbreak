package com.sequenceiq.environment.experience.common;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

public class InvalidCommonExperienceArgumentProvider implements ArgumentsProvider {

    private static final String XP_HOST_ADDRESS = "hostAddress:1234";

    private static final String XP_INTERNAL_ENV_ENDPOINT = "/some/internal/endpoint";

    private static final String VALUE_NOT_SET = "${somexp}";

    //CHECKSTYLE:OFF
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(new CommonExperience()),
                Arguments.of(new CommonExperience("", "", "", "", "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", null, "", "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", "", null, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", null, null, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", "", VALUE_NOT_SET, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", VALUE_NOT_SET, "", "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", "", XP_HOST_ADDRESS, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", null, VALUE_NOT_SET, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", null, XP_HOST_ADDRESS, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", VALUE_NOT_SET, null, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", VALUE_NOT_SET, VALUE_NOT_SET, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", XP_INTERNAL_ENV_ENDPOINT, "", "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", XP_INTERNAL_ENV_ENDPOINT, null, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", VALUE_NOT_SET, XP_HOST_ADDRESS, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false)),
                Arguments.of(new CommonExperience("", "", XP_INTERNAL_ENV_ENDPOINT, VALUE_NOT_SET, "str", "somePolicyPath", "envPort", "baseAddress", "policyPort", false))
                );
    }
    //CHECKSTYLE:ON

}
