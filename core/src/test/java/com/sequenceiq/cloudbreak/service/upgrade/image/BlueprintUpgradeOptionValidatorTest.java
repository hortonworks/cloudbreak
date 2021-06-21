package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertFalse;
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
        assertTrue(underTest.isValidBlueprint(blueprint, false));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        assertFalse(underTest.isValidBlueprint(blueprint, false));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsAValidCustomTemplate() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        when(customTemplateUpgradeValidator.isValid(blueprint)).thenReturn(true);
        assertTrue(underTest.isValidBlueprint(blueprint, false));
        verify(customTemplateUpgradeValidator).isValid(blueprint);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsANotValidCustomTemplate() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        when(customTemplateUpgradeValidator.isValid(blueprint)).thenReturn(false);
        assertFalse(underTest.isValidBlueprint(blueprint, false));
        verify(customTemplateUpgradeValidator).isValid(blueprint);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsEnabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, true));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsEnabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, true));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsEnabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, false));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, false));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionNullOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, null);
        assertTrue(underTest.isValidBlueprint(blueprint, true));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsOsDisabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        assertFalse(underTest.isValidBlueprint(blueprint, true));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        assertFalse(underTest.isValidBlueprint(blueprint, true));
        verifyNoInteractions(customTemplateUpgradeValidator);
    }

    private Blueprint createBlueprint(ResourceStatus resourceStatus, BlueprintUpgradeOption upgradeOption) {
        Blueprint blueprint = new Blueprint();
        blueprint.setStatus(resourceStatus);
        blueprint.setBlueprintUpgradeOption(upgradeOption);
        return blueprint;
    }

}