package com.sequenceiq.cloudbreak.init.blueprint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.gov.CommonGovService;

@ExtendWith(MockitoExtension.class)
class GovCloudExculsionFilterTest {

    @Mock
    private CommonGovService commonGovService;

    @Mock
    private ExclusionListProperties exclusionListProperties;

    @InjectMocks
    private GovCloudExclusionFilter govCloudExclusionFilter;

    @Test
    public void testShouldAddBlueprintWhenStackVersionCompatibleAndNotExcluded() {
        String stackVersion = "1.0";
        String blueprintName = "exampleBlueprint";

        when(commonGovService.govCloudCompatibleVersion(stackVersion)).thenReturn(true);
        when(exclusionListProperties.isBlueprintExcluded(stackVersion, blueprintName)).thenReturn(false);

        assertTrue(govCloudExclusionFilter.shouldAddBlueprint(stackVersion, blueprintName));
    }

    @Test
    public void testShouldAddBlueprintWhenStackVersionNotCompatible() {
        String stackVersion = "2.0";
        String blueprintName = "exampleBlueprint";

        when(commonGovService.govCloudCompatibleVersion(stackVersion)).thenReturn(false);

        assertFalse(govCloudExclusionFilter.shouldAddBlueprint(stackVersion, blueprintName));
    }

    @Test
    public void testShouldAddBlueprintWhenBlueprintExcluded() {
        String stackVersion = "1.0";
        String blueprintName = "excludedBlueprint";

        when(commonGovService.govCloudCompatibleVersion(stackVersion)).thenReturn(true);
        when(exclusionListProperties.isBlueprintExcluded(stackVersion, blueprintName)).thenReturn(true);

        assertFalse(govCloudExclusionFilter.shouldAddBlueprint(stackVersion, blueprintName));
    }
}