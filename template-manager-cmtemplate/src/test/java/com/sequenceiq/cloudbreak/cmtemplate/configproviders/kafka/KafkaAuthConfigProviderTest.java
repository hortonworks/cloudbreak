package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
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
import com.sequenceiq.cloudbreak.dto.LdapView.LdapViewBuilder;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;

@ExtendWith(MockitoExtension.class)
class KafkaAuthConfigProviderTest {

    private static final String V7_0_1 = "7.0.1";

    private static final String V7_0_2 = "7.0.2";

    private static final String V7_0_99 = "7.0.99";

    private static final String V7_1_0 = "7.1.0";

    private static final String V7_X_0 = "7.x.0";

    private static final Iterable<String> NO_AUTH_VERSIONS = Lists.newArrayList(null, V7_0_1);

    private static final Iterable<String> LDAP_AUTH_VERSIONS = Set.of(V7_0_2, V7_0_99);

    private static final Iterable<String> PAM_AUTH_VERSIONS = Set.of(V7_1_0, V7_X_0);

    private static final Set<ApiClusterTemplateConfig> NO_AUTH_EXPECTED_CONFIGS = Set.of();

    private static final Set<ApiClusterTemplateConfig> LDAP_AUTH_EXPECTED_CONFIGS = Set.of(
            config("ldap.auth.url", "protocol://host:1234"),
            config("ldap.auth.user.dn.template", "pattern"),
            config("ldap.auth.enable", "true"));

    private static final Set<ApiClusterTemplateConfig> PAM_AUTH_EXPECTED_CONFIGS = Set.of(
            config("ldap.auth.url", "protocol://host:1234"),
            config("ldap.auth.user.dn.template", "pattern"),
            config("sasl.plain.auth", "PAM"));

    private KafkaAuthConfigProvider underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @BeforeEach
    void setUp() {
        underTest = new KafkaAuthConfigProvider();
    }

    private static TemplatePreparationObject createTemplatePreparationObject() {
        return Builder.builder()
                .withLdapConfig(LdapViewBuilder.aLdapView()
                        .withProtocol("protocol")
                        .withServerPort(1234)
                        .withServerHost("host")
                        .withUserDnPattern("pattern")
                        .build())
                .build();
    }

    @Test
    void getServiceConfigsNoAuth() {
        testGetServiceConfigs(NO_AUTH_VERSIONS, NO_AUTH_EXPECTED_CONFIGS);
    }

    @Test
    void getServiceConfigsLdap() {
        testGetServiceConfigs(LDAP_AUTH_VERSIONS, LDAP_AUTH_EXPECTED_CONFIGS);
    }

    @Test
    void getServiceConfigsPam() {
        testGetServiceConfigs(PAM_AUTH_VERSIONS, PAM_AUTH_EXPECTED_CONFIGS);
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
                .withLdapConfig(LdapViewBuilder.aLdapView().build())
                .build();
        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo)).isTrue();
    }

    @Test
    void isConfigurationNotNeededWithRoleTypeNotPresent() {
        when(cmTemplateProcessor.isRoleTypePresentInService(anyString(), anyList())).thenReturn(false);
        TemplatePreparationObject tpo = Builder.builder()
                .withLdapConfig(LdapViewBuilder.aLdapView().build())
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
        assertThat(underTest.getRoleTypes()).hasSameElementsAs(List.of("KAFKA_BROKER"));
    }

    @Test
    void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo("KAFKA");
    }
}
