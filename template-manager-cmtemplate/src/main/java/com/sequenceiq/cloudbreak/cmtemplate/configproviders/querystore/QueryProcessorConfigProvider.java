package com.sequenceiq.cloudbreak.cmtemplate.configproviders.querystore;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class QueryProcessorConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView queryProcessorView = getRdsView(source);
        return List.of(
                config("data_analytics_studio_database_host", queryProcessorView.getHost()),
                config("data_analytics_studio_database_port", queryProcessorView.getPort()),
                config("data_analytics_studio_database_name", queryProcessorView.getDatabaseName()),
                config("data_analytics_studio_database_username", queryProcessorView.getConnectionUserName()),
                config("data_analytics_studio_database_password", queryProcessorView.getConnectionPassword())
        );
    }

    @Override
    public String getServiceType() {
        return QueryStoreRoles.HUE_QUERY_PROCESSOR;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(QueryStoreRoles.QUERY_PROCESSOR);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.QUERY_PROCESSOR;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case QueryStoreRoles.WEBAPP:
                return List.of(config("data_analytics_studio_user_authentication", "KNOX_PROXY"));
            default:
                return List.of();
        }
    }
}
