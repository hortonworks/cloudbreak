package com.sequenceiq.environment.environment.experience.service;

import com.sequenceiq.environment.environment.experience.resolve.Experience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceValidatorTest {

    private static final String TEST_PREFIX = "someprefix";

    private static final String TEST_PORT = "1111";

    private static final String TEST_INFIX = "someinfix";

    private ExperienceValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExperienceValidator();
    }

    @Test
    void testIsExperienceFilledWhenGivenExperienceIsNullThenFalseReturns() {
        boolean result = underTest.isExperienceFilled(null);

        assertFalse(result);
    }

    @Test
    void testIsExperienceFilledWhenOneOfTheGivenFieldsHasFilledWithEmptyThenFalseReturn() {
        Experience xp = new Experience();
        xp.setPathPrefix(TEST_PREFIX);
        xp.setPathInfix(TEST_INFIX);
        xp.setPort("");

        boolean result = underTest.isExperienceFilled(xp);

        assertFalse(result);
    }

    @Test
    void testIsExperienceFilledWhenOneOfTheGivenFieldsIsNullThenFalseReturn() {
        Experience xp = new Experience();
        xp.setPathPrefix(TEST_PREFIX);
        xp.setPathInfix(null);
        xp.setPort(TEST_PORT);

        boolean result = underTest.isExperienceFilled(xp);

        assertFalse(result);
    }

    @Test
    void testIsExperienceFilledWhenAllFieldsAreNullThenFalseReturn() {
        Experience xp = new Experience();
        xp.setPathPrefix(null);
        xp.setPathInfix(null);
        xp.setPort(null);

        boolean result = underTest.isExperienceFilled(xp);

        assertFalse(result);
    }

    @Test
    void testIsExperienceFilledWhenAllFieldsAreEmptyThenFalseReturn() {
        Experience xp = new Experience();
        xp.setPathPrefix("");
        xp.setPathInfix("");
        xp.setPort("");

        boolean result = underTest.isExperienceFilled(xp);

        assertFalse(result);
    }

    @Test
    void testIsExperienceFilledWhenOneOfTheGivenFieldsHasAnUnfilledEnvVariableExpressionThenFalseReturn() {
        Experience xp = new Experience();
        xp.setPathPrefix("${SOME_PATH_PREFIX}");
        xp.setPathInfix(TEST_INFIX);
        xp.setPort(TEST_PORT);

        boolean result = underTest.isExperienceFilled(xp);

        assertFalse(result);
    }

    @Test
    void testIsExperienceFilledWhenAllFieldsHasFilledWithSomeValueWhichIsNotEmptyNullOrEnvVariablePatternThenTrueReturns() {
        Experience xp = new Experience();
        xp.setPathPrefix(TEST_PREFIX);
        xp.setPathInfix(TEST_INFIX);
        xp.setPort(TEST_PORT);

        boolean result = underTest.isExperienceFilled(xp);

        assertTrue(result);
    }

}