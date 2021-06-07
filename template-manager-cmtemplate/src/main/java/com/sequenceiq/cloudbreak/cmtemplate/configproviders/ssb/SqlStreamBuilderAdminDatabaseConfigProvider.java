package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ssb;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.StreamingAppRdsRoleConfigProviderUtil.dataBaseTypeForCM;
import static java.util.Collections.emptyList;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class SqlStreamBuilderAdminDatabaseConfigProvider extends SqlStreamBuilderConfigProvider {

    static final String DATABASE_TYPE = "database_type";

    static final String DATABASE_HOST = "database_host";

    static final String DATABASE_PORT = "database_port";

    static final String DATABASE_SCHEMA = "database_schema";

    static final String DATABASE_USER = "database_user";

    static final String DATABASE_PASSWORD = "database_password";

    @Override
    public String getServiceType() {
        return SqlStreamBuilderRoles.SQL_STREAM_BUILDER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(SqlStreamBuilderRoles.STREAMING_SQL_ENGINE, SqlStreamBuilderRoles.STREAMING_SQL_CONSOLE, SqlStreamBuilderRoles.MATERIALIZED_VIEW_ENGINE);
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView ssbRdsView = getRdsView(source);

        return List.of(
                config(DATABASE_TYPE, dataBaseTypeForCM(ssbRdsView.getDatabaseVendor())),
                config(DATABASE_HOST, ssbRdsView.getHost()),
                config(DATABASE_PORT, ssbRdsView.getPort()),
                config(DATABASE_SCHEMA, ssbRdsView.getDatabaseName()),
                config(DATABASE_USER, ssbRdsView.getConnectionUserName()),
                config(DATABASE_PASSWORD, ssbRdsView.getConnectionPassword())
        );
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return emptyList();
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.SQL_STREAM_BUILDER_ADMIN;
    }
}
