package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

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
public class ProfilerAdminRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case ProfilerManagerRoles.PROFILER_ADMIN_AGENT:
                RdsView profilerManagerRdsView = getRdsView(source);
                return List.of(config("profiler_admin_database_host", profilerManagerRdsView.getHost()),
                        config("profiler_admin_database_name", profilerManagerRdsView.getDatabaseName()),
                        config("profiler_admin_database_type", ConfigUtils.getDbTypePostgres(profilerManagerRdsView, ProfilerManagerRoles.PROFILER_MANAGER)),
                        config("profiler_admin_database_user", profilerManagerRdsView.getConnectionUserName()),
                        config("profiler_admin_database_password", profilerManagerRdsView.getConnectionPassword()));
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
        return List.of(ProfilerManagerRoles.PROFILER_ADMIN_AGENT);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.PROFILER_AGENT;
    }
}
