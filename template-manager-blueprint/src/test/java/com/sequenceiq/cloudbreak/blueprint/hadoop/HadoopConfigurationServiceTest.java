package com.sequenceiq.cloudbreak.blueprint.hadoop;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.ConfigService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@RunWith(MockitoJUnitRunner.class)
public class HadoopConfigurationServiceTest {

    @InjectMocks
    private final HadoopConfigurationService underTest = new HadoopConfigurationService();

    @Mock
    private AmbariBlueprintTextProcessor ambariBlueprintTextProcessor;

    @Mock
    private ConfigService configService;

    @Test
    public void testConfigure() {
        TemplatePreparationObject source = Builder.builder()
                .build();

        when(configService.getHostGroupConfiguration(ambariBlueprintTextProcessor, source.getHostgroupViews())).thenReturn(emptyMap());
        when(configService.getComponentsByHostGroup(ambariBlueprintTextProcessor, source.getHostgroupViews())).thenReturn(emptyMap());

        when(ambariBlueprintTextProcessor.extendBlueprintHostGroupConfiguration(any(HostgroupConfigurations.class), any(Boolean.class)))
                .thenReturn(ambariBlueprintTextProcessor);
        when(ambariBlueprintTextProcessor.extendBlueprintGlobalConfiguration(any(SiteConfigurations.class), any(Boolean.class)))
                .thenReturn(ambariBlueprintTextProcessor);

        BlueprintTextProcessor actual = underTest.customTextManipulation(source, ambariBlueprintTextProcessor);

        Assert.assertEquals(ambariBlueprintTextProcessor, actual);
    }

    @Test
    public void testAdditionalCriteriaWhenTrue() {
        TemplatePreparationObject source = Builder.builder()
                .withBlueprintView(new BlueprintView("blueprintText", "2.5", "HDP", ambariBlueprintTextProcessor))
                .build();

        boolean actual = underTest.specialCondition(source, "blueprintText");
        Assert.assertTrue(actual);
    }

    @Test
    public void testAdditionalCriteriaWhenFalse() {
        TemplatePreparationObject source = Builder.builder()
                .withBlueprintView(new BlueprintView("blueprintText", "2.5", "HDF", ambariBlueprintTextProcessor))
                .build();

        boolean actual = underTest.specialCondition(source, "blueprintText");
        Assert.assertFalse(actual);
    }

    private Blueprint prepareBlueprintForBuilder(String text, String version, String type) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(text);
        return blueprint;
    }

}
