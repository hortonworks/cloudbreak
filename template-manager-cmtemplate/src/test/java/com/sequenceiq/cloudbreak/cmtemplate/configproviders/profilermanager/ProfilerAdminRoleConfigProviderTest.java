package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

import static com.sequenceiq.cloudbreak.TestUtil.rdsConfigWithoutCluster;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_JDBC_URL_OVERRIDE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class ProfilerAdminRoleConfigProviderTest {
    private ProfilerAdminRoleConfigProvider underTest;

    @BeforeEach
    public void setUp() {
        underTest = new ProfilerAdminRoleConfigProvider();
    }

    @Test
    void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(RdsSslMode.DISABLED);
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>>
                roleConfigs =
                underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig>
                profilerAdmin =
                roleConfigs.get("profiler_manager-PROFILER_ADMIN_AGENT-BASE");

        assertThat(profilerAdmin.size()).isEqualTo(5);

        assertThat(profilerAdmin.get(0).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_USER);
        assertThat(profilerAdmin.get(0).getValue()).isEqualTo("heyitsme");

        assertThat(profilerAdmin.get(1).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_PASSWORD);
        assertThat(profilerAdmin.get(1).getValue()).isEqualTo("iamsoosecure");

        assertThat(profilerAdmin.get(2).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_HOST);
        assertThat(profilerAdmin.get(2).getValue()).isEqualTo("10.1.1.1");

        assertThat(profilerAdmin.get(3).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_NAME);
        assertThat(profilerAdmin.get(3).getValue()).isEqualTo("profiler_agent");

        assertThat(profilerAdmin.get(4).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_TYPE);
        assertThat(profilerAdmin.get(4).getValue()).isEqualTo("POSTGRES");
    }

    @Test
    void testGetRoleConfigsWithSingleRolesPerHostGroupWithSSL() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(RdsSslMode.ENABLED);
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>>
                roleConfigs =
                underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig>
                profilerAdmin =
                roleConfigs.get("profiler_manager-PROFILER_ADMIN_AGENT-BASE");

        assertThat(profilerAdmin.size()).isEqualTo(4);

        assertThat(profilerAdmin.get(0).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_USER);
        assertThat(profilerAdmin.get(0).getValue()).isEqualTo("heyitsme");

        assertThat(profilerAdmin.get(1).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_PASSWORD);
        assertThat(profilerAdmin.get(1).getValue()).isEqualTo("iamsoosecure");

        assertThat(profilerAdmin.get(2).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_HOST);
        assertThat(profilerAdmin.get(2).getValue()).isEqualTo("10.1.1.1");

        assertThat(profilerAdmin.get(3).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_JDBC_URL_OVERRIDE);
        assertThat(profilerAdmin.get(3).getValue()).isEqualTo("jdbc:postgresql://10.1.1.1:5432/profiler_agent?sslmode=verify-full&sslrootcert=");
    }

    private TemplatePreparationObject getTemplatePreparationObject(RdsSslMode rdsSslMode) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        return Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withProductDetails(new ClouderaManagerRepo()
                        .withBaseUrl("url")
                        .withVersion("7.2.2"), new ArrayList<>())
                .withRdsViews(Set.of(rdsConfigWithoutCluster(DatabaseType.PROFILER_AGENT, rdsSslMode))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .build();
    }

    @Test
    void testGetRoleConfigsInGatewayHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, 1);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        RdsConfigWithoutCluster rdsConfigWithoutCluster = rdsConfigWithoutCluster(DatabaseType.PROFILER_AGENT, RdsSslMode.DISABLED);

        String inputJson = getBlueprintText("input/profilermanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, gateway))
                .withProductDetails(new ClouderaManagerRepo()
                        .withVersion("7.2.2"), new ArrayList<>())
                .withRdsViews(Set.of(rdsConfigWithoutCluster)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .build();

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs =
                underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> profilerAdmin =
                roleConfigs.get("profiler_manager-PROFILER_ADMIN_AGENT-BASE");

        assertThat(profilerAdmin.size()).isEqualTo(5);

        assertThat(profilerAdmin.get(0).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_USER);
        assertThat(profilerAdmin.get(0).getValue()).isEqualTo("heyitsme");

        assertThat(profilerAdmin.get(1).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_PASSWORD);
        assertThat(profilerAdmin.get(1).getValue()).isEqualTo("iamsoosecure");

        assertThat(profilerAdmin.get(2).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_HOST);
        assertThat(profilerAdmin.get(2).getValue()).isEqualTo("10.1.1.1");

        assertThat(profilerAdmin.get(3).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_NAME);
        assertThat(profilerAdmin.get(3).getValue()).isEqualTo("profiler_agent");

        assertThat(profilerAdmin.get(4).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_TYPE);
        assertThat(profilerAdmin.get(4).getValue()).isEqualTo("POSTGRES");
    }

    @Test
    void testGetRoleConfigsInGatewayHostGroupWithSsl() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, 1);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        RdsConfigWithoutCluster rdsConfigWithoutCluster = rdsConfigWithoutCluster(DatabaseType.PROFILER_AGENT, RdsSslMode.ENABLED);

        String inputJson = getBlueprintText("input/profilermanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, gateway))
                .withProductDetails(new ClouderaManagerRepo()
                        .withVersion("7.2.2"), new ArrayList<>())
                .withRdsViews(Set.of(rdsConfigWithoutCluster)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .build();

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs =
                underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> profilerAdmin =
                roleConfigs.get("profiler_manager-PROFILER_ADMIN_AGENT-BASE");

        assertThat(profilerAdmin.size()).isEqualTo(4);

        assertThat(profilerAdmin.get(0).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_USER);
        assertThat(profilerAdmin.get(0).getValue()).isEqualTo("heyitsme");

        assertThat(profilerAdmin.get(1).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_PASSWORD);
        assertThat(profilerAdmin.get(1).getValue()).isEqualTo("iamsoosecure");

        assertThat(profilerAdmin.get(2).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_HOST);
        assertThat(profilerAdmin.get(2).getValue()).isEqualTo("10.1.1.1");

        assertThat(profilerAdmin.get(3).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_JDBC_URL_OVERRIDE);
        assertThat(profilerAdmin.get(3).getValue()).isEqualTo("jdbc:postgresql://10.1.1.1:5432/profiler_agent?sslmode=verify-full&sslrootcert=");
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
