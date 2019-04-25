package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.template.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class BlueprintComponentConfigProviderTest {

    @Test
    public void testBlueprintComponentConfigProviderTestWhenSimpleImplementationExist() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        TemplatePreparationObject object = Builder.builder().build();

        BlueprintComponentConfigProviderTestImpl blueprintComponentConfigProviderTest = new BlueprintComponentConfigProviderTestImpl();

        Assert.assertNotNull(blueprintComponentConfigProviderTest.components());
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getSettingsEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getConfigurationEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.specialCondition(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getHostgroupConfigurationEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.customTextManipulation(object, new AmbariBlueprintTextProcessor(blueprintText)));
    }

    public static class BlueprintComponentConfigProviderTestImpl implements BlueprintComponentConfigProvider {

    }

}