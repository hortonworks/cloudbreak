package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class KafkaDatahubConfigProviderTest {

    private KafkaDatahubConfigProvider configProvider;

    @BeforeEach
    void setUp() {
        configProvider = new KafkaDatahubConfigProvider();
    }

    @Test
    void getServiceConfigs() {
        List<ApiClusterTemplateConfig> expectedConfigs = List.of(config("producer.metrics.enable", "true"));
        List<ApiClusterTemplateConfig> serviceConfigs = configProvider.getServiceConfigs(null, templatePreparationObject());
        assertThat(serviceConfigs).hasSameElementsAs(expectedConfigs);
    }

    @ParameterizedTest
    @EnumSource(StackType.class)
    void isConfigurationNeeded(StackType stackType) {
        TemplatePreparationObject tpo = TemplatePreparationObject.Builder.builder()
                .withStackType(stackType)
                .build();
        boolean expectedIsNeeded = stackType == StackType.WORKLOAD;
        assertThat(configProvider.isConfigurationNeeded(null, tpo))
                .as("Configuration should %sbe needed for stack type %s", expectedIsNeeded ? "" : "NOT ", stackType)
                .isEqualTo(expectedIsNeeded);
    }

    private TemplatePreparationObject templatePreparationObject() {
        return TemplatePreparationObject.Builder.builder().build();
    }
}