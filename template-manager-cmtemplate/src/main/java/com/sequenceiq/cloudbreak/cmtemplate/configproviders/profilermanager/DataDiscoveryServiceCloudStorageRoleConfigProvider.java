package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class DataDiscoveryServiceCloudStorageRoleConfigProvider extends AbstractRoleConfigProvider {
    private static final String FILE_SYSTEM_URI = "file_system_uri";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor,
            TemplatePreparationObject templatePreparationObject) {
        switch (roleType) {
            case ProfilerManagerRoles.DATA_DISCOVERY_SERVICE_AGENT:
                return ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, FILE_SYSTEM_URI)
                        .map(location -> List.of(config(FILE_SYSTEM_URI, location.getValue())))
                        .orElseGet(List::of);
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return ProfilerManagerRoles.PROFILER_MANAGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ProfilerManagerRoles.DATA_DISCOVERY_SERVICE_AGENT);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
