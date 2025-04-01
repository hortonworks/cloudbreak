package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtilsTest.cdhParcelVersion;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.DEFAULT_REPLICATION_FACTOR;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.ENABLE_RACK_AWARENESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@ExtendWith(MockitoExtension.class)
public class KafkaMultiAzConfigProviderTest {

    private static final Set<ApiClusterTemplateConfig> SERVICE_CONFIG_WITH_KAFKA_7_2_14 = Set.of(
            config(DEFAULT_REPLICATION_FACTOR, "3"));

    private static final Set<ApiClusterTemplateConfig> ROLE_CONFIG_WITH_KAFKA_7_2_14 = Set.of(
            config(ENABLE_RACK_AWARENESS, "true"));

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private BlueprintView blueprintView;

    private KafkaMultiAzConfigProvider configProviderUnderTest;

    @BeforeEach
    void setUp() {
        configProviderUnderTest = new KafkaMultiAzConfigProvider();
    }

    @ParameterizedTest
    @MethodSource("testArgsForGetServiceConfigs")
    void getServiceConfigs(String cdhMainVersion, String cdhParcelVersion, Collection<ApiClusterTemplateConfig> expectedConfigs) {
        when(blueprintView.getProcessor()).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(cdhMainVersion);
        TemplatePreparationObject tpo = templatePreparationObject(StackType.DATALAKE, cdhParcelVersion);
        List<ApiClusterTemplateConfig> serviceConfigs = configProviderUnderTest.getServiceConfigs(cmTemplateProcessor, tpo);
        assertThat(serviceConfigs).as("Expected configs for cdh version: %s / %s", cdhMainVersion, cdhParcelVersion).hasSameElementsAs(expectedConfigs);
    }

    @ParameterizedTest
    @MethodSource("testArgsForGetRoleConfigs")
    void getRoleConfigs(String cdhMainVersion, String cdhParcelVersion, Collection<ApiClusterTemplateConfig> expectedConfigs) {
        when(blueprintView.getProcessor()).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(cdhMainVersion);
        TemplatePreparationObject tpo = templatePreparationObject(StackType.DATALAKE, cdhParcelVersion);
        List<ApiClusterTemplateConfig> serviceConfigs = configProviderUnderTest.getRoleConfigs(KafkaRoles.KAFKA_BROKER, cmTemplateProcessor, tpo);
        assertThat(serviceConfigs).as("Expected configs for cdh version: %s / %s", cdhMainVersion, cdhParcelVersion).hasSameElementsAs(expectedConfigs);
    }

    static Stream<Arguments> testArgsForGetServiceConfigs() {
        return Stream.of(
                Arguments.of("7.2.14", cdhParcelVersion("7.2.14", 0), SERVICE_CONFIG_WITH_KAFKA_7_2_14));
    }

    static Stream<Arguments> testArgsForGetRoleConfigs() {
        return Stream.of(
                Arguments.of("7.2.14", cdhParcelVersion("7.2.14", 0), ROLE_CONFIG_WITH_KAFKA_7_2_14));
    }

    @ParameterizedTest
    @EnumSource(StackType.class)
    void isConfigurationNeeded(StackType stackType) {
        TemplatePreparationObject tpo = templatePreparationObject(stackType, null);
        boolean expectedIsNeeded = stackType == StackType.DATALAKE || stackType == StackType.WORKLOAD;
        assertThat(configProviderUnderTest.isConfigurationNeeded(null, tpo))
                .as("Configuration should %sbe needed for stack type %s", expectedIsNeeded ? "" : "NOT ", stackType)
                .isEqualTo(expectedIsNeeded);
    }

    private TemplatePreparationObject templatePreparationObject(StackType stackType, String cdhVersion) {
        GeneralClusterConfigs configs = new GeneralClusterConfigs();
        configs.setMultiAzEnabled(true);
        TemplatePreparationObject.Builder builder = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withStackType(stackType)
                .withGeneralClusterConfigs(configs);
        if (cdhVersion != null) {
            List<ClouderaManagerProduct> products = KafkaConfigProviderUtilsTest.products("CDH=" + cdhVersion);
            builder.withProductDetails(new ClouderaManagerRepo(), products);
        }
        return builder.build();
    }
}
