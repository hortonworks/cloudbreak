package com.sequenceiq.cloudbreak.blueprint.hadoop;

import com.sequenceiq.cloudbreak.blueprint.ConfigService;
import com.sequenceiq.cloudbreak.templateprocessor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.templateprocessor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateTextProcessor;
import com.sequenceiq.cloudbreak.templateprocessor.template.views.BlueprintView;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static java.util.Collections.emptyMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HadoopConfigurationServiceTest {

    @InjectMocks
    private final HadoopConfigurationService underTest = new HadoopConfigurationService();

    @Mock
    private ConfigService configService;

    @Test
    public void testConfigure() throws IOException {
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .build();
        TemplateTextProcessor blueprintTextProcessor = mock(TemplateTextProcessor.class);

        when(configService.getHostGroupConfiguration(blueprintTextProcessor, source.getHostgroupViews())).thenReturn(emptyMap());
        when(configService.getComponentsByHostGroup(blueprintTextProcessor, source.getHostgroupViews())).thenReturn(emptyMap());

        when(blueprintTextProcessor.extendBlueprintHostGroupConfiguration(any(HostgroupConfigurations.class), anyBoolean())).thenReturn(blueprintTextProcessor);
        when(blueprintTextProcessor.extendBlueprintGlobalConfiguration(any(SiteConfigurations.class), anyBoolean())).thenReturn(blueprintTextProcessor);

        TemplateTextProcessor actual = underTest.customTextManipulation(source, blueprintTextProcessor);

        Assert.assertEquals(blueprintTextProcessor, actual);
    }

    @Test
    public void testAdditionalCriteriaWhenTrue() {
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(new BlueprintView("blueprintText", "2.5", "HDP"))
                .build();

        boolean actual = underTest.additionalCriteria(source, "blueprintText");
        Assert.assertTrue(actual);
    }

    @Test
    public void testAdditionalCriteriaWhenFalse() {
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(new BlueprintView("blueprintText", "2.5", "HDF"))
                .build();

        boolean actual = underTest.additionalCriteria(source, "blueprintText");
        Assert.assertFalse(actual);
    }
}
