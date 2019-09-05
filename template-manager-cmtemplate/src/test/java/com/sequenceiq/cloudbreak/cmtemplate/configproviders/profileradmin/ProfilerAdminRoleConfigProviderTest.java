package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profileradmin;

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
import static org.junit.Assert.assertEquals;

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
                roleConfigs.get("profiler_admin-PROFILER_ADMIN_AGENT-BASE");

        assertEquals(5, profilerAdmin.size());
        assertEquals("profiler_admin_database_host", profilerAdmin.get(0).getName());
        assertEquals("10.1.1.1", profilerAdmin.get(0).getValue());

        assertEquals("profiler_admin_database_name", profilerAdmin.get(1).getName());
        assertEquals("profiler_agent", profilerAdmin.get(1).getValue());

        assertEquals("profiler_admin_database_type", profilerAdmin.get(2).getName());
        assertEquals("POSTGRES", profilerAdmin.get(2).getValue());

        assertEquals("profiler_admin_database_user", profilerAdmin.get(3).getName());
        assertEquals("heyitsme", profilerAdmin.get(3).getValue());

        assertEquals("profiler_admin_database_password", profilerAdmin.get(4).getName());
        assertEquals("iamsoosecure", profilerAdmin.get(4).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        return Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig(DatabaseType.PROFILER_AGENT))).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
