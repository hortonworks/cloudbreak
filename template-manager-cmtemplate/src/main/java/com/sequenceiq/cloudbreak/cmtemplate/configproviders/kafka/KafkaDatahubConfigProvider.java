package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtils.CdhVersionForStreaming.VERSION_7_0_2_2_OR_LATER;

@Component
public class KafkaDatahubConfigProvider implements CmTemplateComponentConfigProvider {

    static final String RANGER_PLUGIN_KAFKA_SERVICE_NAME = "ranger_plugin_kafka_service_name";

    static final String GENERATED_RANGER_SERVICE_NAME = "{{GENERATED_RANGER_SERVICE_NAME}}";

    static final String PRODUCER_METRICS_ENABLE = "producer.metrics.enable";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        ArrayList<ApiClusterTemplateConfig> configs = Lists.newArrayList(config(PRODUCER_METRICS_ENABLE, "true"));
        if (supportsRangerServiceCreation(source)) {
            configs.add(config(RANGER_PLUGIN_KAFKA_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME));
        }
        return configs;
    }

    private boolean supportsRangerServiceCreation(TemplatePreparationObject source) {
        return VERSION_7_0_2_2_OR_LATER.equals(KafkaConfigProviderUtils.getCdhVersionForStreaming(source));
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.WORKLOAD.equals(source.getStackType());
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KafkaRoles.KAFKA_BROKER);
    }

    @Override
    public String getServiceType() {
        return KafkaRoles.KAFKA_SERVICE;
    }

}