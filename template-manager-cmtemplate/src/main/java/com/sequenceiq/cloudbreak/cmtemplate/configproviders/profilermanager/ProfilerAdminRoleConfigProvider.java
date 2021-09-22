package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

@Component
public class ProfilerAdminRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    @VisibleForTesting
    static final String PROFILER_ADMIN_DATABASE_HOST = "profiler_admin_database_host";

    @VisibleForTesting
    static final String PROFILER_ADMIN_DATABASE_NAME = "profiler_admin_database_name";

    @VisibleForTesting
    static final String PROFILER_ADMIN_DATABASE_TYPE = "profiler_admin_database_type";

    @VisibleForTesting
    static final String PROFILER_ADMIN_DATABASE_USER = "profiler_admin_database_user";

    @VisibleForTesting
    static final String PROFILER_ADMIN_DATABASE_PASSWORD = "profiler_admin_database_password";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        if (ProfilerManagerRoles.PROFILER_ADMIN_AGENT.equals(roleType)) {
            RdsView profilerManagerRdsView = getRdsView(source);
            return List.of(config(PROFILER_ADMIN_DATABASE_HOST, profilerManagerRdsView.getHost()),
                    config(PROFILER_ADMIN_DATABASE_NAME, profilerManagerRdsView.getDatabaseName()),
                    config(PROFILER_ADMIN_DATABASE_TYPE, ConfigUtils.getDbTypePostgres(profilerManagerRdsView, ProfilerManagerRoles.PROFILER_MANAGER)),
                    config(PROFILER_ADMIN_DATABASE_USER, profilerManagerRdsView.getConnectionUserName()),
                    config(PROFILER_ADMIN_DATABASE_PASSWORD, profilerManagerRdsView.getConnectionPassword()));
        }
        return List.of();
    }

    @Override
    public String getServiceType() {
        return ProfilerManagerRoles.PROFILER_MANAGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ProfilerManagerRoles.PROFILER_ADMIN_AGENT);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.PROFILER_AGENT;
    }
}
