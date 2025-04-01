package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifiregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class NifiRegistryRoleConfigProviderTest {

    @InjectMocks
    private NifiRegistryRoleConfigProvider underTest;

    @Test
    void testGetRegistryServiceConfigs700() {
        String inputJson = loadBlueprint("7.0.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertThat(serviceConfigs).isEmpty();
    }

    static Object[][] testGetRoleConfigsDataProvider() {
        return new Object[][] {
                {"2.2.4.0", false},
                {"2.2.7.0", true},
                {"2.2.6.200", true},
                {"2.2.6.100", false},
                {"2.2.5.300", true}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testGetRoleConfigsDataProvider")
    void testGetRoleConfigs(String cfmVersion, boolean useParcelEmbeddedJdbcDrivers) {
        String inputJson = loadBlueprint("7.2.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor, cfmVersion);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(NifiRegistryRoles.NIFI_REGISTRY_SERVER, cmTemplateProcessor, preparationObject);

        Builder<ApiClusterTemplateConfig> expectedRoleConfigs =
                ImmutableList
                        .<ApiClusterTemplateConfig>builder()
                        .add(config("nifi.registry.db.url", "jdbc:postgresql://testhost:5432/nifi_registry"))
                        .add(config("nifi.registry.db.username", "nifi_registry_server_user"))
                        .add(config("nifi.registry.db.password", "nifi_registry_server_password"))
                        .add(config("nifi.registry.db.driver.class", "org.postgresql.Driver"));

        if (!useParcelEmbeddedJdbcDrivers) {
            expectedRoleConfigs.add(config("nifi.registry.db.driver.directory", "/usr/share/java/"));
        }

        assertThat(roleConfigs).hasSameElementsAs(expectedRoleConfigs.build());
    }

    static Object[][] getRoleConfigsTestWhenSslDataProvider() {
        return new Object[][]{
                // cfmVersion, sslMode, expectedDbUrl, cloudPlatform, externalDB
                {"2.2.5.100", RdsSslMode.DISABLED, "jdbc:postgresql://testhost:5432/nifi_registry", "AWS", true},
                {"2.2.5.100", RdsSslMode.DISABLED, "jdbc:postgresql://testhost:5432/nifi_registry", "GCP", true},
                {"2.2.5.100", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=require&sslrootcert=/foo/bar.pem", "AWS", true},
                {"2.2.6.0", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=require&sslrootcert=/foo/bar.pem", "AWS", true},
                {"2.2.6.199", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=require&sslrootcert=/foo/bar.pem", "AWS", true},
                {"2.2.6.200", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-full&sslrootcert=/foo/bar.pem", "AWS", true},
                {"2.2.6.201", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-full&sslrootcert=/foo/bar.pem", "AWS", true},
                {"2.2.7.0", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-full&sslrootcert=/foo/bar.pem", "AWS", true},
                {"2.2.5.100", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=require&sslrootcert=/foo/bar.pem", "GCP", true},
                {"2.2.6.0", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=require&sslrootcert=/foo/bar.pem", "GCP", true},
                {"2.2.6.199", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=require&sslrootcert=/foo/bar.pem", "GCP", true},
                {"2.2.6.200", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-full&sslrootcert=/foo/bar.pem", "GCP", false},
                {"2.2.6.201", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-full&sslrootcert=/foo/bar.pem", "GCP", false},
                {"2.2.7.0", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-full&sslrootcert=/foo/bar.pem", "GCP", false},
                {"2.2.6.200", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-ca&sslrootcert=/foo/bar.pem", "GCP", true},
                {"2.2.6.201", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-ca&sslrootcert=/foo/bar.pem", "GCP", true},
                {"2.2.7.0", RdsSslMode.ENABLED, "jdbc:postgresql://testhost:5432/nifi_registry?sslmode=verify-ca&sslrootcert=/foo/bar.pem", "GCP", true}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getRoleConfigsTestWhenSslDataProvider")
    void getRoleConfigsTestWhenSsl(String cfmVersion, RdsSslMode sslMode, String expectedDbUrl,
            String cloudPlatform, boolean externalDBRequested) {
        String inputJson = loadBlueprint("7.2.0");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor, cfmVersion, sslMode, cloudPlatform,
                externalDBRequested);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(NifiRegistryRoles.NIFI_REGISTRY_SERVER, cmTemplateProcessor, preparationObject);

        Optional<String> dbUrlOptional = roleConfigs.stream()
                .filter(c -> "nifi.registry.db.url".equals(c.getName()))
                .map(ApiClusterTemplateConfig::getValue)
                .findFirst();
        assertThat(dbUrlOptional).isPresent();
        assertThat(dbUrlOptional).hasValue(expectedDbUrl);
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor) {
        return getTemplatePreparationObject(cmTemplateProcessor, "2.2.6.0");
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor, String cfmVersion) {
        return getTemplatePreparationObject(cmTemplateProcessor, cfmVersion, RdsSslMode.DISABLED, "AWS", true);
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor, String cfmVersion, RdsSslMode sslMode,
            String cloudPlatform, boolean externalDBRequested) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);
        ClouderaManagerRepo repo = new ClouderaManagerRepo();
        List<ClouderaManagerProduct> products = singletonList(createCfmProduct(cfmVersion));

        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getType()).thenReturn(DatabaseType.NIFIREGISTRY.toString());
        when(rdsConfig.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfig.getConnectionDriver()).thenReturn(DatabaseVendor.POSTGRES.connectionDriver());
        when(rdsConfig.getConnectionURL()).thenReturn("jdbc:postgresql://testhost:5432/nifi_registry");
        when(rdsConfig.getConnectionUserName()).thenReturn("nifi_registry_server_user");
        when(rdsConfig.getConnectionPassword()).thenReturn("nifi_registry_server_password");
        when(rdsConfig.getSslMode()).thenReturn(sslMode);

        return TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withHostgroupViews(Set.of(master, worker))
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "/foo/bar.pem", cloudPlatform, externalDBRequested))
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