package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class KafkaKraftConfigProviderTest {

    private final KafkaKraftConfigProvider kraftConfigProvider = new KafkaKraftConfigProvider();

    @Mock
    private BlueprintView blueprintView;

    @Mock
    private CmTemplateProcessor processor;

    @Test
    public void testKraftConfigPresent() {
        cdpMainVersionIs("7.2.17");
        List<ApiClusterTemplateConfig> expectedConfig = List.of(config(KafkaConfigs.METADATA_STORE, "KRaft"));
        List<ApiClusterTemplateConfig> actualConfig =
                kraftConfigProvider.getRoleConfigs(KafkaRoles.KAFKA_BROKER, processor, sourceWithKraft(StackType.WORKLOAD));

        assertEquals(expectedConfig, actualConfig);
    }

    @Test
    public void testKraftConfigDefaultWithoutKRaft() {
        cdpMainVersionIs("7.2.17");
        List<ApiClusterTemplateConfig> actualConfig =
                kraftConfigProvider.getRoleConfigs(KafkaRoles.KAFKA_BROKER, processor, sourceWithoutKraft(StackType.WORKLOAD));

        assertEquals(List.of(), actualConfig);
    }

    @Test
    public void testConfigDefaultWithOldVersion() {
        cdpMainVersionIs("7.2.16");
        List<ApiClusterTemplateConfig> actualConfig =
                kraftConfigProvider.getRoleConfigs(KafkaRoles.KAFKA_BROKER, processor, sourceWithKraft(StackType.WORKLOAD));

        assertEquals(List.of(), actualConfig);
    }

    @Test
    public void testConfigDefaultOnDatalakeCluster() {
        cdpMainVersionIs("7.2.16");
        List<ApiClusterTemplateConfig> actualConfig =
                kraftConfigProvider.getRoleConfigs(KafkaRoles.KAFKA_BROKER, processor, sourceWithKraft(StackType.DATALAKE));

        assertEquals(List.of(), actualConfig);
    }

    private void cdpMainVersionIs(String version) {
        when(blueprintView.getProcessor()).thenReturn(processor);
        when(processor.getStackVersion()).thenReturn(version);
    }

    private TemplatePreparationObject sourceWithKraft(StackType stackType) {
        HostgroupView kraft = new HostgroupView("kraft", 1, InstanceGroupType.CORE, 3);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);

        when(source.getBlueprintView()).thenReturn(blueprintView);
        Mockito.lenient().when(source.getHostGroupsWithComponent(KafkaRoles.KAFKA_KRAFT)).thenReturn(Stream.of(kraft));
        Mockito.lenient().when(source.getStackType()).thenReturn(stackType);

        return source;
    }

    private TemplatePreparationObject sourceWithoutKraft(StackType stackType) {
        HostgroupView kafka = new HostgroupView("kafka", 1, InstanceGroupType.CORE, 3);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);

        when(source.getBlueprintView()).thenReturn(blueprintView);
        Mockito.lenient().when(source.getHostGroupsWithComponent(KafkaRoles.KAFKA_BROKER)).thenReturn(Stream.of(kafka));
        Mockito.lenient().when(source.getStackType()).thenReturn(stackType);

        return source;
    }
}
