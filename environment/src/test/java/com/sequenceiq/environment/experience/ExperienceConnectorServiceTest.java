package com.sequenceiq.environment.experience;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;

@ExtendWith(MockitoExtension.class)
class ExperienceConnectorServiceTest {

    private static final boolean SCAN_ENABLED = true;

    private static final String TENANT = "someTenantValue";

    @Mock
    private Experience mockExperience;

    @Mock
    private EnvironmentExperienceDto mockDto;

    @Mock
    private EntitlementService entitlementServiceMock;

    private ExperienceConnectorService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExperienceConnectorService(List.of(mockExperience), entitlementServiceMock);
    }

    @Test
    void testWhenScanIsNotEnabledThenNoExperienceCallHappensAndZeroShouldReturn() {
        when(entitlementServiceMock.isExperienceDeletionEnabled(any())).thenReturn(false);
        int result = underTest.getConnectedExperienceCount(mockDto);

        Assertions.assertEquals(0, result);
        verify(mockExperience, never()).clusterCountForEnvironment(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testWhenNoExperienceHasConfiguredThenZeroShouldReturn() {
        when(entitlementServiceMock.isExperienceDeletionEnabled(any())).thenReturn(false);
        int result = underTest.getConnectedExperienceCount(mockDto);

        Assertions.assertEquals(0, result);
    }

}
