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
    public void testIsValidBlueprintShouldReturnTrueWhenTheUpgradeOptionIsEnabledOnDefaultBlueprint() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint));
    }

    @Test
    public void testIsValidBlueprintShouldReturnFalseWhenTheUpgradeOptionIsDisabledOnDefaultBlueprint() {
        Blueprint blueprint = createBlueprint(ResourceStatus.DEFAULT, BlueprintUpgradeOption.DISABLED);
        assertFalse(underTest.isValidBlueprint(blueprint));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsCustomAndUpgradeOptionIsEnabled() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.ENABLED);
        assertTrue(underTest.isValidBlueprint(blueprint));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsCustomAndUpgradeOptionIsDisabled() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, BlueprintUpgradeOption.DISABLED);
        assertTrue(underTest.isValidBlueprint(blueprint));
    }

    @Test
    public void testIsValidBlueprintShouldReturnTrueWhenTheTemplateIsCustomAndUpgradeOptionIsNull() {
        Blueprint blueprint = createBlueprint(ResourceStatus.USER_MANAGED, null);
        assertTrue(underTest.isValidBlueprint(blueprint));
    }

    private Blueprint createBlueprint(ResourceStatus resourceStatus, BlueprintUpgradeOption upgradeOption) {
        Blueprint blueprint = new Blueprint();
        blueprint.setStatus(resourceStatus);
        blueprint.setBlueprintUpgradeOption(upgradeOption);
        return blueprint;
    }

}