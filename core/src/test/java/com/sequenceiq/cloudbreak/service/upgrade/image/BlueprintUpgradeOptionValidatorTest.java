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
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false);
        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false);
        assertFalse(actual.isValid());
        assertEquals("The cluster template is not eligible for upgrade because the upgrade option is: DISABLED", actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsAValidCustomTemplate() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        when(customTemplateUpgradeValidator.isValid(blueprint)).thenReturn(new BlueprintValidationResult(true));
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false);
        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verify(customTemplateUpgradeValidator).isValid(blueprint);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsANotValidCustomTemplate() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        BlueprintValidationResult result = new BlueprintValidationResult(false, "Custom template not eligible.");
        when(customTemplateUpgradeValidator.isValid(blueprint)).thenReturn(result);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false);
        assertFalse(actual.isValid());
        assertEquals(result.getReason(), actual.getReason());
        verify(customTemplateUpgradeValidator).isValid(blueprint);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsEnabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsEnabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsEnabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, false);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionNullOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, null);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true);
        assertTrue(actual.isValid());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsOsDisabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true);
        assertFalse(actual.isValid());
        assertEquals("The cluster template is not eligible for upgrade because the upgrade option is: OS_UPGRADE_DISABLED", actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        BlueprintValidationResult actual = underTest.isValidBlueprint(blueprint, true);
        assertFalse(actual.isValid());
        assertEquals("The cluster template is not eligible for upgrade because the upgrade option is: DISABLED", actual.getReason());
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    private Blueprint createBlueprint(ResourceStatus resourceStatus, BlueprintUpgradeOption upgradeOption) {
        Blueprint blueprint = new Blueprint();
        blueprint.setStatus(resourceStatus);
        blueprint.setBlueprintUpgradeOption(upgradeOption);
        return blueprint;
    }

}