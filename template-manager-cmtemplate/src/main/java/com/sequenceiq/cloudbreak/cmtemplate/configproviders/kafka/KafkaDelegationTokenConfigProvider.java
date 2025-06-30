package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class KafkaDelegationTokenConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String CONFIG_VALUE = "true";

    private static final int MINIMUM_KRAFT_NODECOUNT = 0;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        if (kraftRolePresent(source)) {
            return List.of(config(KafkaConfigs.DELEGATION_TOKEN_ENABLE, CONFIG_VALUE));
        }
        return List.of();
    }

    @Override
    public String getServiceType() {
        return KafkaRoles.KAFKA_SERVICE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KafkaRoles.KAFKA_BROKER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return isKerberosEnabled(source)
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private boolean isKerberosEnabled(TemplatePreparationObject source) {
        return source.getKerberosConfig().isPresent();
    }

    private boolean kraftRolePresent(TemplatePreparationObject source) {
        Optional<HostgroupView> kraftHostGroup = source.getHostGroupsWithComponent(KafkaRoles.KAFKA_KRAFT).findFirst();
        return kraftHostGroup.isPresent() && kraftHostGroup.get().getNodeCount() > MINIMUM_KRAFT_NODECOUNT;
    }
}