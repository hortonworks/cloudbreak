package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintUpgradeOptionValidatorTest {

    @InjectMocks
    private BlueprintUpgradeOptionValidator underTest;

    @Mock
    private CustomTemplateUpgradeValidator customTemplateUpgradeValidator;

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsEnabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, true);
        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsGaOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.GA);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, true);
        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsGaOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.GA);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true, false, true);
        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, true);
        assertFalse(actual.isValid());
        assertEquals("The cluster template is not eligible for upgrade", actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgradeWithInternal() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, true, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsAValidCustomTemplate() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        when(customTemplateUpgradeValidator.isValid(blueprint)).thenReturn(new BlueprintValidationResult(true));
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, false);
        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verify(customTemplateUpgradeValidator).isValid(blueprint);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheTemplateIsANotValidCustomTemplate() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        BlueprintValidationResult result = new BlueprintValidationResult(false, "Custom template not eligible.");
        when(customTemplateUpgradeValidator.isValid(blueprint)).thenReturn(result);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, false);
        assertFalse(actual.isValid());
        assertEquals(result.getReason(), actual.getReason());
        verify(customTemplateUpgradeValidator).isValid(blueprint);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsANotValidCustomTemplateButTheEntitlementIsGranted() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsANotValidCustomTemplateButInternalApiUsed() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, true, false);
        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsEnabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true, false, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsEnabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true, false, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsEnabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionNullOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, null);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true, false, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsOsDisabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true, false, true);
        assertFalse(actual.isValid());
        assertEquals("The cluster template is not eligible for upgrade", actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true, false, true);
        assertFalse(actual.isValid());
        assertEquals("The cluster template is not eligible for upgrade", actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsMaintenanceGAOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.MAINTENANCE_UPGRADE_GA);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, false);
        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsEnabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgradeAndNoEntitlement() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, false);
        assertFalse(actual.isValid());
        assertEquals(actual.getReason(), "The cluster template is not eligible for upgrade");
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsOSEnabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgradeAndNoEntitlement() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false, false, false);
        assertFalse(actual.isValid());
        assertEquals(actual.getReason(), "The cluster template is not eligible for upgrade");
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    private Blueprint createBlueprint(ResourceStatus resourceStatus, BlueprintUpgradeOption upgradeOption) {
        Blueprint blueprint = new Blueprint();
        blueprint.setStatus(resourceStatus);
        blueprint.setBlueprintUpgradeOption(upgradeOption);
        return blueprint;
    }

}