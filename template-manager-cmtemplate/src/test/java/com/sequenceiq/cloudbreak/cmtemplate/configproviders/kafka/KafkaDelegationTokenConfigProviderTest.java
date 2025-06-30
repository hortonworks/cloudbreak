package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class KafkaDelegationTokenConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @InjectMocks
    private KafkaDelegationTokenConfigProvider underTest;

    @Test
    void testIsConfigurationNeededWhenNoKafkaBrokerRoleType() {
        KerberosConfig kerberosConfig = KerberosConfig
                .KerberosConfigBuilder
                .aKerberosConfig()
                .build();
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder
                .builder()
                .withKerberosConfig(kerberosConfig)
                .build();
        when(cmTemplateProcessor.isRoleTypePresentInService(underTest.getServiceType(), underTest.getRoleTypes())).thenReturn(Boolean.FALSE);

        boolean result = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);

        Assertions.assertFalse(result);
    }

    @Test
    void testIsConfigurationNeededWhenKerberosIsNotEnabled() {
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder.builder().build();

        boolean result = underTest.isConfigurationNeeded(null, templatePreparationObject);

        Assertions.assertFalse(result);
    }

    @Test
    void testIsConfigurationNeededShouldReturnTrueWhenKafkaBrokerRoleTypeAndKafkaServiceAreAvailableAndKerberosIsEnabled() {
        KerberosConfig kerberosConfig = KerberosConfig
                .KerberosConfigBuilder
                .aKerberosConfig()
                .build();
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder
                .builder()
                .withKerberosConfig(kerberosConfig)
                .build();
        when(cmTemplateProcessor.isRoleTypePresentInService(underTest.getServiceType(), underTest.getRoleTypes())).thenReturn(Boolean.TRUE);

        boolean result = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);

        Assertions.assertTrue(result);
    }

    @Test
    void testKRaftRoleIsPresent() {
        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        HostgroupView kraft = new HostgroupView("kraft", 1, InstanceGroupType.CORE, 3);
        when(templatePreparationObject.getHostGroupsWithComponent(KafkaRoles.KAFKA_KRAFT)).thenReturn(Stream.of(kraft));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(cmTemplateProcessor, templatePreparationObject);
        List<ApiClusterTemplateConfig> expected = List.of(config(KafkaConfigs.DELEGATION_TOKEN_ENABLE, "true"));

        Assertions.assertEquals(expected, result);
    }
}