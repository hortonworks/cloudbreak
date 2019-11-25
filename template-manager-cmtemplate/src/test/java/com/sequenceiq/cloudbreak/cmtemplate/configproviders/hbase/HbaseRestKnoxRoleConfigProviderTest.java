package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseRestKnoxRoleConfigProvider.HBASE_RESTSERVER_CONFIG_SAFETY_VALVE;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;

@ExtendWith(MockitoExtension.class)
class HbaseRestKnoxRoleConfigProviderTest {

    private static final String V7_0_1 = "7.0.1";

    private static final String V7_0_2 = "7.0.2";

    private static final String V7_0_99 = "7.0.99";

    private static final String V7_1_0 = "7.1.0";

    private static final String V7_X_0 = "7.x.0";

    private static final Iterable<String> SAFETY_VALVE_VERSIONS = Lists.newArrayList(null, V7_0_1, V7_0_2, V7_0_99);

    private static final Iterable<String> NO_SAFETY_VALVE_VERSIONS = Set.of(V7_1_0, V7_X_0);

    private static final Set<ApiClusterTemplateConfig> SAFETY_VALVE_EXPECTED_CONFIGS = Set.of(
            config(HBASE_RESTSERVER_CONFIG_SAFETY_VALVE,
                    ConfigUtils.getSafetyValveProperty("hbase.rest.support.proxyuser", "true")));

    private static final Set<ApiClusterTemplateConfig> NO_SAFETY_VALVE_EXPECTED_CONFIGS = Set.of();

    private HbaseRestKnoxRoleConfigProvider underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @BeforeEach
    void setUp() {
        underTest = new HbaseRestKnoxRoleConfigProvider();
    }

    private static ApiClusterTemplateConfig config(String name, String value) {
        ApiClusterTemplateConfig cfg = new ApiClusterTemplateConfig();
        cfg.setName(name);
        cfg.setValue(value);
        return cfg;
    }

    private static TemplatePreparationObject createTemplatePreparationObject() {
        Gateway gateway = TestUtil.gatewayEnabledWithExposedKnoxServices(ExposedService.HBASE_REST.getKnoxService());
        return Builder.builder()
                .withGateway(gateway, "/cb/secret/signkey")
                .build();
    }

    @Test
    void getServiceConfigsSafetyValve() {
        testGetServiceConfigs(SAFETY_VALVE_VERSIONS, SAFETY_VALVE_EXPECTED_CONFIGS);
    }

    @Test
    void getServiceConfigsNoSafetyValve() {
        testGetServiceConfigs(NO_SAFETY_VALVE_VERSIONS, NO_SAFETY_VALVE_EXPECTED_CONFIGS);
    }

    private void testGetServiceConfigs(Iterable<String> versions, Iterable<ApiClusterTemplateConfig> expectedConfigs) {
        for (String version: versions) {
            when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable(version));
            TemplatePreparationObject tpo = createTemplatePreparationObject();
            if (underTest.isConfigurationNeeded(cmTemplateProcessor, tpo)) {
                assertThat(version).isIn(SAFETY_VALVE_VERSIONS);
                List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(HbaseRoles.HBASERESTSERVER, tpo);
                assertThat(roleConfigs).as("Expected configs for cdh version: %s", version).hasSameElementsAs(expectedConfigs);
            } else {
                assertThat(version).isIn(NO_SAFETY_VALVE_VERSIONS);
            }
        }
    }

    @Test
    void isConfigurationNeeded() {
        for (String version : SAFETY_VALVE_VERSIONS) {
            when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable(version));
            TemplatePreparationObject tpo = createTemplatePreparationObject();
            assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo)).isTrue();
        }
    }

    @Test
    void isConfigurationNotNeeded() {
        for (String version : NO_SAFETY_VALVE_VERSIONS) {
            when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable(version));
            TemplatePreparationObject tpo = createTemplatePreparationObject();
            assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo)).isFalse();
        }
    }

    @Test
    void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).hasSameElementsAs(List.of(HbaseRoles.HBASERESTSERVER));
    }

    @Test
    void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo("HBASE");
    }
}
