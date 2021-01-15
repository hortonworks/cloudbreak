package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.config.ExperienceServicesConfig;

@ExtendWith({MockitoExtension.class})
class CommonExperienceServiceTest {

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

    private CommonExperienceService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(mockEnvironment.getCrn()).thenReturn(ENV_CRN);
        lenient().when(mockEnvironment.getAccountId()).thenReturn(TENANT);
        lenient().when(mockEnvironment.getName()).thenReturn(TEST_XP_NAME);
        lenient().when(mockCommonExperience.getName()).thenReturn(TEST_XP_NAME);
        lenient().when(mockCommonExperience.getInternalEnvEndpoint()).thenReturn(XP_INTERNAL_ENV_ENDPOINT);
        lenient().when(mockCommonExperience.getHostAddress()).thenReturn(XP_HOST_ADDRESS);
        lenient().when(mockCommonExperience.getPort()).thenReturn(XP_PORT);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenEnvironmentCrnIsNullThenIllegalArgumentExceptionComes() {
        when(mockEnvironment.getCrn()).thenReturn(null);

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> underTest.clusterCountForEnvironment(mockEnvironment));

        assertNotNull(exception);
        assertEquals("Unable to check environment - experience relation, since the " +
                "given environment crn is null or empty!", exception.getMessage());
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenEnvironmentCrnIsEmptyThenIllegalArgumentExceptionComes() {
        when(mockEnvironment.getCrn()).thenReturn("");

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> underTest.clusterCountForEnvironment(mockEnvironment));

        assertNotNull(exception);
        assertEquals("Unable to check environment - experience relation, since the " +
                "given environment crn is null or empty!", exception.getMessage());
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenNoConfiguredExperienceExistsThenNoXpConnectorServiceCallHappens() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(Collections.emptyList());

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        underTest.clusterCountForEnvironment(mockEnvironment);

        verify(mockExperienceConnectorService, never()).getWorkspaceNamesConnectedToEnv(any(), any());
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenNoConfiguredExperienceExistsThenZeroReturns() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(Collections.emptyList());

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        int result = underTest.clusterCountForEnvironment(mockEnvironment);

        assertEquals(0, result);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenHaveConfiguredExperienceButItsNotProperlyFilledThenNoXpConnectorServiceCallHappens() {

        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(false);

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        underTest.clusterCountForEnvironment(mockEnvironment);

        verify(mockExperienceConnectorService, never()).getWorkspaceNamesConnectedToEnv(any(), any());
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenHaveConfiguredExperienceButItsNotProperlyFilledThenZeroReturns() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(false);

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        int result = underTest.clusterCountForEnvironment(mockEnvironment);

        assertEquals(0, result);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenExperienceIsConfiguredThenPathToExperienceShouldBeCombindedProperly() {
        String expectedPath = XP_PROTOCOL + "://" + XP_HOST_ADDRESS + ":" + XP_PORT + XP_INTERNAL_ENV_ENDPOINT;
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(true);

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        underTest.clusterCountForEnvironment(mockEnvironment);

        verify(mockExperienceConnectorService, times(1)).getWorkspaceNamesConnectedToEnv(any(), any());
        verify(mockExperienceConnectorService, times(1)).getWorkspaceNamesConnectedToEnv(expectedPath, ENV_CRN);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenExperienceIsConfiguredButHasNoActiveWorkspaceForEnvThenZeroReturns() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(true);

        when(mockExperienceConnectorService.getWorkspaceNamesConnectedToEnv(any(), eq(ENV_CRN))).thenReturn(Collections.emptySet());

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        int result = underTest.clusterCountForEnvironment(mockEnvironment);

        assertEquals(0, result);
    }

    @Test
    void testHasExistingClusterForEnvironmentWhenExperienceIsConfiguredAndHasActiveWorkspaceForEnvThenCountReturns() {
        when(mockExperienceServicesConfig.getExperiences()).thenReturn(List.of(mockCommonExperience));
        when(mockExperienceValidator.isExperienceFilled(mockCommonExperience)).thenReturn(true);

        when(mockExperienceConnectorService.getWorkspaceNamesConnectedToEnv(any(), eq(ENV_CRN))).thenReturn(Set.of("SomeConnectedXP"));

        underTest = new CommonExperienceService(XP_PROTOCOL, mockExperienceConnectorService, mockExperienceServicesConfig, mockExperienceValidator);

        int result = underTest.clusterCountForEnvironment(mockEnvironment);

        assertEquals(1, result);
    }

}
