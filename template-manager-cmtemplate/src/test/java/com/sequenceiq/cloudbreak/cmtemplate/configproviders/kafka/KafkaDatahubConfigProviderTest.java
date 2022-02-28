package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtilsTest.cdhParcelVersion;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.GENERATED_RANGER_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.PRODUCER_METRICS_ENABLE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.RANGER_PLUGIN_KAFKA_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.KAFKA_DECOMMISSION_HOOK_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@ExtendWith(MockitoExtension.class)
class KafkaDatahubConfigProviderTest {

    private static final Set<ApiClusterTemplateConfig> CONFIG_WITHOUT_RANGER = Set.of(
            config(PRODUCER_METRICS_ENABLE, "true"));

    private static final Set<ApiClusterTemplateConfig> CONFIG_WITH_RANGER = Set.of(
            config(PRODUCER_METRICS_ENABLE, "true"),
            config(RANGER_PLUGIN_KAFKA_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME));

    private static final Set<ApiClusterTemplateConfig> CONFIG_WITH_KAFKA = Set.of(
            config(RANGER_PLUGIN_KAFKA_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME),
            config(KAFKA_DECOMMISSION_HOOK_ENABLED, "true"));

    private static final Set<ApiClusterTemplateConfig> CONFIG_WITHOUT_KAFKA = Set.of(
            config(RANGER_PLUGIN_KAFKA_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME));

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private BlueprintView blueprintView;

    private KafkaDatahubConfigProvider configProviderUnderTest;

    @BeforeEach
    void setUp() {
        configProviderUnderTest = new KafkaDatahubConfigProvider();
    }

    @ParameterizedTest
    @MethodSource("testArgsForGetServiceConfigs")
    void getServiceConfigs(String cdhMainVersion, String cdhParcelVersion, Collection<ApiClusterTemplateConfig> expectedConfigs) {
        when(blueprintView.getProcessor()).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(cdhMainVersion);
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.ofNullable(cdhMainVersion));
        TemplatePreparationObject tpo = templatePreparationObject(StackType.WORKLOAD, cdhParcelVersion);
        List<ApiClusterTemplateConfig> serviceConfigs = configProviderUnderTest.getServiceConfigs(cmTemplateProcessor, tpo);
        assertThat(serviceConfigs).as("Expected configs for cdh version: %s / %s", cdhMainVersion, cdhParcelVersion).hasSameElementsAs(expectedConfigs);
    }

    static Stream<Arguments> testArgsForGetServiceConfigs() {
        return Stream.of(
                Arguments.of("7.0.1", cdhParcelVersion("7.0.1", 5), CONFIG_WITHOUT_RANGER),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 0), CONFIG_WITHOUT_RANGER),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 2), CONFIG_WITH_RANGER),
                Arguments.of("7.0.2", cdhParcelVersion("7.0.2", 3), CONFIG_WITH_RANGER),
                Arguments.of("7.0.2", "irregularCdhVersion-123", CONFIG_WITHOUT_RANGER),
                Arguments.of("7.0.3", cdhParcelVersion("7.0.2", 0), CONFIG_WITHOUT_RANGER),
                Arguments.of("7.0.3", cdhParcelVersion("7.0.2", 3), CONFIG_WITHOUT_RANGER),
                Arguments.of("7.1.0", cdhParcelVersion("7.1.0", 0), CONFIG_WITH_RANGER),
                Arguments.of("7.2.11", cdhParcelVersion("7.2.11", 0), CONFIG_WITHOUT_KAFKA),
                Arguments.of("7.2.12", cdhParcelVersion("7.2.12", 0), CONFIG_WITH_KAFKA));
    }

    @ParameterizedTest
    @EnumSource(StackType.class)
    void isConfigurationNeeded(StackType stackType) {
        TemplatePreparationObject tpo = templatePreparationObject(stackType, null);
        boolean expectedIsNeeded = stackType == StackType.WORKLOAD;
        assertThat(configProviderUnderTest.isConfigurationNeeded(null, tpo))
                .as("Configuration should %sbe needed for stack type %s", expectedIsNeeded ? "" : "NOT ", stackType)
                .isEqualTo(expectedIsNeeded);
    }

    private TemplatePreparationObject templatePreparationObject(StackType stackType, String cdhVersion) {
        TemplatePreparationObject.Builder builder = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withStackType(stackType);
        if (cdhVersion != null) {
            List<ClouderaManagerProduct> products = KafkaConfigProviderUtilsTest.products("CDH=" + cdhVersion);
            builder.withProductDetails(new ClouderaManagerRepo(), products);
        }
        return builder.build();
    }
}