package com.sequenceiq.cloudbreak.cmtemplate.configproviders.modelregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class ModelRegistryConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String MODEL_REGISTRY_DATABASE_HOST = "model_registry_database_host";

    static final String MODEL_REGISTRY_DATABASE_PORT = "model_registry_database_port";

    static final String MODEL_REGISTRY_DATABASE_NAME = "model_registry_database_name";

    static final String MODEL_REGISTRY_DATABASE_USER = "model_registry_database_user";

    static final String MODEL_REGISTRY_DATABASE_PASSWORD = "model_registry_database_password";

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.MODEL_REGISTRY;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        RdsView modelRegistryRdsView = getRdsView(source);
        configList.add(config(MODEL_REGISTRY_DATABASE_HOST, modelRegistryRdsView.getHost()));
        configList.add(config(MODEL_REGISTRY_DATABASE_PORT, modelRegistryRdsView.getPort()));
        configList.add(config(MODEL_REGISTRY_DATABASE_USER, modelRegistryRdsView.getUserName()));
        configList.add(config(MODEL_REGISTRY_DATABASE_PASSWORD, modelRegistryRdsView.getPassword()));
        configList.add(config(MODEL_REGISTRY_DATABASE_NAME, modelRegistryRdsView.getDatabaseName()));
        return configList;
    }

    @Override
    public String getServiceType() {
        return "MODEL_REGISTRY";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of("MODEL_REGISTRY");
    }

}
