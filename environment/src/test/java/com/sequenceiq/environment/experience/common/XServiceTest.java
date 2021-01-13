package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.config.ExperienceServicesConfig;

class XServiceTest {

    private static final String XP_PROTOCOL = "https";

    private static final String TENANT = "someTenantValue";

    private static final String ENV_CRN = "someEnvCrnValue";

    private static final String XP_PORT = "9999";

    private static final String XP_HOST_ADDRESS = "127.0.0.1";

    private static final String XP_INTERNAL_ENV_ENDPOINT = "/somexp/api/v3/cp-internal/environment/{crn}";

    private static final String TEST_XP_NAME = "AWESOME_XP";

    @Mock
    private CommonExperienceConnectorService mockExperienceConnectorService;

    @Mock
    private ExperienceServicesConfig mockExperienceServicesConfig;

    @Mock
    private CommonExperienceValidator mockExperienceValidator;

    @Mock
    private EnvironmentExperienceDto mockEnvironment;

   @Mock
   private CommonExperience mockCommonExperience;

    private XService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockEnvironment.getCrn()).thenReturn(ENV_CRN);
        when(mockEnvironment.getAccountId()).thenReturn(TENANT);
        when(mockEnvironment.getName()).thenReturn(TEST_XP_NAME);
        when(mockCommonExperience.getName()).thenReturn(TEST_XP_NAME);
        when(mockCommonExperience.getInternalEnvEndpoint()).thenReturn(XP_INTERNAL_ENV_ENDPOINT);
        when(mockCommonExperience.getHostAddress()).thenReturn(XP_HOST_ADDRESS);
        when(mockCommonExperience.getPort()).thenReturn(XP_PORT);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenEnvironmentCrnIsNullThenIllegalArgumentExceptionComes() {
        when(mockEnvironment.getCrn()).thenReturn(null);

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> underTest.hasExistingClusterForEnvironment(mockEnvironment));

        assertNotNull(exception);
        assertEquals("Unable to check environment - experience relation, since the " +
                "given environment crn is null or empty!", exception.getMessage());
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenEnvironmentCrnIsEmptyThenIllegalArgumentExceptionComes() {
        when(mockEnvironment.getCrn()).thenReturn("");

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> underTest.hasExistingClusterForEnvironment(mockEnvironment));

        assertNotNull(exception);
        assertEquals("Unable to check environment - experience relation, since the " +
                "given environment crn is null or empty!", exception.getMessage());
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenNoConfiguredExperienceExistsThenNoXpConnectorServiceCallHappens() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(Collections.emptyList());

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        underTest.hasExistingClusterForEnvironment(mockEnvironment);

        verify(mockExperienceConnectorService, never()).getWorkspaceNamesConnectedToEnv(any(), any());
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenNoConfiguredExperienceExistsThenFalseReturns() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(Collections.emptyList());

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        boolean result = underTest.hasExistingClusterForEnvironment(mockEnvironment);

        assertFalse(result);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenHaveConfiguredExperienceButItsNotProperlyFilledThenNoXpConnectorServiceCallHappens() {

        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(false);

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        underTest.hasExistingClusterForEnvironment(mockEnvironment);

        verify(mockExperienceConnectorService, never()).getWorkspaceNamesConnectedToEnv(any(), any());
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenHaveConfiguredExperienceButItsNotProperlyFilledThenFalseReturns() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(false);

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        boolean result = underTest.hasExistingClusterForEnvironment(mockEnvironment);

        assertFalse(result);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenExperienceIsConfiguredThenPathToExperienceShouldBeCombindedProperly() {
        String expectedPath = XP_PROTOCOL + "://" + XP_HOST_ADDRESS + ":" + XP_PORT + XP_INTERNAL_ENV_ENDPOINT;
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(true);

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        underTest.hasExistingClusterForEnvironment(mockEnvironment);

        verify(mockExperienceConnectorService, times(1)).getWorkspaceNamesConnectedToEnv(any(), any());
        verify(mockExperienceConnectorService, times(1)).getWorkspaceNamesConnectedToEnv(expectedPath, ENV_CRN);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenExperienceIsConfiguredButHasNoActiveWorkspaceForEnvThenFalseReturns() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(true);

        when(mockExperienceConnectorService.getWorkspaceNamesConnectedToEnv(any(), eq(ENV_CRN))).thenReturn(Collections.emptySet());

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        boolean result = underTest.hasExistingClusterForEnvironment(mockEnvironment);

        assertFalse(result);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenExperienceIsConfiguredAndHasActiveWorkspaceForEnvThentrueReturns() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(true);

        when(mockExperienceConnectorService.getWorkspaceNamesConnectedToEnv(any(), eq(ENV_CRN))).thenReturn(Set.of("SomeConnectedXP"));

        underTest = new XService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        boolean result = underTest.hasExistingClusterForEnvironment(mockEnvironment);

        assertTrue(result);
    }

}
