package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.GENERATED_RANGER_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.StreamingAppRdsRoleConfigProviderUtil.dataBaseTypeForCM;

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
public class SchemaRegistryServiceConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String DATABASE_TYPE = "database_type";

    static final String DATABASE_NAME = "database_name";

    static final String DATABASE_HOST = "database_host";

    static final String DATABASE_PORT = "database_port";

    static final String DATABASE_USER = "database_user";

    static final String DATABASE_PASSWORD = "database_password";

    static final String DATABASE_JDBC_URL_OVERRIDE = "database_jdbc_url_override";

    static final String RANGER_PLUGIN_SR_SERVICE_NAME = "ranger.plugin.schema-registry.service.name";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        List<ApiClusterTemplateConfig> config = new ArrayList<>();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
            RdsView schemaRegistryRdsView = getRdsView(source);
            config.add(config(DATABASE_TYPE, dataBaseTypeForCM(schemaRegistryRdsView.getDatabaseVendor())));
            config.add(config(DATABASE_USER, schemaRegistryRdsView.getConnectionUserName()));
            config.add(config(DATABASE_PASSWORD, schemaRegistryRdsView.getConnectionPassword()));
            if (schemaRegistryRdsView.isUseSsl()) {
                config.add(config(DATABASE_JDBC_URL_OVERRIDE, schemaRegistryRdsView.getConnectionURL()));
            } else {
                config.add(config(DATABASE_NAME, schemaRegistryRdsView.getDatabaseName()));
                config.add(config(DATABASE_HOST, schemaRegistryRdsView.getHost()));
                config.add(config(DATABASE_PORT, schemaRegistryRdsView.getPort()));
            }
        }
        return config;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
            return List.of(config(RANGER_PLUGIN_SR_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME));
        } else {
            // Legacy db configs
            RdsView schemaRegistryRdsView = getRdsView(source);
            return List.of(
                    config("schema.registry.storage.connector.connectURI", schemaRegistryRdsView.getConnectionURL()),
                    config("schema.registry.storage.connector.user", schemaRegistryRdsView.getConnectionUserName()),
                    config("schema.registry.storage.connector.password", schemaRegistryRdsView.getConnectionPassword())
            );
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
