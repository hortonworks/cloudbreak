package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@ExtendWith(MockitoExtension.class)
public class KafkaConnectDataHubConfigProviderTest {

    @Mock
    private BlueprintView blueprintView;

    @Mock
    private CmTemplateProcessor processor;

    private final KafkaConnectDatahubConfigProvider provider = new KafkaConnectDatahubConfigProvider();

    @ParameterizedTest
    @MethodSource("validConfigurationParameters")
    void testRoleConfigsReturnedWithValidParameters(String cdhVersion, String roleType) {
        cdpMainVersionIs(cdhVersion);
        HostgroupView hostGroup = new HostgroupView("test");
        assertEquals(expectedConfigWithConnectAndCdhAtLeast7214(),
                provider.getRoleConfigs(roleType, processor, getTemplatePreparationObject(hostGroup)));
    }

    @ParameterizedTest
    @MethodSource("invalidConfigurationParameters")
    void testEmptyListReturnedWithInvalidParameters(String cdhVersion, String roleType) {
        cdpMainVersionIs(cdhVersion);
        HostgroupView hostGroup = new HostgroupView("test");
        assertEquals(List.of(),
                provider.getRoleConfigs(roleType, processor, getTemplatePreparationObject(hostGroup)));
    }

    @ParameterizedTest
    @EnumSource(StackType.class)
    void testIsConfigurationNeeded(StackType stackType) {
        TemplatePreparationObject tpo = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withStackType(stackType).build();
        boolean expectedIsNeeded = stackType == StackType.WORKLOAD;
        assertThat(provider.isConfigurationNeeded(null, tpo)).isEqualTo(expectedIsNeeded);
    }

    private static Stream<Arguments> validConfigurationParameters() {
        return Stream.of(
                Arguments.of("7.2.14", KafkaRoles.KAFKA_CONNECT),
                Arguments.of("7.2.15", KafkaRoles.KAFKA_CONNECT),
                Arguments.of("7.2.16", KafkaRoles.KAFKA_CONNECT)
        );
    }

    private static Stream<Arguments> invalidConfigurationParameters() {
        return Stream.of(
                Arguments.of("7.2.12", KafkaRoles.KAFKA_CONNECT),
                Arguments.of("7.2.12", KafkaRoles.KAFKA_BROKER),
                Arguments.of("7.2.14", KafkaRoles.KAFKA_BROKER),
                Arguments.of("7.2.15", KafkaRoles.KAFKA_BROKER)
        );
    }

    private void cdpMainVersionIs(String version) {
        when(blueprintView.getProcessor()).thenReturn(processor);
        when(processor.getStackVersion()).thenReturn(version);
    }

    private TemplatePreparationObject getTemplatePreparationObject(HostgroupView hostGroup) {
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(hostGroup))
                .withBlueprintView(blueprintView)
                .build();
        return preparationObject;
    }

    private List<ApiClusterTemplateConfig> expectedConfigWithConnectAndCdhAtLeast7214() {
        return List.of(
                config(KafkaConnectDatahubConfigProvider.RANGER_PLUGIN_KAFKA_CONNECT_SERVICE_NAME_CONFIG, KafkaConfigs.GENERATED_RANGER_SERVICE_NAME)
        );
    }
}
