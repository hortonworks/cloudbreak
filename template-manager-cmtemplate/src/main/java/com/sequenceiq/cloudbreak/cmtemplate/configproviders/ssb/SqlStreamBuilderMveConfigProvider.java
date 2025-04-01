package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ssb;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.Collections.emptyList;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class SqlStreamBuilderMveConfigProvider extends SqlStreamBuilderConfigProvider {

    static final String DATABASE_URL = "ssb.mve.datasource.url";

    static final String DATABASE_USER = "ssb.mve.datasource.username";

    static final String DATABASE_PASSWORD = "ssb.mve.datasource.password";

    @Override
    public String dbUserKey() {
        return DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return DATABASE_PASSWORD;
    }

    @Override
    public String getServiceType() {
        return SqlStreamBuilderRoles.SQL_STREAM_BUILDER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(SqlStreamBuilderRoles.MATERIALIZED_VIEW_ENGINE);
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView ssbRdsView = getRdsView(source);

        if (SqlStreamBuilderRoles.MATERIALIZED_VIEW_ENGINE.equals(roleType)) {
            return List.of(
                    config(DATABASE_URL, ssbRdsView.getConnectionURL()),
                    config(DATABASE_USER, ssbRdsView.getConnectionUserName()),
                    config(DATABASE_PASSWORD, ssbRdsView.getConnectionPassword())
            );
        }

        return emptyList();
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.SQL_STREAM_BUILDER_SNAPPER;
    }
}
