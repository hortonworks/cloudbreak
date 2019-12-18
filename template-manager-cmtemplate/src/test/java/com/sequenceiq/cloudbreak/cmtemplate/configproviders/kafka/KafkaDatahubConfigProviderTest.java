package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtilsTest.cdhParcelVersion;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaDatahubConfigProvider.GENERATED_RANGER_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaDatahubConfigProvider.PRODUCER_METRICS_ENABLE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaDatahubConfigProvider.RANGER_PLUGIN_KAFKA_SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaDatahubConfigProviderTest {

    private static final Set<ApiClusterTemplateConfig> CONFIG_WITHOUT_RANGER = Set.of(
            config(PRODUCER_METRICS_ENABLE, "true"));

    private static final Set<ApiClusterTemplateConfig> CONFIG_WITH_RANGER = Set.of(
            config(PRODUCER_METRICS_ENABLE, "true"),
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
                Arguments.of("7.1.0", cdhParcelVersion("7.1.0", 0), CONFIG_WITH_RANGER));
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