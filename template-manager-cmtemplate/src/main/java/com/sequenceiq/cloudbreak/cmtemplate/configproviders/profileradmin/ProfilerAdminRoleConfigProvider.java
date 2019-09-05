package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profileradmin;

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
            case ProfilerAdminRoles.PROFILER_ADMIN_AGENT:
                RdsView profilerAdminRdsView = getRdsView(source);
                return List.of(config("profiler_admin_database_host", profilerAdminRdsView.getHost()),
                        config("profiler_admin_database_name", profilerAdminRdsView.getDatabaseName()),
                        config("profiler_admin_database_type", ConfigUtils.getDbTypePostgres(profilerAdminRdsView, ProfilerAdminRoles.PROFILER_ADMIN)),
                        config("profiler_admin_database_user",
                                profilerAdminRdsView.getConnectionUserName()),
                        config("profiler_admin_database_password", profilerAdminRdsView.getConnectionPassword()));
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
    protected DatabaseType dbType() {
        return DatabaseType.PROFILER_AGENT;
    }
}
