package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class YarnCloudStorageRoleConfigProvider extends AbstractRoleConfigConfigProvider {

    private static final String NODEMANAGER_CONFIG_SAFETY_VALVE = "nodemanager_config_safety_valve";

    private static final String NODEMANAGER_REMOTE_APP_LOG_DIR = "yarn.nodemanager.remote-app-log-dir";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case YarnRoles.NODEMANAGER:
                String cloudStorageProperty = getCloudStorageProperty(source);
                if (!cloudStorageProperty.isEmpty()) {
                    return List.of(config(NODEMANAGER_CONFIG_SAFETY_VALVE, cloudStorageProperty));
                }
                return List.of();
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return YarnRoles.YARN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(YarnRoles.NODEMANAGER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    protected String getCloudStorageProperty(TemplatePreparationObject source) {
        StringBuilder yarnCloudStorage = new StringBuilder();
        ConfigUtils.getStorageLocationForServiceProperty(source, NODEMANAGER_REMOTE_APP_LOG_DIR).ifPresent(
                storageLocation -> yarnCloudStorage.append(
                        ConfigUtils.getSafetyValveProperty(NODEMANAGER_REMOTE_APP_LOG_DIR, storageLocation.getValue()))
        );
        return yarnCloudStorage.toString();
    }
}