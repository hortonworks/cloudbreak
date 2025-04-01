package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class YarnCloudStorageRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String NODEMANAGER_REMOTE_APP_LOG_DIR_TEMPLATE_PARAM = "yarn_nodemanager_remote_app_log_dir";

    private static final String NODEMANAGER_REMOTE_APP_LOG_DIR = "yarn.nodemanager.remote-app-log-dir";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case YarnRoles.NODEMANAGER:
                return ConfigUtils.getStorageLocationForServiceProperty(source, NODEMANAGER_REMOTE_APP_LOG_DIR)
                        .map(location -> List.of(config(NODEMANAGER_REMOTE_APP_LOG_DIR_TEMPLATE_PARAM, location.getValue())))
                        .orElseGet(List::of);
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
}