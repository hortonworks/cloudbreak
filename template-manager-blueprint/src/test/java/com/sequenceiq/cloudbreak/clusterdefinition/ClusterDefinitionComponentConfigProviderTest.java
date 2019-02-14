package com.sequenceiq.cloudbreak.clusterdefinition;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.template.ClusterDefinitionComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class ClusterDefinitionComponentConfigProviderTest {

    @Test
    public void testBlueprintComponentConfigProviderTestWhenSimpleImplementationExist() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        TemplatePreparationObject object = Builder.builder().build();

        ClusterDefinitionComponentConfigProviderTestImpl blueprintComponentConfigProviderTest = new ClusterDefinitionComponentConfigProviderTestImpl();

        Assert.assertNotNull(blueprintComponentConfigProviderTest.components());
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getSettingsEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getConfigurationEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.specialCondition(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.getHostgroupConfigurationEntries(object, blueprintText));
        Assert.assertNotNull(blueprintComponentConfigProviderTest.customTextManipulation(object, new AmbariBlueprintTextProcessor(blueprintText)));
    }

    public static class ClusterDefinitionComponentConfigProviderTestImpl implements ClusterDefinitionComponentConfigProvider {

    }

}