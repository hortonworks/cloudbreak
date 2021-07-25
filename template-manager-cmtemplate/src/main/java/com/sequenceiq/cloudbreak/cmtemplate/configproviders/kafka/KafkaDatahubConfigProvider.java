package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_12;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class KafkaDatahubConfigProvider implements CmTemplateComponentConfigProvider {

    public static final String RANGER_PLUGIN_KAFKA_SERVICE_NAME = "ranger_plugin_kafka_service_name";

    public static final String GENERATED_RANGER_SERVICE_NAME = "{{GENERATED_RANGER_SERVICE_NAME}}";

    static final String PRODUCER_METRICS_ENABLE = "producer.metrics.enable";

    static final String KAFKA_DECOMMISSION_HOOK_ENABLED = "kafka.decommission.hook.enabled";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        ArrayList<ApiClusterTemplateConfig> configs = Lists.newArrayList();
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (!isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
            configs.add(config(PRODUCER_METRICS_ENABLE, "true"));
        }
        if (KafkaConfigProviderUtils.getCdhVersionForStreaming(source).supportsRangerServiceCreation()) {
            configs.add(config(RANGER_PLUGIN_KAFKA_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME));
        }
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_12)) {
            configs.add(config(KAFKA_DECOMMISSION_HOOK_ENABLED, "true"));
        }
        return configs;
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