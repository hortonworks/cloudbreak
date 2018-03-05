package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class BlueprintComponentConfigProviderTest {

    @Test
    public void testBlueprintComponentConfigProviderTestWhenSimpleImplementationExist() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder().build();

        BlueprintComponentConfigProviderTestImpl blueprintComponentConfigProviderTest = new BlueprintComponentConfigProviderTestImpl();

        Assert.assertNotNull(blueprintComponentConfigProviderTest.components());
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getSettingsEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getConfigurationEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.additionalCriteria(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getHostgroupConfigurationEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.customTextManipulation(object, new BlueprintTextProcessor(blueprintText)));
    }

    public static class BlueprintComponentConfigProviderTestImpl implements BlueprintComponentConfigProvider {

    }

}