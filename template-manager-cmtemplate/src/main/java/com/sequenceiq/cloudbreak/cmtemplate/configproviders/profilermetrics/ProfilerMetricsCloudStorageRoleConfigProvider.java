package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermetrics;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

@Component
public class ProfilerMetricsCloudStorageRoleConfigProvider extends AbstractRoleConfigProvider {
    private static final String FILE_SYSTEM_URI = "file_system_uri";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject templatePreparationObject) {
        switch (roleType) {
            case ProfilerMetricsRoles.PROFILER_METRICS_AGENT:
                return ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, FILE_SYSTEM_URI)
                        .map(location -> List.of(config(FILE_SYSTEM_URI, location.getValue())))
                        .orElseGet(List::of);
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return ProfilerMetricsRoles.PROFILER_METRICS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ProfilerMetricsRoles.PROFILER_METRICS_AGENT);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}