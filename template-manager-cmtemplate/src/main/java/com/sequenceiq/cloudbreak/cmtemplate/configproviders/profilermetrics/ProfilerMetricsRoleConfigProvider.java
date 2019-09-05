package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermetrics;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

@Component
public class ProfilerMetricsRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case ProfilerMetricsRoles.PROFILER_METRICS_AGENT:
                RdsView profilerMetricsRdsView = getRdsView(source);
                return List.of(config("profiler_metrics_database_host", profilerMetricsRdsView.getHost()),
                        config("profiler_metrics_database_name", profilerMetricsRdsView.getDatabaseName()),
                        config("profiler_metrics_database_type", ConfigUtils.getDbTypePostgres(profilerMetricsRdsView, ProfilerMetricsRoles.PROFILER_METRICS)),
                        config("profiler_metrics_database_user",
                                profilerMetricsRdsView.getConnectionUserName()),
                        config("profiler_metrics_database_password", profilerMetricsRdsView.getConnectionPassword()));
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
    protected DatabaseType dbType() {
        return DatabaseType.PROFILER_METRIC;
    }
}
