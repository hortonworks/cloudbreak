package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smartrouter;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smartrouter.SmartRouterRoles.SMARTROUTER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smartrouter.SmartRouterRoles.SMARTROUTER_SERVER;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class SmartRouterRdsRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    public DatabaseType dbType() {
        return DatabaseType.SMART_ROUTER;
    }

    @Override
    public String dbUserKey() {
        return "smartrouter_database_user";
    }

    @Override
    public String dbPasswordKey() {
        return "smartrouter_database_password";
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        RdsView rdsView = getRdsView(source);
        result.add(config("smartrouter_database_host", rdsView.getHost()));
        result.add(config("smartrouter_database_port", rdsView.getPort()));
        result.add(config("smartrouter_database_name", rdsView.getDatabaseName()));
        result.add(config("smartrouter_database_type", rdsView.getSubprotocol()));
        result.add(config(dbUserKey(), rdsView.getConnectionUserName()));
        result.add(config(dbPasswordKey(), rdsView.getConnectionPassword()));
        return result;
    }

    @Override
    public String getServiceType() {
        return SMARTROUTER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(SMARTROUTER_SERVER);
    }
}
