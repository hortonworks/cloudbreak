package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
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

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_HOST;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_PORT;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryServiceConfigProvider.DATABASE_USER;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SchemaRegistryServiceConfigProviderTest {

    private final SchemaRegistryServiceConfigProvider underTest = new SchemaRegistryServiceConfigProvider();

    @Test
    public void testGetSchemaRegistryServerConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/cdp-streaming.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

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

    private TemplatePreparationObject getTemplatePreparationObject() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(DatabaseType.REGISTRY.toString());
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        rdsConfig.setConnectionURL("jdbc:postgresql://testhost:5432/schema_registry");
        rdsConfig.setConnectionUserName("schema_registry_server");
        rdsConfig.setConnectionPassword("schema_registry_server_password");

        return TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig))
                .build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
