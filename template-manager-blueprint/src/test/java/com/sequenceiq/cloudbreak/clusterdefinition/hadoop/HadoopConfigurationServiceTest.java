package com.sequenceiq.cloudbreak.clusterdefinition.hadoop;

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

import com.sequenceiq.cloudbreak.clusterdefinition.ConfigService;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.template.views.ClusterDefinitionView;

@RunWith(MockitoJUnitRunner.class)
public class HadoopConfigurationServiceTest {

    @InjectMocks
    private final HadoopConfigurationService underTest = new HadoopConfigurationService();

    @Mock
    private ConfigService configService;

    @Test
    public void testConfigure() {
        TemplatePreparationObject source = Builder.builder()
                .build();
        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        when(configService.getHostGroupConfiguration(ambariBlueprintTextProcessor, source.getHostgroupViews())).thenReturn(emptyMap());
        when(configService.getComponentsByHostGroup(ambariBlueprintTextProcessor, source.getHostgroupViews())).thenReturn(emptyMap());

        when(ambariBlueprintTextProcessor.extendBlueprintHostGroupConfiguration(any(HostgroupConfigurations.class), any(Boolean.class)))
                .thenReturn(ambariBlueprintTextProcessor);
        when(ambariBlueprintTextProcessor.extendBlueprintGlobalConfiguration(any(SiteConfigurations.class), any(Boolean.class)))
                .thenReturn(ambariBlueprintTextProcessor);

        AmbariBlueprintTextProcessor actual = underTest.customTextManipulation(source, ambariBlueprintTextProcessor);

        Assert.assertEquals(ambariBlueprintTextProcessor, actual);
    }

    @Test
    public void testAdditionalCriteriaWhenTrue() {
        TemplatePreparationObject source = Builder.builder()
                .withClusterDefinitionView(new ClusterDefinitionView("blueprintText", "2.5", "HDP"))
                .build();

        boolean actual = underTest.specialCondition(source, "blueprintText");
        Assert.assertTrue(actual);
    }

    @Test
    public void testAdditionalCriteriaWhenFalse() {
        TemplatePreparationObject source = Builder.builder()
                .withClusterDefinitionView(new ClusterDefinitionView("blueprintText", "2.5", "HDF"))
                .build();

        boolean actual = underTest.specialCondition(source, "blueprintText");
        Assert.assertFalse(actual);
    }

    private ClusterDefinition prepareBlueprintForBuilder(String text, String version, String type) {
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setClusterDefinitionText(text);
        return clusterDefinition;
    }

}
