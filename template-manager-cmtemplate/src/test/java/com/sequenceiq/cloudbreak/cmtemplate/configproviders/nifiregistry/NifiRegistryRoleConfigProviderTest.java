package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifiregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class NifiRegistryRoleConfigProviderTest {

    private final NifiRegistryRoleConfigProvider underTest = new NifiRegistryRoleConfigProvider();

    @Test
    public void testGetSchemaRegistryServiceConfigs701() {
        String inputJson = loadBlueprint("7.0.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertThat(serviceConfigs).isEmpty();
    }

    @Test
    public void testGetSchemaRegistryRoleConfigs720() {
        String inputJson = loadBlueprint("7.2.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(NifiRegistryRoles.NIFI_REGISTRY_SERVER, preparationObject);

        assertThat(roleConfigs).hasSameElementsAs(
                List.of(config("nifi.registry.db.url", "jdbc:postgresql://testhost:5432/nifi_registry"),
                        config("nifi.registry.db.username", "nifi_registry_server_user"),
                        config("nifi.registry.db.password", "nifi_registry_server_password"),
                        config("nifi.registry.db.driver.class", "org.postgresql.Driver"),
                        config("nifi.registry.db.driver.directory", "/usr/share/java/")));
    }

    @Test
    public void testThatUseParcelEmbeddedJdbcDriversIfAvailable() {
        String inputJson = loadBlueprint("7.2.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor, "2.2.7.0");

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(NifiRegistryRoles.NIFI_REGISTRY_SERVER, preparationObject);

        assertThat(roleConfigs).hasSameElementsAs(
                List.of(config("nifi.registry.db.url", "jdbc:postgresql://testhost:5432/nifi_registry"),
                        config("nifi.registry.db.username", "nifi_registry_server_user"),
                        config("nifi.registry.db.password", "nifi_registry_server_password"),
                        config("nifi.registry.db.driver.class", "org.postgresql.Driver")));
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor) {
        return getTemplatePreparationObject(cmTemplateProcessor, "2.2.6.0");
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor, String cfmVersion) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, cmTemplateProcessor);
        ClouderaManagerRepo repo = new ClouderaManagerRepo();
        List<ClouderaManagerProduct> products = singletonList(createCfmProduct(cfmVersion));

        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getType()).thenReturn(DatabaseType.NIFIREGISTRY.toString());
        when(rdsConfig.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfig.getConnectionDriver()).thenReturn(DatabaseVendor.POSTGRES.connectionDriver());
        when(rdsConfig.getConnectionURL()).thenReturn("jdbc:postgresql://testhost:5432/nifi_registry");
        when(rdsConfig.getConnectionUserName()).thenReturn("nifi_registry_server_user");
        when(rdsConfig.getConnectionPassword()).thenReturn("nifi_registry_server_password");

        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withHostgroupViews(Set.of(master, worker))
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e))
                        .collect(Collectors.toSet()))
                .withProductDetails(repo, products)
                .build();
    }

    private ClouderaManagerProduct createCfmProduct(String version) {
        ClouderaManagerProduct cfm = new ClouderaManagerProduct();
        cfm.withVersion(version);
        cfm.withName("CFM");

        return cfm;
    }

    private String loadBlueprint(String cdhVersion) {
        return FileReaderUtils.readFileFromClasspathQuietly("input/nifiregistry.bp").replace("__CDH_VERSION__", cdhVersion);
    }
}