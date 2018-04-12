package com.sequenceiq.cloudbreak.blueprint;

import com.sequenceiq.cloudbreak.template.processor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class BlueprintComponentConfigProviderTest {

    @Test
    public void testBlueprintComponentConfigProviderTestWhenSimpleImplementationExist() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        TemplatePreparationObject object = TemplatePreparationObject.Builder.builder().build();

        BlueprintComponentConfigProviderTestImpl blueprintComponentConfigProviderTest = new BlueprintComponentConfigProviderTestImpl();

        Assert.assertNotNull(blueprintComponentConfigProviderTest.components());
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getSettingsEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getConfigurationEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.additionalCriteria(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getHostgroupConfigurationEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.customTextManipulation(object, new TemplateTextProcessor(blueprintText)));
    }

    public static class BlueprintComponentConfigProviderTestImpl implements BlueprintComponentConfigProvider {

    }

}