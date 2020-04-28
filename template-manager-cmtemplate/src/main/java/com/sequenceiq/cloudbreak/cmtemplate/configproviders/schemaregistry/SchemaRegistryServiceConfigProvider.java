package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.StreamingAppRdsRoleConfigProviderUtil.dataBaseTypeForCM;
import static java.util.Collections.emptyList;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class SchemaRegistryServiceConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String DATABASE_TYPE = "database_type";

    static final String DATABASE_NAME = "database_name";

    static final String DATABASE_HOST = "database_host";

    static final String DATABASE_PORT = "database_port";

    static final String DATABASE_USER = "database_user";

    static final String DATABASE_PASSWORD = "database_password";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_1)) {
            RdsView schemaRegistryRdsView = getRdsView(source);
            return List.of(
                    config(DATABASE_TYPE, dataBaseTypeForCM(schemaRegistryRdsView.getDatabaseVendor())),
                    config(DATABASE_NAME, schemaRegistryRdsView.getDatabaseName()),
                    config(DATABASE_HOST, schemaRegistryRdsView.getHost()),
                    config(DATABASE_PORT, schemaRegistryRdsView.getPort()),
                    config(DATABASE_USER, schemaRegistryRdsView.getConnectionUserName()),
                    config(DATABASE_PASSWORD, schemaRegistryRdsView.getConnectionPassword())
            );
        }
        return emptyList();
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (!isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_1)) {
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

        return emptyList();
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
