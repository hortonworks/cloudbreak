package com.sequenceiq.cloudbreak.cmtemplate.configproviders.das;

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
public class DasConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView dasView = getRdsView(source);
        return List.of(
                config("data_analytics_studio_database_host", dasView.getHost()),
                config("data_analytics_studio_database_port", dasView.getPort()),
                config("data_analytics_studio_database_name", dasView.getDatabaseName()),
                config("data_analytics_studio_database_username", dasView.getConnectionUserName()),
                config("data_analytics_studio_database_password", dasView.getConnectionPassword())
        );
    }

    @Override
    public String getServiceType() {
        return DasRoles.DAS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(DasRoles.WEBAPP, DasRoles.EVENTPROCESSOR);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.HIVE_DAS;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case DasRoles.WEBAPP:
                return List.of(config("data_analytics_studio_user_authentication", "KNOX_PROXY"));
            default:
                return List.of();
        }
    }
}
