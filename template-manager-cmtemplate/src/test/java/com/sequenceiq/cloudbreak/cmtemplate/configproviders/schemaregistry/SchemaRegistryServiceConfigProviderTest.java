package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.GENERATED_RANGER_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_HOST;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_JDBC_URL_OVERRIDE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_PORT;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_USER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.RANGER_PLUGIN_SR_SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class SchemaRegistryServiceConfigProviderTest {

    private final SchemaRegistryServiceConfigProvider underTest = new SchemaRegistryServiceConfigProvider();

    @Test
    public void testGetSchemaRegistryServiceConfigs710() {
        String inputJson = loadBlueprint("7.1.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor, false);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertThat(serviceConfigs).isEmpty();
    }

    @Test
    public void testGetSchemaRegistryServiceConfigs722() {
        String inputJson = loadBlueprint("7.2.2");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor, false);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertThat(serviceConfigs).hasSameElementsAs(List.of(
                config(DATABASE_TYPE, "postgresql"),
                config(DATABASE_NAME, "schema_registry"),
                config(DATABASE_HOST, "testhost"),
                config(DATABASE_PORT, "5432"),
                config(DATABASE_USER, "schema_registry_server"),
                config(DATABASE_PASSWORD, "schema_registry_server_password")
        ));
    }

    @Test
    public void testGetSchemaRegistryServiceConfigs722WithSsl() {
        String inputJson = loadBlueprint("7.2.2");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor, true);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertThat(serviceConfigs).hasSameElementsAs(List.of(
                config(DATABASE_TYPE, "postgresql"),
                config(DATABASE_JDBC_URL_OVERRIDE, "jdbc:postgresql://testhost:5432/schema_registry?sslmode=true"),
                config(DATABASE_USER, "schema_registry_server"),
                config(DATABASE_PASSWORD, "schema_registry_server_password")
        ));
    }

    @Test
    public void testGetSchemaRegistryRoleConfigs710() {
        String inputJson = loadBlueprint("7.1.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor, true);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER,
                preparationObject);

        assertThat(roleConfigs).hasSameElementsAs(
                List.of(config("schema.registry.storage.connector.connectURI", "jdbc:postgresql://testhost:5432/schema_registry?sslmode=true"),
                        config("schema.registry.storage.connector.user", "schema_registry_server"),
                        config("schema.registry.storage.connector.password", "schema_registry_server_password")));
    }

    @Test
    public void testGetSchemaRegistryRoleConfigs720() {
        String inputJson = loadBlueprint("7.2.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor, false);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER,
                preparationObject);

        assertThat(roleConfigs).hasSameElementsAs(List.of(
                config(RANGER_PLUGIN_SR_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME)));
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor, boolean ssl) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(DatabaseType.REGISTRY.toString());
        when(rdsConfig.getHost()).thenReturn("testhost");
        when(rdsConfig.getConnectionUserName()).thenReturn("schema_registry_server");
        when(rdsConfig.getConnectionPassword()).thenReturn("schema_registry_server_password");
        when(rdsConfig.getDatabaseName()).thenReturn("schema_registry");
        when(rdsConfig.getPort()).thenReturn("5432");
        when(rdsConfig.getDatabaseVendor()).thenReturn(DatabaseVendor.POSTGRES);
        if (ssl) {
            when(rdsConfig.getConnectionURL()).thenReturn("jdbc:postgresql://testhost:5432/schema_registry?sslmode=true");
            when(rdsConfig.isUseSsl()).thenReturn(true);
        }

        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withHostgroupViews(Set.of(master, worker))
                .withRdsViews(Set.of(rdsConfig))
                .build();
    }

    private String loadBlueprint(String cdhVersion) {
        return FileReaderUtils.readFileFromClasspathQuietly("input/cdp-streaming.bp").replace("__CDH_VERSION__", cdhVersion);
    }

}
