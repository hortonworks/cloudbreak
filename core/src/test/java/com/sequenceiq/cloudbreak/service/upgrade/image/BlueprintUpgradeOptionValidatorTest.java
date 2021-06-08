package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintUpgradeOptionValidatorTest {

    private final BlueprintUpgradeOptionValidator underTest = new BlueprintUpgradeOptionValidator();

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsEnabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, false));
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        assertFalse(underTest.isValidBlueprint(blueprint, false));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsCustomAndUpgradeOptionIsEnabledAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, false));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsCustomAndUpgradeOptionIsDisabledAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.DISABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, false));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsCustomAndUpgradeOptionIsNullAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, null);
        assertTrue(underTest.isValidBlueprint(blueprint, true));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsEnabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, true));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsEnabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, true));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsEnabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, false));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsOsDisabledOnDefaultBlueprintAndTheUpgradeIsRuntimeUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        assertTrue(underTest.isValidBlueprint(blueprint, false));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionNullOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, null);
        assertTrue(underTest.isValidBlueprint(blueprint, true));
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsOsDisabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        assertFalse(underTest.isValidBlueprint(blueprint, true));
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprintAndTheUpgradeIsOsUpgrade() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        assertFalse(underTest.isValidBlueprint(blueprint, true));
    }

    private Blueprint createBlueprint(ResourceStatus resourceStatus, BlueprintUpgradeOption upgradeOption) {
        Blueprint blueprint = new Blueprint();
        blueprint.setStatus(resourceStatus);
        blueprint.setBlueprintUpgradeOption(upgradeOption);
        return blueprint;
    }

}