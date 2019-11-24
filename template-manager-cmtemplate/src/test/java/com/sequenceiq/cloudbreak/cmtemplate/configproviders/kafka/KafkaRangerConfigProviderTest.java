package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;

@RunWith(MockitoJUnitRunner.class)
public class KafkaRangerConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @InjectMocks
    private KafkaRangerConfigProvider underTest;

    @Test
    public void testKafkaRangerNameWhenWorkloadAndKafkaPresentedAndCM710ShouldShowTheGeneratedName() {
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable("7.1.0"));
        when(cmTemplateProcessor.isRoleTypePresentInService(underTest.getServiceType(), underTest.getRoleTypes())).thenReturn(Boolean.TRUE);
        when(source.getGeneralClusterConfigs()).thenReturn(getGeneralClusterConfigs());
        when(source.getStackType()).thenReturn(StackType.WORKLOAD);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, source);
        assertEquals(1, serviceConfigs.size());
        assertEquals("cm-kafka-cluster", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testKafkaRangerNameWhenWorkloadAndKafkaPresentedAndCM701ShouldShowTheGeneratedName() {
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable("7.0.1"));

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, source);
        assertEquals(0, serviceConfigs.size());

    }

    private GeneralClusterConfigs getGeneralClusterConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setStackName("kafka-cluster");
        return generalClusterConfigs;
    }

}