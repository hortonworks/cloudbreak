package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveOnTezSafetyValveConfigProvider.HIVE_SERVICE_CONFIG_SAFETY_VALVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

@ExtendWith(MockitoExtension.class)
class HiveOnTezSafetyValveConfigProviderTest {

    private static final String V7_0_1 = "7.0.1";

    private static final String V7_0_2 = "7.0.2";

    private static final String V7_0_99 = "7.0.99";

    private static final String V7_1_0 = "7.1.0";

    private static final String V7_16_0 = "7.16.0";

    private static final Iterable<String> KERBEROS_SAFETY_VALVE_VERSIONS = Lists.newArrayList(null, V7_0_1, V7_0_2, V7_0_99);

    private static final Iterable<String> NO_KERBEROS_SAFETY_VALVE_VERSIONS = Set.of(V7_1_0);

    private static final Iterable<String> NO_KERBEROS_SAFETY_VALVE_WITH_SSL_CHANNEL_MODE = Set.of(V7_16_0);

    private static final Set<ApiClusterTemplateConfig> KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS = Set.of(
            config(HIVE_SERVICE_CONFIG_SAFETY_VALVE,
                    ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.principal", "HTTP/_HOST@EXAMPLE.COM") +
                            ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.keytab", "hive.keytab") +
                            ConfigUtils.getSafetyValveProperty("hive.hook.proto.file.per.event", "true")));

    private static final Set<ApiClusterTemplateConfig> NO_KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS = Set.of(
            config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty("hive.hook.proto.file.per.event", "true")));

    private static final Set<ApiClusterTemplateConfig> NO_KERBEROS_SAFETY_VALVE_WITH_SSL_CHANNEL_MODE_EXPECTED_CONFIGS = Set.of(
            config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty("hive.hook.proto.file.per.event", "true") +
                    ConfigUtils.getSafetyValveProperty("fs.s3a.ssl.channel.mode", "openssl"))
            );

    @Mock
    private HdfsConfigHelper hdfsConfigHelper;

    @InjectMocks
    private HiveOnTezSafetyValveConfigProvider underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @BeforeEach
    void setUp() {
        lenient().when(templatePreparationObject.getKerberosConfig()).thenReturn(Optional.of(KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withRealm("EXAMPLE.COM")
                .build()));
        lenient().when(templatePreparationObject.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        lenient().when(cmTemplateProcessor.isHybridDatahub(templatePreparationObject)).thenReturn(false);
    }

    private static ApiClusterTemplateConfig config(String name, String value) {
        ApiClusterTemplateConfig cfg = new ApiClusterTemplateConfig();
        cfg.setName(name);
        cfg.setValue(value);
        return cfg;
    }

    private TemplatePreparationObject setPlatfrormVariant(String platformVariant) {
        when(templatePreparationObject.getPlatformVariant()).thenReturn(platformVariant);
        return templatePreparationObject;
    }

    @Test
    void getServiceConfigsKerberosSafetyValve() {
        testGetServiceConfigs(KERBEROS_SAFETY_VALVE_VERSIONS, KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS);
    }

    @Test
    void getServiceConfigsNoKerberosSafetyValve() {
        testGetServiceConfigs(NO_KERBEROS_SAFETY_VALVE_VERSIONS, NO_KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS);
    }

    @Test
    void getServiceConfigsKerberosSafetyValveForAWSGov() {
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable(V7_16_0));
        TemplatePreparationObject tpo = setPlatfrormVariant("AWS_NATIVE_GOV");
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, tpo);
        assertThat(serviceConfigs).as("Expected configs for cdh version: %s", V7_16_0).hasSameElementsAs(NO_KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS);
    }

    @Test
    void getServiceConfigsNoKerberosSafetyValveWithSslChannelMode() {
        testGetServiceConfigs(NO_KERBEROS_SAFETY_VALVE_WITH_SSL_CHANNEL_MODE, NO_KERBEROS_SAFETY_VALVE_WITH_SSL_CHANNEL_MODE_EXPECTED_CONFIGS);
    }

    @Test
    void getServiceConfigsNotHybrid() {
        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(cmTemplateProcessor, templatePreparationObject);

        assertThat(result.getFirst().getName()).isEqualTo(HIVE_SERVICE_CONFIG_SAFETY_VALVE);
        assertThat(result.getFirst().getValue())
                .doesNotContain("hive.metastore.uris")
                .doesNotContain("hive.exec.scratchdir");
    }

    @Test
    void getServiceConfigsHybrid() {
        DatalakeView datalakeView = mock();
        when(templatePreparationObject.getDatalakeView()).thenReturn(Optional.of(datalakeView));
        RdcView rdcView = mock();
        when(datalakeView.getRdcView()).thenReturn(rdcView);
        when(rdcView.getEndpoints(HiveRoles.HIVE, HiveRoles.HIVEMETASTORE)).thenReturn(Set.of("thrift://dl"));
        when(cmTemplateProcessor.isHybridDatahub(templatePreparationObject)).thenReturn(true);
        when(hdfsConfigHelper.getHdfsUrl(cmTemplateProcessor, templatePreparationObject)).thenReturn(Optional.of("hdfs://nshybrid"));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(cmTemplateProcessor, templatePreparationObject);

        assertThat(result.getFirst().getName()).isEqualTo(HIVE_SERVICE_CONFIG_SAFETY_VALVE);
        assertThat(result.getFirst().getValue())
                .contains(ConfigUtils.getSafetyValveProperty("hive.metastore.uris", "thrift://dl"))
                .contains(ConfigUtils.getSafetyValveProperty("hive.exec.scratchdir", "hdfs://nshybrid/tmp/hive"));
    }

    private void testGetServiceConfigs(Iterable<String> versions, Iterable<ApiClusterTemplateConfig> expectedConfigs) {
        for (String version: versions) {
            when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable(version));
            TemplatePreparationObject tpo = setPlatfrormVariant(null);
            List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, tpo);
            assertThat(serviceConfigs).as("Expected configs for cdh version: %s", version).hasSameElementsAs(expectedConfigs);
        }
    }

    @Test
    void isConfigurationNeeded() {
        when(cmTemplateProcessor.isRoleTypePresentInService(anyString(), anyList())).thenReturn(true);
        TemplatePreparationObject tpo = Builder.builder()
                .withKerberosConfig(KerberosConfig.KerberosConfigBuilder.aKerberosConfig().build())
                .build();
        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo)).isTrue();
    }

    @Test
    void isConfigurationNotNeededWithRoleTypeNotPresent() {
        when(cmTemplateProcessor.isRoleTypePresentInService(anyString(), anyList())).thenReturn(false);
        TemplatePreparationObject tpo = Builder.builder()
                .withKerberosConfig(KerberosConfig.KerberosConfigBuilder.aKerberosConfig().build())
                .build();
        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo)).isFalse();
    }

    @Test
    void isConfigurationNotNeededWithRoleTypePresent() {
        lenient().when(cmTemplateProcessor.isRoleTypePresentInService(anyString(), anyList())).thenReturn(true);
        TemplatePreparationObject tpo = Builder.builder()
                .build();
        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo)).isFalse();
    }

    @Test
    void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).hasSameElementsAs(List.of(HiveRoles.HIVESERVER2));
    }

    @Test
    void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo(HiveRoles.HIVE_ON_TEZ);
    }
}
