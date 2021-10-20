package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

class CommonExperienceValidatorTest {

    private static final String XP_HOST_ADDRESS = "somePrefixValue:somePortValue";

    private static final String XP_ENV_ENDPOINT = "someInfixValue";

    private CommonExperienceValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new CommonExperienceValidator();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidCommonExperienceArgumentProvider.class)
    void testIsExperienceFilledWhenTheInputFieldIsInvalidThenFalseReturns(CommonExperience testData) {
        boolean result = underTest.isExperienceFilled(testData);

        assertFalse(result);
    }

    @Test
    void testIsExperienceFilledWhenAllTheFieldsAreValidThenTrueReturns() {
        boolean result = underTest.isExperienceFilled(new CommonExperience("awesomeXP", "awesomeDescription", XP_ENV_ENDPOINT, XP_HOST_ADDRESS,
                "somePolicyPath", "somePolicyPath", "envPort", "baseAddress", "policyPort", false));

        assertTrue(result);
    }

}
