package com.sequenceiq.cloudbreak.blueprint.hadoop;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject.Builder;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.ConfigService;
import com.sequenceiq.cloudbreak.blueprint.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@RunWith(MockitoJUnitRunner.class)
public class HadoopConfigurationServiceTest {

    @InjectMocks
    private final HadoopConfigurationService underTest = new HadoopConfigurationService();

    @Mock
    private ConfigService configService;

    @Test
    public void testConfigure() {
        BlueprintPreparationObject source = Builder.builder()
                .build();
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);

        when(configService.getHostGroupConfiguration(blueprintTextProcessor, source.getHostgroupViews())).thenReturn(emptyMap());
        when(configService.getComponentsByHostGroup(blueprintTextProcessor, source.getHostgroupViews())).thenReturn(emptyMap());

        when(blueprintTextProcessor.extendBlueprintHostGroupConfiguration(any(HostgroupConfigurations.class), any(Boolean.class)))
                .thenReturn(blueprintTextProcessor);
        when(blueprintTextProcessor.extendBlueprintGlobalConfiguration(any(SiteConfigurations.class), any(Boolean.class))).thenReturn(blueprintTextProcessor);

        BlueprintTextProcessor actual = underTest.customTextManipulation(source, blueprintTextProcessor);

        Assert.assertEquals(blueprintTextProcessor, actual);
    }

    @Test
    public void testAdditionalCriteriaWhenTrue() {
        BlueprintPreparationObject source = Builder.builder()
                .withBlueprintView(new BlueprintView("blueprintText", "2.5", "HDP"))
                .build();

        boolean actual = underTest.specialCondition(source, "blueprintText");
        Assert.assertTrue(actual);
    }

    @Test
    public void testAdditionalCriteriaWhenFalse() {
        BlueprintPreparationObject source = Builder.builder()
                .withBlueprintView(new BlueprintView("blueprintText", "2.5", "HDF"))
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
