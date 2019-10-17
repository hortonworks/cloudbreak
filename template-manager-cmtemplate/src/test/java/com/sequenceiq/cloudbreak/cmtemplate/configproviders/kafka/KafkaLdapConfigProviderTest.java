package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
class KafkaLdapConfigProviderTest {

    private static final String V7_0_1 = "7.0.1";

    private static final String V7_0_2 = "7.0.2";

    private KafkaLdapConfigProvider underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @BeforeEach
    void setUp() {
        underTest = new KafkaLdapConfigProvider();
    }

    @Test
    void getServiceConfigs() {
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.of(V7_0_2));

        TemplatePreparationObject tpo = Builder.builder()
                .withLdapConfig(LdapViewBuilder.aLdapView()
                        .withProtocol("protocol")
                        .withServerPort(1234)
                        .withServerHost("host")
                        .withUserDnPattern("pattern")
                        .build())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, tpo);

        ApiClusterTemplateConfig exp1 = new ApiClusterTemplateConfig();
        exp1.setName("ldap.auth.url");
        exp1.setValue("protocol://host:1234");

        ApiClusterTemplateConfig exp2 = new ApiClusterTemplateConfig();
        exp2.setName("ldap.auth.user.dn.template");
        exp2.setValue("pattern");

        ApiClusterTemplateConfig exp3 = new ApiClusterTemplateConfig();
        exp3.setName("ldap.auth.enable");
        exp3.setValue("true");

        assertThat(serviceConfigs).hasSameElementsAs(List.of(exp1, exp2, exp3));
    }

    @Test
    void getServiceConfigsWrongVersion() {
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.of(V7_0_1));

        TemplatePreparationObject tpo = Builder.builder()
                .withLdapConfig(LdapViewBuilder.aLdapView()
                        .withProtocol("protocol")
                        .withServerPort(1234)
                        .withServerHost("host")
                        .withUserDnPattern("pattern")
                        .build())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, tpo);
        assertThat(serviceConfigs).isEmpty();
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
