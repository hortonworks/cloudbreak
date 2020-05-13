package com.sequenceiq.environment.experience;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;

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
        MockitoAnnotations.initMocks(this);
        underTest = new ExperienceConnectorService(List.of(mockExperience), entitlementServiceMock);
    }

    @Test
    void testWhenScanIsNotEnabledThenNoExperienceCallHappensAndZeroShouldReturn() {
        when(entitlementServiceMock.isExperienceDeletionEnabled(any())).thenReturn(false);
        ExperienceConnectorService underTest = new ExperienceConnectorService(List.of(mockExperience), entitlementServiceMock);
        long result = underTest.getConnectedExperienceQuantity(mockDto);

        Assert.assertEquals(0L, result);
        verify(mockExperience, never()).hasExistingClusterForEnvironment(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testWhenNoExperienceHasConfiguredThenZeroShouldReturn() {
        when(entitlementServiceMock.isExperienceDeletionEnabled(any())).thenReturn(false);
        ExperienceConnectorService underTest = new ExperienceConnectorService(Collections.emptyList(), entitlementServiceMock);
        long result = underTest.getConnectedExperienceQuantity(mockDto);

        Assert.assertEquals(0L, result);
    }

}