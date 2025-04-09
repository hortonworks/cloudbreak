package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.TestUtil.rdsConfigWithoutCluster;
import static com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight.HBASE_ADMIN;
import static com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight.RANGER_ADMIN;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToValueMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToVariableNameMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_ADMIN_SITE_XML_ROLE_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_DATABASE_PORT;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_DATABASE_USER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_DEFAULT_POLICY_GROUPS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider.RANGER_HBASE_ADMIN_VIRTUAL_GROUPS;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class RangerRoleConfigProviderTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String ADMIN_GROUP = "cdh_test";

    private static final String HBASE_ADMIN_GROUP = "hbase_test";

    @Mock
    private VirtualGroupService virtualGroupService;

    @InjectMocks
    private RangerRoleConfigProvider underTest;

    @Test
    public void testGetRoleConfigsWithoutRanger() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();

        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        //Since the blueprint does not contain Ranger
        assertThat(roleConfigs.size()).isEqualTo(0);

        List<ApiClusterTemplateConfig> masterRangerAdmin = roleConfigs.get("ranger-RANGER_ADMIN-BASE");

        assertThat(masterRangerAdmin).isNull();
    }

    public static Object[][] defaultPolicyGroupsDataProvider() {
        return new Object[][]{
                // testCaseName cdhVersion expectedRoleConfigCount expectedSvcConfigCount
                {"cdhVersion=6.9.0", "6.9.0", 5, 0},
                {"cdhVersion=6.x.0", "6.x.0", 5, 0},
                {"cdhVersion=7.0.0", "7.0.0", 5, 0},
                {"cdhVersion=", "", 5, 0},

                {"cdhVersion=7.0.1", "7.0.1", 6, 0},
                {"cdhVersion=7.1.0", "7.1.0", 6, 0},
                {"cdhVersion=7.1.x", "7.1.x", 6, 0},
                {"cdhVersion=7.2.0", "7.2.0", 6, 0},
                {"cdhVersion=7.2.1", "7.2.1", 1, 6},
                {"cdhVersion=7.3.1", "7.3.1", 1, 6},
                {"cdhVersion=7.6.0", "7.6.0", 2, 6},
                {"cdhVersion=8.1.0", "8.1.0", 2, 6},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("defaultPolicyGroupsDataProvider")
    public void testGetRangerAdminDefaultPolicyGroups(String testCaseName, String cdhVersion, int expectedRoleConfigCount, int expectedSvcConfigCount) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        cmTemplateProcessor.setCdhVersion(cdhVersion);

        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withBlueprintView(new BlueprintView(inputJson, "", "", null, cmTemplateProcessor))
                .withRdsViews(Set.of(rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, ""))
                .withProductDetails(generateCmRepo(() -> cdhVersion), null)
                .build();
        if (expectedRoleConfigCount == 6) {
            when(virtualGroupService.createOrGetVirtualGroup(preparationObject.getVirtualGroupRequest(), RANGER_ADMIN)).thenReturn(ADMIN_GROUP);
        }
        if ("7.6.0".equals(cdhVersion)) {
            when(virtualGroupService.createOrGetVirtualGroup(preparationObject.getVirtualGroupRequest(), RANGER_ADMIN)).thenReturn(ADMIN_GROUP);
            when(virtualGroupService.createOrGetVirtualGroup(preparationObject.getVirtualGroupRequest(), HBASE_ADMIN)).thenReturn(HBASE_ADMIN_GROUP);
        }

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterRangerAdmin = roleConfigs.get("ranger-RANGER_ADMIN-BASE");
        assertThat(masterRangerAdmin.size()).isEqualTo(expectedRoleConfigCount);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertThat(serviceConfigs.size()).isEqualTo(expectedSvcConfigCount);

        if (expectedRoleConfigCount == 6) {
            assertThat(masterRangerAdmin.get(5).getName()).isEqualTo(RANGER_DEFAULT_POLICY_GROUPS);
            assertThat(masterRangerAdmin.get(5).getValue()).isEqualTo(ADMIN_GROUP);
        }

        if ("7.6.0".equals(cdhVersion)) {
            assertThat(masterRangerAdmin.get(1).getName()).isEqualTo(RANGER_HBASE_ADMIN_VIRTUAL_GROUPS);
            assertThat(masterRangerAdmin.get(1).getValue()).isEqualTo(HBASE_ADMIN_GROUP);
        }
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withBlueprintView(new BlueprintView(inputJson, "", "", null, cmTemplateProcessor))
                .withRdsViews(Set.of(rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .withProductDetails(generateCmRepo(() -> "7.0.0"), null)
                .build();

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        //Shall be only a single match, which is the Ranger one
        assertThat(roleConfigs.size()).isEqualTo(1);

        List<ApiClusterTemplateConfig> masterRangerAdmin = roleConfigs.get("ranger-RANGER_ADMIN-BASE");

        assertThat(masterRangerAdmin.size()).isEqualTo(5);

        assertThat(masterRangerAdmin.get(0).getName()).isEqualTo(RANGER_DATABASE_HOST);
        assertThat(masterRangerAdmin.get(0).getValue()).isEqualTo("10.1.1.1");

        assertThat(masterRangerAdmin.get(1).getName()).isEqualTo(RANGER_DATABASE_NAME);
        assertThat(masterRangerAdmin.get(1).getValue()).isEqualTo("ranger");

        assertThat(masterRangerAdmin.get(2).getName()).isEqualTo(RANGER_DATABASE_TYPE);
        assertThat(masterRangerAdmin.get(2).getValue()).isEqualTo("PostgreSQL");

        assertThat(masterRangerAdmin.get(3).getName()).isEqualTo(RANGER_DATABASE_USER);
        assertThat(masterRangerAdmin.get(3).getValue()).isEqualTo("heyitsme");

        assertThat(masterRangerAdmin.get(4).getName()).isEqualTo(RANGER_DATABASE_PASSWORD);
        assertThat(masterRangerAdmin.get(4).getValue()).isEqualTo("iamsoosecure");
    }

    @Test
    public void validateRangerServiceConfigPost72() {
        String inputJson = getBlueprintText("input/cb6720.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);

        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master))
                .withBlueprintView(new BlueprintView(inputJson, "", "", null, cmTemplateProcessor))
                .withRdsViews(Set.of(rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertThat(serviceConfigs.size()).isEqualTo(6);

        assertThat(serviceConfigs.get(0).getName()).isEqualTo(RANGER_DATABASE_HOST);
        assertThat(serviceConfigs.get(0).getValue()).isEqualTo("10.1.1.1");

        assertThat(serviceConfigs.get(1).getName()).isEqualTo(RANGER_DATABASE_NAME);
        assertThat(serviceConfigs.get(1).getValue()).isEqualTo("ranger");

        assertThat(serviceConfigs.get(2).getName()).isEqualTo(RANGER_DATABASE_TYPE);
        assertThat(serviceConfigs.get(2).getValue()).isEqualTo("postgresql");

        assertThat(serviceConfigs.get(3).getName()).isEqualTo(RANGER_DATABASE_USER);
        assertThat(serviceConfigs.get(3).getValue()).isEqualTo("heyitsme");

        assertThat(serviceConfigs.get(4).getName()).isEqualTo(RANGER_DATABASE_PASSWORD);
        assertThat(serviceConfigs.get(4).getValue()).isEqualTo("iamsoosecure");

        assertThat(serviceConfigs.get(5).getName()).isEqualTo(RANGER_DATABASE_PORT);
        assertThat(serviceConfigs.get(5).getValue()).isEqualTo("5432");
    }

    @Test
    public void testGetEmptyRoleConfigs() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        // We do not configure RANGER DB
        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, worker))
                .withRdsViews(Set.of(rdsConfigWithoutCluster(DatabaseType.HIVE, RdsSslMode.DISABLED)).stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .build();

        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        assertThat(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject)).isFalse();
    }

    @Test
    public void testRoleConfigsForMultipleDb() {
        // We configure multiple DBs
        assertThatCode(() -> Builder.builder()
                .withRdsViews(Set.of(
                        rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED),
                        rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testGetRoleConfigsDbWithSsl() {
        RdsConfigWithoutCluster rdsConfig = rdsConfigWithoutCluster(DatabaseType.RANGER, RdsSslMode.DISABLED);
        when(rdsConfig.getSslMode()).thenReturn(RdsSslMode.ENABLED);
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, SSL_CERTS_FILE_PATH, "AWS", true))
                        .collect(Collectors.toSet()))
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();

        when(virtualGroupService.createOrGetVirtualGroup(tpo.getVirtualGroupRequest(), RANGER_ADMIN)).thenReturn(ADMIN_GROUP);

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(RangerRoles.RANGER_ADMIN, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                entry(RANGER_ADMIN_SITE_XML_ROLE_SAFETY_VALVE,
                        "<property>" +
                                "<name>ranger.jpa.jdbc.url</name>" +
                                "<value>jdbc:postgresql://10.1.1.1:5432/ranger?sslmode=verify-full&amp;sslrootcert=" + SSL_CERTS_FILE_PATH + "</value>" +
                                "</property>"),
                entry(RANGER_DEFAULT_POLICY_GROUPS, ADMIN_GROUP)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

    private ClouderaManagerRepo generateCmRepo(Versioned version) {
        return new ClouderaManagerRepo()
                .withBaseUrl("baseurl")
                .withGpgKeyUrl("gpgurl")
                .withPredefined(true)
                .withVersion(version.getVersion());
    }

}
