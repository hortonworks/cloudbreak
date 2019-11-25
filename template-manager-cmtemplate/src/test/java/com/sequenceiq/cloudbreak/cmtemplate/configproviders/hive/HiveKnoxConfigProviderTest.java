package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveKnoxConfigProvider.HIVE_SERVICE_CONFIG_SAFETY_VALVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;

@ExtendWith(MockitoExtension.class)
class HiveKnoxConfigProviderTest {

    private static final String V7_0_1 = "7.0.1";

    private static final String V7_0_2 = "7.0.2";

    private static final String V7_0_99 = "7.0.99";

    private static final String V7_1_0 = "7.1.0";

    private static final String V7_X_0 = "7.x.0";

    private static final Iterable<String> KERBEROS_SAFETY_VALVE_VERSIONS = Lists.newArrayList(null, V7_0_1, V7_0_2, V7_0_99);

    private static final Iterable<String> NO_KERBEROS_SAFETY_VALVE_VERSIONS = Set.of(V7_1_0, V7_X_0);

    private static final Set<ApiClusterTemplateConfig> KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS = Set.of(
            config(HIVE_SERVICE_CONFIG_SAFETY_VALVE,
                    ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.principal", "HTTP/_HOST@EXAMPLE.COM") +
                            ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.keytab", "hive.keytab") +
                            ConfigUtils.getSafetyValveProperty("hive.hook.proto.file.per.event", "true")));

    private static final Set<ApiClusterTemplateConfig> NO_KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS = Set.of(
            config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty("hive.hook.proto.file.per.event", "true")));

    private HiveKnoxConfigProvider underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @BeforeEach
    void setUp() {
        underTest = new HiveKnoxConfigProvider();
    }

    private static ApiClusterTemplateConfig config(String name, String value) {
        ApiClusterTemplateConfig cfg = new ApiClusterTemplateConfig();
        cfg.setName(name);
        cfg.setValue(value);
        return cfg;
    }

    private static TemplatePreparationObject createTemplatePreparationObject() {
        return Builder.builder()
                .withKerberosConfig(KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                        .withRealm("EXAMPLE.COM")
                        .build())
                .build();
    }

    @Test
    void getServiceConfigsKerberosSafetyValve() {
        testGetServiceConfigs(KERBEROS_SAFETY_VALVE_VERSIONS, KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS);
    }

    @Test
    void getServiceConfigsNoKerberosSafetyValve() {
        testGetServiceConfigs(NO_KERBEROS_SAFETY_VALVE_VERSIONS, NO_KERBEROS_SAFETY_VALVE_EXPECTED_CONFIGS);
    }

    private void testGetServiceConfigs(Iterable<String> versions, Iterable<ApiClusterTemplateConfig> expectedConfigs) {
        for (String version: versions) {
            when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable(version));
            TemplatePreparationObject tpo = createTemplatePreparationObject();
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
