package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@ExtendWith(MockitoExtension.class)
class BlueprintBasedUpgradeValidatorTest {

    private static final String ACCOUNT_ID = "account-id";

    @InjectMocks
    private BlueprintBasedUpgradeValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private BlueprintUpgradeOptionValidator blueprintUpgradeOptionValidator;

    @Test
    public void testIsValidBlueprintShouldReturnValidationResultWhenTheStackTypeIsWorkload() {
        BlueprintValidationResult expected = new BlueprintValidationResult(false, "Invalid blueprint");
        ImageFilterParams params = createImageFilterParams("7.2.14 - Data Engineering: Apache Spark, Apache Hive, Apache Oozie", StackType.WORKLOAD);
        when(blueprintUpgradeOptionValidator.isValidBlueprint(params.getBlueprint(), true, true, true)).thenReturn(expected);

        BlueprintValidationResult actual = underTest.isValidBlueprint(params, ACCOUNT_ID);

        assertEquals(expected, actual);
        verify(blueprintUpgradeOptionValidator).isValidBlueprint(params.getBlueprint(), true, true, true);
        verifyNoInteractions(entitlementService);
    }

    @Test
    public void testIsValidBlueprintShouldReturnInvalidResultWhenTheStackTypeIsDataLakeMediumDutyAndEntitlementIsDisabled() {
        ImageFilterParams params = createImageFilterParams("7.2.14 - SDX Medium Duty: Apache Hive Metastore, Apache Ranger, Apache Atlas", StackType.DATALAKE);
        when(entitlementService.haUpgradeEnabled(ACCOUNT_ID)).thenReturn(false);

        BlueprintValidationResult actual = underTest.isValidBlueprint(params, ACCOUNT_ID);

        assertFalse(actual.isValid());
        assertEquals("The upgrade is not allowed for this template.", actual.getReason());
        verify(entitlementService).haUpgradeEnabled(ACCOUNT_ID);
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnValidResultWhenTheStackTypeIsDataLakeMediumDutyAndEntitlementIsEnabled() {
        ImageFilterParams params = createImageFilterParams("7.2.14 - SDX Medium Duty: Apache Hive Metastore, Apache Ranger, Apache Atlas", StackType.DATALAKE);
        when(entitlementService.haUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);

        BlueprintValidationResult actual = underTest.isValidBlueprint(params, ACCOUNT_ID);

        assertTrue(actual.isValid());
        verify(entitlementService).haUpgradeEnabled(ACCOUNT_ID);
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnValidResultWhenTheStackTypeIsDataLakeLightDuty() {
        ImageFilterParams params = createImageFilterParams("7.2.14 - SDX Light Duty: Apache Hive Metastore, Apache Ranger, Apache Atlas", StackType.DATALAKE);

        BlueprintValidationResult actual = underTest.isValidBlueprint(params, ACCOUNT_ID);

        assertTrue(actual.isValid());
        verifyNoInteractions(entitlementService);
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    private ImageFilterParams createImageFilterParams(String blueprintName, StackType stackType) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName(blueprintName);
        return new ImageFilterParams(null, true, null, stackType, blueprint, null, new InternalUpgradeSettings(true, true, true), null);
    }

}