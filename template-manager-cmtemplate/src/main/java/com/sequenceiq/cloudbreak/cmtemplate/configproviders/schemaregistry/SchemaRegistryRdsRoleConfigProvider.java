package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class SchemaRegistryRdsRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER:
                RdsView schemaRegistryRdsView = getRdsView(source);
                return List.of(
                        config("schema.registry.storage.connector.connectURI", schemaRegistryRdsView.getConnectionURL()),
                        config("schema.registry.storage.connector.user", schemaRegistryRdsView.getConnectionUserName()),
                        config("schema.registry.storage.connector.password", schemaRegistryRdsView.getConnectionPassword())
                );
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return SchemaRegistryRoles.SCHEMAREGISTRY;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.REGISTRY;
    }
}
