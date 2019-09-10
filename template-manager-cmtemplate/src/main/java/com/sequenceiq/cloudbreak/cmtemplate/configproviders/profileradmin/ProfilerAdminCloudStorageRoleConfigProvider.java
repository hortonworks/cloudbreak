package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profileradmin;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

@Component
public class ProfilerAdminCloudStorageRoleConfigProvider extends AbstractRoleConfigProvider {
    private static final String FILE_SYSTEM_URI = "file_system_uri";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject templatePreparationObject) {
        switch (roleType) {
            case ProfilerAdminRoles.PROFILER_ADMIN_AGENT:
                return ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, FILE_SYSTEM_URI)
                        .map(location -> List.of(config(FILE_SYSTEM_URI, location.getValue())))
                        .orElseGet(List::of);
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return ProfilerAdminRoles.PROFILER_ADMIN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ProfilerAdminRoles.PROFILER_ADMIN_AGENT);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
