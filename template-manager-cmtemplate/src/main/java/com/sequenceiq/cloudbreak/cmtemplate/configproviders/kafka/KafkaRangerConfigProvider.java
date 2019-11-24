package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class KafkaRangerConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String RANGER_PLUGIN_KAFKA_SERVICE_NAME = "ranger.plugin.kafka.service.name";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cdhVersion = templateProcessor.getVersion().orElse("");
        List<ApiClusterTemplateConfig> apiClusterTemplateConfigs = new ArrayList<>();

        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_0)
                && isConfigurationNeeded(templateProcessor, source)) {
            apiClusterTemplateConfigs.add(config(RANGER_PLUGIN_KAFKA_SERVICE_NAME,
                    String.format("cm-%s", source.getGeneralClusterConfigs().getStackName())));
        }
        return apiClusterTemplateConfigs;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.WORKLOAD.equals(source.getStackType())
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
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
