package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtilsTest.cdhParcelVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.LdapView.LdapViewBuilder;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;

@ExtendWith(MockitoExtension.class)
class KafkaAuthConfigProviderTest {

    private static final Set<ApiClusterTemplateConfig> NO_AUTH_EXPECTED_CONFIGS = Set.of();

    private static final Set<ApiClusterTemplateConfig> GENERAL_AUTH_EXPECTED_CONFIGS = Set.of(
            config("ldap.auth.url", "protocol://host:1234"),
            config("ldap.auth.user.dn.template", "pattern"));

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

    @Mock
    private BlueprintView blueprintView;

    @BeforeEach
    void setUp() {
        underTest = new KafkaAuthConfigProvider();
    }

    private static LdapView ldapView() {
        return LdapViewBuilder.aLdapView()
                .withProtocol("protocol")
                .withServerPort(1234)
                .withServerHost("host")
                .withUserDnPattern("pattern")
                .build();
    }

    private TemplatePreparationObject templatePreparationObject(StackType stackType, String cdhVersion, boolean addLdapView) {
        Builder builder = Builder.builder()
                .withBlueprintView(blueprintView)
                .withLdapConfig(addLdapView ? ldapView() : null)
                .withStackType(stackType);
        if (cdhVersion != null) {
            List<ClouderaManagerProduct> products = KafkaConfigProviderUtilsTest.products("CDH=" + cdhVersion);
            builder.withProductDetails(new ClouderaManagerRepo(), products);
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("testArgsForGetServiceConfigs")
    void getServiceConfigs(String cdhMainVersion, String cdhParcelVersion, Collection<ApiClusterTemplateConfig> expectedConfigs) {
        when(blueprintView.getProcessor()).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable(cdhMainVersion));
        TemplatePreparationObject tpo = templatePreparationObject(StackType.WORKLOAD, cdhParcelVersion, true);
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, tpo);
        assertThat(serviceConfigs).as("Expected configs for cdh version: %s / %s", cdhMainVersion, cdhParcelVersion).hasSameElementsAs(expectedConfigs);
    }

    static Stream<Arguments> testArgsForGetServiceConfigs() {
        return Stream.of(
                Arguments.of("7.0.1", cdhParcelVersion("7.0.1", 5), NO_AUTH_EXPECTED_CONFIGS),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 0), LDAP_AUTH_EXPECTED_CONFIGS),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 2), PAM_AUTH_EXPECTED_CONFIGS),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 3), PAM_AUTH_EXPECTED_CONFIGS),
                Arguments.of("7.0.2", "irregularCdhVersion-123", GENERAL_AUTH_EXPECTED_CONFIGS),
                Arguments.of("7.1.0", cdhParcelVersion("7.1.0", 0), PAM_AUTH_EXPECTED_CONFIGS));
    }

    @ParameterizedTest
    @MethodSource("testArgsForIsConfigurationNeeded")
    void isConfigurationNeeded(StackType stackType, Boolean ldapConfigPresent, Boolean stackContainsKafkaBroker, Boolean expectedResult) {
        TemplatePreparationObject tpo = templatePreparationObject(stackType, null, ldapConfigPresent);
        lenient().when(cmTemplateProcessor.isRoleTypePresentInService(anyString(), anyList())).thenReturn(stackContainsKafkaBroker);
        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, tpo))
                .as("Configuration should %sbe needed for stack type: %s, LDAP configPresent: %s, Kafka in stack: %s",
                        expectedResult ? "" : "NOT ", stackType, ldapConfigPresent, stackContainsKafkaBroker)
                .isEqualTo(expectedResult);
    }

    static Stream<Arguments> testArgsForIsConfigurationNeeded() {
        return Stream.of(
                Arguments.of(StackType.DATALAKE, true, true, false),
                Arguments.of(StackType.DATALAKE, false, true, false),
                Arguments.of(StackType.DATALAKE, true, false, false),
                Arguments.of(StackType.DATALAKE, false, false, false),
                Arguments.of(StackType.TEMPLATE, true, true, false),
                Arguments.of(StackType.TEMPLATE, false, true, false),
                Arguments.of(StackType.TEMPLATE, true, false, false),
                Arguments.of(StackType.TEMPLATE, false, false, false),
                Arguments.of(StackType.WORKLOAD, true, true, true),
                Arguments.of(StackType.WORKLOAD, false, true, false),
                Arguments.of(StackType.WORKLOAD, true, false, false),
                Arguments.of(StackType.WORKLOAD, false, false, false));
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
