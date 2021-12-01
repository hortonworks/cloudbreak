package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigs.GENERATED_RANGER_SERVICE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.RANGER_PLUGIN_SR_SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class SchemaRegistryServiceConfigProviderTest {

    private final SchemaRegistryServiceConfigProvider underTest = new SchemaRegistryServiceConfigProvider();

    @Test
    public void testGetSchemaRegistryServiceConfigs710() {
        String inputJson = loadBlueprint("7.1.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertThat(serviceConfigs).isEmpty();
    }

    @Test
    public void testGetSchemaRegistryRoleConfigs710() {
        String inputJson = loadBlueprint("7.1.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER,
                preparationObject);

        assertThat(roleConfigs).hasSameElementsAs(
                List.of(config("schema.registry.storage.connector.connectURI", "jdbc:postgresql://testhost:5432/schema_registry"),
                        config("schema.registry.storage.connector.user", "schema_registry_server"),
                        config("schema.registry.storage.connector.password", "schema_registry_server_password")));
    }

    @Test
    public void testGetSchemaRegistryRoleConfigs720() {
        String inputJson = loadBlueprint("7.2.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER,
                preparationObject);

        assertThat(roleConfigs).hasSameElementsAs(List.of(
                config(RANGER_PLUGIN_SR_SERVICE_NAME, GENERATED_RANGER_SERVICE_NAME)));
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, cmTemplateProcessor);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(DatabaseType.REGISTRY.toString());
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        rdsConfig.setConnectionURL("jdbc:postgresql://testhost:5432/schema_registry");
        rdsConfig.setConnectionUserName("schema_registry_server");
        rdsConfig.setConnectionPassword("schema_registry_server_password");

        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig))
                .build();
    }

    private String loadBlueprint(String cdhVersion) {
        return FileReaderUtils.readFileFromClasspathQuietly("input/cdp-streaming.bp").replace("__CDH_VERSION__", cdhVersion);
    }

}
