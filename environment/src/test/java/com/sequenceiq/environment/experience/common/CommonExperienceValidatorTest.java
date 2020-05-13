package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CommonExperienceValidatorTest {

    private static final String XP_PORT = "somePortValue";

    private static final String XP_HOST_ADDRESS = "somePrefixValue";

    private static final String XP_ENV_ENDPOINT = "someInfixValue";

    @Mock
    private CommonExperience commonExperience;

    private CommonExperienceValidator underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(commonExperience.getPort()).thenReturn(XP_PORT);
        when(commonExperience.getInternalEnvEndpoint()).thenReturn(XP_ENV_ENDPOINT);
        when(commonExperience.getHostAddress()).thenReturn(XP_HOST_ADDRESS);

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
        boolean result = underTest.isExperienceFilled(new CommonExperience("awesomeXP", XP_HOST_ADDRESS, XP_ENV_ENDPOINT, XP_PORT));

        assertTrue(result);
    }

}