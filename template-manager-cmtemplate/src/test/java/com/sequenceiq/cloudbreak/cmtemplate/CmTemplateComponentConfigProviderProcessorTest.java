package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles.HIVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CommonCoreConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaDatahubConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CmTemplateComponentConfigProviderProcessor.class)
class CmTemplateComponentConfigProviderProcessorTest {

    @MockBean
    private CommonCoreConfigProvider commonCoreConfigProvider;

    @MockBean
    private KafkaDatahubConfigProvider kafkaDatahubConfigProvider;

    @MockBean
    private HiveMetastoreConfigProvider hiveMetastoreConfigProvider;

    @MockBean
    private HueConfigProvider hueConfigProvider;

    @Inject
    private CmTemplateComponentConfigProviderProcessor underTest;

    @Test
    void testProcess() {
        CmTemplateProcessor cmTemplateProcessor = mock();
        TemplatePreparationObject template = mock();

        when(commonCoreConfigProvider.getAdditionalServices(cmTemplateProcessor, template)).thenReturn(Map.of());
        when(commonCoreConfigProvider.isConfigurationNeeded(cmTemplateProcessor, template)).thenReturn(true);
        when(commonCoreConfigProvider.getServiceType()).thenCallRealMethod();
        List<ApiClusterTemplateConfig> coreServiceConfigs = mock();
        when(commonCoreConfigProvider.getServiceConfigs(cmTemplateProcessor, template)).thenReturn(coreServiceConfigs);
        List<ApiClusterTemplateVariable> coreServiceConfigVariables = mock();
        when(commonCoreConfigProvider.getServiceConfigVariables(template)).thenReturn(coreServiceConfigVariables);
        Map<String, List<ApiClusterTemplateConfig>> coreRoleConfigs = mock();
        when(commonCoreConfigProvider.getRoleConfigs(cmTemplateProcessor, template)).thenReturn(coreRoleConfigs);
        List<ApiClusterTemplateVariable> coreRoleConfigVariables = mock();
        when(commonCoreConfigProvider.getRoleConfigVariables(cmTemplateProcessor, template)).thenReturn(coreRoleConfigVariables);

        ApiClusterTemplateService kafkaApiClusterTemplateService = mock();
        when(kafkaDatahubConfigProvider.getAdditionalServices(cmTemplateProcessor, template)).thenReturn(Map.of("service1", kafkaApiClusterTemplateService));
        when(kafkaDatahubConfigProvider.isConfigurationNeeded(cmTemplateProcessor, template)).thenReturn(true);
        when(kafkaDatahubConfigProvider.getServiceType()).thenCallRealMethod();
        List<ApiClusterTemplateConfig> kafkaServiceConfigs = mock();
        when(kafkaDatahubConfigProvider.getServiceConfigs(cmTemplateProcessor, template)).thenReturn(kafkaServiceConfigs);
        List<ApiClusterTemplateVariable> kafkaServiceConfigVariables = mock();
        when(kafkaDatahubConfigProvider.getServiceConfigVariables(template)).thenReturn(kafkaServiceConfigVariables);
        Map<String, List<ApiClusterTemplateConfig>> kafkaRoleConfigs = mock();
        when(kafkaDatahubConfigProvider.getRoleConfigs(cmTemplateProcessor, template)).thenReturn(kafkaRoleConfigs);
        List<ApiClusterTemplateVariable> kafkaRoleConfigVariables = mock();
        when(kafkaDatahubConfigProvider.getRoleConfigVariables(cmTemplateProcessor, template)).thenReturn(kafkaRoleConfigVariables);

        when(hiveMetastoreConfigProvider.getAdditionalServices(cmTemplateProcessor, template)).thenReturn(Map.of());
        when(hiveMetastoreConfigProvider.isConfigurationNeeded(cmTemplateProcessor, template)).thenReturn(false);

        when(hueConfigProvider.getAdditionalServices(cmTemplateProcessor, template)).thenReturn(Map.of());
        when(hueConfigProvider.isConfigurationNeeded(cmTemplateProcessor, template)).thenReturn(true);
        when(hueConfigProvider.getServiceType()).thenCallRealMethod();
        List<ApiClusterTemplateConfig> hueServiceConfigs = mock();
        when(hueConfigProvider.getServiceConfigs(cmTemplateProcessor, template)).thenReturn(hueServiceConfigs);
        List<ApiClusterTemplateVariable> hueServiceConfigVariables = mock();
        when(hueConfigProvider.getServiceConfigVariables(template)).thenReturn(hueServiceConfigVariables);
        Map<String, List<ApiClusterTemplateConfig>> hueRoleConfigs = mock();
        when(hueConfigProvider.getRoleConfigs(cmTemplateProcessor, template)).thenReturn(hueRoleConfigs);
        List<ApiClusterTemplateVariable> hueRoleConfigVariables = mock();
        when(hueConfigProvider.getRoleConfigVariables(cmTemplateProcessor, template)).thenReturn(hueRoleConfigVariables);

        CmTemplateProcessor result = underTest.process(cmTemplateProcessor, template);

        verify(cmTemplateProcessor).extendTemplateWithAdditionalServices(Map.of("service1", kafkaApiClusterTemplateService));
        verify(cmTemplateProcessor, times(3)).extendTemplateWithAdditionalServices(Map.of());

        verify(cmTemplateProcessor).addServiceConfigs(CORE_SETTINGS, coreServiceConfigs);
        verify(cmTemplateProcessor).addVariables(coreServiceConfigVariables);
        verify(cmTemplateProcessor).addRoleConfigs(CORE_SETTINGS, coreRoleConfigs);
        verify(cmTemplateProcessor).addVariables(coreRoleConfigVariables);

        verify(cmTemplateProcessor).addServiceConfigs(KAFKA_SERVICE, kafkaServiceConfigs);
        verify(cmTemplateProcessor).addVariables(kafkaServiceConfigVariables);
        verify(cmTemplateProcessor).addRoleConfigs(KAFKA_SERVICE, kafkaRoleConfigs);
        verify(cmTemplateProcessor).addVariables(kafkaRoleConfigVariables);

        verify(hiveMetastoreConfigProvider, never()).getServiceConfigs(any(), any());
        verify(hiveMetastoreConfigProvider, never()).getServiceConfigVariables(any());
        verify(hiveMetastoreConfigProvider, never()).getRoleConfigs(any(), any());
        verify(hiveMetastoreConfigProvider, never()).getRoleConfigVariables(any(), any());

        verify(cmTemplateProcessor).addServiceConfigs("HUE", hueServiceConfigs);
        verify(cmTemplateProcessor).addVariables(hueServiceConfigVariables);
        verify(cmTemplateProcessor).addRoleConfigs("HUE", hueRoleConfigs);
        verify(cmTemplateProcessor).addVariables(hueRoleConfigVariables);

        assertEquals(cmTemplateProcessor, result);
    }

    @Test
    void testGetServiceConfigsToBeUpdatedDuringUpgrade() {
        String fromCmVersion = "7.12.0.400";
        String toCmVersion = "7.12.0.500-58279810";
        String mockServiceType = "MOCK_SERVICE";
        CmTemplateProcessor cmTemplateProcessor = mock();
        TemplatePreparationObject template = mock();

        when(commonCoreConfigProvider.isServiceConfigUpdateNeededForUpgrade(fromCmVersion, toCmVersion)).thenCallRealMethod();
        when(commonCoreConfigProvider.getUpdatedServiceConfigForUpgrade(cmTemplateProcessor, template)).thenReturn(Map.of("key1", "value1"));
        when(commonCoreConfigProvider.getServiceType()).thenReturn(mockServiceType);

        when(kafkaDatahubConfigProvider.isServiceConfigUpdateNeededForUpgrade(fromCmVersion, toCmVersion)).thenReturn(true);
        when(kafkaDatahubConfigProvider.getUpdatedServiceConfigForUpgrade(cmTemplateProcessor, template)).thenCallRealMethod();

        when(hiveMetastoreConfigProvider.isServiceConfigUpdateNeededForUpgrade(fromCmVersion, toCmVersion)).thenReturn(true);
        when(hiveMetastoreConfigProvider.getUpdatedServiceConfigForUpgrade(cmTemplateProcessor, template)).thenReturn(Map.of("key2", "value2"));
        when(hiveMetastoreConfigProvider.getServiceType()).thenReturn(mockServiceType);

        when(hueConfigProvider.isServiceConfigUpdateNeededForUpgrade(fromCmVersion, toCmVersion)).thenCallRealMethod();

        Map<String, Map<String, String>> result =
                underTest.getServiceConfigsToBeUpdatedDuringUpgrade(cmTemplateProcessor, template, fromCmVersion, toCmVersion);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(mockServiceType, Map.of("key1", "value1", "key2", "value2")));
    }

    @Test
    void testCollectDataConfigurations() {
        CmTemplateProcessor cmTemplateProcessor = mock();
        TemplatePreparationObject template = mock();

        when(hiveMetastoreConfigProvider.isConfigurationNeeded(cmTemplateProcessor, template)).thenReturn(true);
        ApiClusterTemplateConfig hiveServiceConfig = mock();
        when(hiveServiceConfig.getName()).thenReturn("hiveConfigName");
        when(hiveServiceConfig.getValue()).thenReturn("hiveConfigValue");
        when(hiveMetastoreConfigProvider.getServiceConfigs(cmTemplateProcessor, template)).thenReturn(List.of(hiveServiceConfig));
        when(hiveMetastoreConfigProvider.getServiceType()).thenCallRealMethod();

        when(hueConfigProvider.isConfigurationNeeded(cmTemplateProcessor, template)).thenReturn(false);

        Table<String, String, String> result = underTest.collectDataConfigurations(cmTemplateProcessor, template);

        verifyNoInteractions(commonCoreConfigProvider);
        verifyNoInteractions(kafkaDatahubConfigProvider);
        assertThat(result.rowMap()).containsExactlyInAnyOrderEntriesOf(Map.of(HIVE, Map.of("hiveConfigName", "hiveConfigValue")));
    }
}
