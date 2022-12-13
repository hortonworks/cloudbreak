package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class ProfilerMetricsRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    public static final String PROFILER_METRICS_DATABASE_HOST = "profiler_metrics_database_host";

    public static final String PROFILER_METRICS_DATABASE_NAME = "profiler_metrics_database_name";

    public static final String PROFILER_METRICS_DATABASE_TYPE = "profiler_metrics_database_type";

    public static final String PROFILER_METRICS_DATABASE_USER = "profiler_metrics_database_user";

    public static final String PROFILER_METRICS_DATABASE_PASSWORD = "profiler_metrics_database_password";

    public static final String PROFILER_METRICS_DATABASE_JDBC_URL_OVERRIDE = "profiler_metrics_database_jdbc_url_override";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        if (ProfilerManagerRoles.PROFILER_METRICS_AGENT.equals(roleType)) {
            RdsView profilerManagerRdsView = getRdsView(source);
            configs.add(config(PROFILER_METRICS_DATABASE_USER, profilerManagerRdsView.getConnectionUserName()));
            configs.add(config(PROFILER_METRICS_DATABASE_PASSWORD, profilerManagerRdsView.getConnectionPassword()));
            configs.add(config(PROFILER_METRICS_DATABASE_HOST, profilerManagerRdsView.getHost()));
            if (profilerManagerRdsView.isUseSsl()) {
                configs.add(config(PROFILER_METRICS_DATABASE_JDBC_URL_OVERRIDE, profilerManagerRdsView.getConnectionURL()));
            } else {
                configs.add(config(PROFILER_METRICS_DATABASE_NAME, profilerManagerRdsView.getDatabaseName()));
                configs.add(config(PROFILER_METRICS_DATABASE_TYPE,
                        ConfigUtils.getDbTypePostgres(profilerManagerRdsView, ProfilerManagerRoles.PROFILER_MANAGER)));
            }
        }
        return configs;
    }

    @Override
    public String getServiceType() {
        return ProfilerManagerRoles.PROFILER_MANAGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ProfilerManagerRoles.PROFILER_METRICS_AGENT);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.PROFILER_METRIC;
    }
}
