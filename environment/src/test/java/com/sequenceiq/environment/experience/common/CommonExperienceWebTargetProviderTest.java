package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.experience.config.ExperiencePathConfig;

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
        underTest = new CommonExperienceWebTargetProvider(new ExperiencePathConfig(Map.of("envCrn", TEST_COMPONENT_TO_REPLACE_IN_PATH)), mockClient);
    }

    @Test
    void testCreateWebTargetForPolicyFetchWhenBasePathIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> underTest.createWebTargetForPolicyFetch(null, "someCloudProvider"));

        assertNotNull(expectedException);
        assertEquals(INVALID_XP_BASE_PATH_GIVEN_MSG, expectedException.getMessage());
    }

    @Test
    void testCreateWebTargetForClusterFetchWhenBasePathIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> underTest.createWebTargetForClusterFetch(null, "someCloudProvider"));

        assertNotNull(expectedException);
        assertEquals(INVALID_XP_BASE_PATH_GIVEN_MSG, expectedException.getMessage());
    }

    @Test
    void testCreateWebTargetBasedOnInputsWhenExperienceBasePathIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> underTest.createWebTargetForClusterFetch(null, TEST_ENV_CRN));

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

        WebTarget resultWebTarget = underTest.createWebTargetForClusterFetch(xpBasePathExtended, TEST_ENV_CRN);

        assertEquals(expectedWebTarget, resultWebTarget);

        verify(mockClient, times(ONCE)).target(anyString());
        verify(mockClient, times(ONCE)).target(expectedTargetCreationContent);
    }

}