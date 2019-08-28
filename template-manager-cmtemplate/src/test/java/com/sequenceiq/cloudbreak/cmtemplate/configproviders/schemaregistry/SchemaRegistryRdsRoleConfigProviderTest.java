package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class SchemaRegistryRdsRoleConfigProviderTest {

    private final SchemaRegistryRdsRoleConfigProvider underTest = new SchemaRegistryRdsRoleConfigProvider();

    @Test
    public void testGetSchemaRegistryServerRoleConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/cdp-streaming.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> schemaRegistryServer = roleConfigs.get("schemaregistry-SCHEMA_REGISTRY_SERVER-BASE");

        assertEquals(3, schemaRegistryServer.size());
        assertEquals("schema.registry.storage.connector.connectURI", schemaRegistryServer.get(0).getName());
        assertEquals("jdbc:postgresql://testhost:5432/schema_registry", schemaRegistryServer.get(0).getValue());

        assertEquals("schema.registry.storage.connector.user", schemaRegistryServer.get(1).getName());
        assertEquals("schema_registry_server", schemaRegistryServer.get(1).getValue());

        assertEquals("schema.registry.storage.connector.password", schemaRegistryServer.get(2).getName());
        assertEquals("schema_registry_server_password", schemaRegistryServer.get(2).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(DatabaseType.REGISTRY.toString());
        rdsConfig.setConnectionUserName("schema_registry_server");
        rdsConfig.setConnectionPassword("schema_registry_server_password");
        rdsConfig.setConnectionURL("jdbc:postgresql://testhost:5432/schema_registry");

        return TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig))
                .build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
