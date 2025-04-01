package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class ProfilerAdminRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    public static final String PROFILER_ADMIN_DATABASE_HOST = "profiler_admin_database_host";

    public static final String PROFILER_ADMIN_DATABASE_NAME = "profiler_admin_database_name";

    public static final String PROFILER_ADMIN_DATABASE_TYPE = "profiler_admin_database_type";

    public static final String PROFILER_ADMIN_DATABASE_USER = "profiler_admin_database_user";

    public static final String PROFILER_ADMIN_DATABASE_PASSWORD = "profiler_admin_database_password";

    public static final String PROFILER_ADMIN_DATABASE_JDBC_URL_OVERRIDE = "profiler_admin_database_jdbc_url_override";

    @Override
    public String dbUserKey() {
        return PROFILER_ADMIN_DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return PROFILER_ADMIN_DATABASE_PASSWORD;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        if (ProfilerManagerRoles.PROFILER_ADMIN_AGENT.equals(roleType)) {
            String cmVersion = ConfigUtils.getCmVersion(source);
            RdsView profilerManagerRdsView = getRdsView(source);
            configs.add(config(PROFILER_ADMIN_DATABASE_USER, profilerManagerRdsView.getConnectionUserName()));
            configs.add(config(PROFILER_ADMIN_DATABASE_PASSWORD, profilerManagerRdsView.getConnectionPassword()));
            configs.add(config(PROFILER_ADMIN_DATABASE_HOST, profilerManagerRdsView.getHost()));
            if (profilerManagerRdsView.isUseSsl()
                    && CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_2)) {
                configs.add(config(PROFILER_ADMIN_DATABASE_JDBC_URL_OVERRIDE, profilerManagerRdsView.getConnectionURL()));
            } else {
                configs.add(config(PROFILER_ADMIN_DATABASE_NAME, profilerManagerRdsView.getDatabaseName()));
                configs.add(config(PROFILER_ADMIN_DATABASE_TYPE,
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
        return List.of(ProfilerManagerRoles.PROFILER_ADMIN_AGENT);
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.PROFILER_AGENT;
    }
}
