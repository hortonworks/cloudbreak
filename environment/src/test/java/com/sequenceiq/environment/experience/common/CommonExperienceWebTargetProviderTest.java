package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CommonExperienceWebTargetProviderTest {

    private static final String MISSING_COMPONENT_TO_REPLACE_EXCEPTION_MSG = "Component what should be replaced in experience path must not be empty or null.";

    private static final String INVALID_XP_BASE_PATH_GIVEN_MSG = "Experience base path should not be null!";

    private static final String TEST_COMPONENT_TO_REPLACE_IN_PATH = "crn";

    private static final String TEST_ENV_CRN = "someEnvCrn";

    private static final int ONCE = 1;

    @Mock
    private Client mockClient;

    private CommonExperienceWebTargetProvider underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new CommonExperienceWebTargetProvider(TEST_COMPONENT_TO_REPLACE_IN_PATH, mockClient);
    }

    @Test
    void testInitializationWhenComponentToReplaceInPathIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> new CommonExperienceWebTargetProvider(null, mockClient));

        assertNotNull(expectedException);
        assertEquals(MISSING_COMPONENT_TO_REPLACE_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testInitializationWhenComponentToReplaceInPathIsEmptyThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> new CommonExperienceWebTargetProvider("", mockClient));

        assertNotNull(expectedException);
        assertEquals(MISSING_COMPONENT_TO_REPLACE_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testCreateWebTargetBasedOnInputsWhenExperienceBasePathIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> underTest.createWebTargetBasedOnInputs(null, TEST_ENV_CRN));

        assertNotNull(expectedException);
        assertEquals(INVALID_XP_BASE_PATH_GIVEN_MSG, expectedException.getMessage());
    }

    @Test
    void testCreateWebTargetBasedOnInputsWhenAllDataHasBeenGivenThenClientTargetCallShouldHappenWithTheExpectedlyReplacedContent() {
        String xpBasePathBase = "someBasePath/";
        String xpBasePathExtended = xpBasePathBase + TEST_COMPONENT_TO_REPLACE_IN_PATH;
        String expectedTargetCreationContent = xpBasePathBase + TEST_ENV_CRN;

        WebTarget expectedWebTarget = mock(WebTarget.class);

        when(mockClient.target(expectedTargetCreationContent)).thenReturn(expectedWebTarget);

        WebTarget resultWebTarget = underTest.createWebTargetBasedOnInputs(xpBasePathExtended, TEST_ENV_CRN);

        assertEquals(expectedWebTarget, resultWebTarget);

        verify(mockClient, times(ONCE)).target(anyString());
        verify(mockClient, times(ONCE)).target(expectedTargetCreationContent);
    }

}