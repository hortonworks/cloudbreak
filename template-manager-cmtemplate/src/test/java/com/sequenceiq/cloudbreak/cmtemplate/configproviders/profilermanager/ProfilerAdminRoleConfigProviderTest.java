package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_USER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager.ProfilerAdminRoleConfigProvider.PROFILER_ADMIN_DATABASE_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ProfilerAdminRoleConfigProviderTest {
    private ProfilerAdminRoleConfigProvider underTest;

    @Before
    public void setUp() {
        underTest = new ProfilerAdminRoleConfigProvider();
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>>
                roleConfigs =
                underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig>
                profilerAdmin =
                roleConfigs.get("profiler_manager-PROFILER_ADMIN_AGENT-BASE");

        assertThat(profilerAdmin.size()).isEqualTo(5);

        assertThat(profilerAdmin.get(0).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_HOST);
        assertThat(profilerAdmin.get(0).getValue()).isEqualTo("10.1.1.1");

        assertThat(profilerAdmin.get(1).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_NAME);
        assertThat(profilerAdmin.get(1).getValue()).isEqualTo("profiler_agent");

        assertThat(profilerAdmin.get(2).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_TYPE);
        assertThat(profilerAdmin.get(2).getValue()).isEqualTo("POSTGRES");

        assertThat(profilerAdmin.get(3).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_USER);
        assertThat(profilerAdmin.get(3).getValue()).isEqualTo("heyitsme");

        assertThat(profilerAdmin.get(4).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_PASSWORD);
        assertThat(profilerAdmin.get(4).getValue()).isEqualTo("iamsoosecure");
    }

    private TemplatePreparationObject getTemplatePreparationObject() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        return Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig(DatabaseType.PROFILER_AGENT))).build();
    }

    @Test
    public void testGetRoleConfigsInGatewayHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, 1);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);

        String inputJson = getBlueprintText("input/profilermanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(master, gateway))
                .withRdsConfigs(Set.of(rdsConfig(DatabaseType.PROFILER_AGENT)))
                .build();

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs =
                underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> profilerAdmin =
                roleConfigs.get("profiler_manager-PROFILER_ADMIN_AGENT-BASE");

        assertThat(profilerAdmin.size()).isEqualTo(5);

        assertThat(profilerAdmin.get(0).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_HOST);
        assertThat(profilerAdmin.get(0).getValue()).isEqualTo("10.1.1.1");

        assertThat(profilerAdmin.get(1).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_NAME);
        assertThat(profilerAdmin.get(1).getValue()).isEqualTo("profiler_agent");

        assertThat(profilerAdmin.get(2).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_TYPE);
        assertThat(profilerAdmin.get(2).getValue()).isEqualTo("POSTGRES");

        assertThat(profilerAdmin.get(3).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_USER);
        assertThat(profilerAdmin.get(3).getValue()).isEqualTo("heyitsme");

        assertThat(profilerAdmin.get(4).getName()).isEqualTo(PROFILER_ADMIN_DATABASE_PASSWORD);
        assertThat(profilerAdmin.get(4).getValue()).isEqualTo("iamsoosecure");
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
