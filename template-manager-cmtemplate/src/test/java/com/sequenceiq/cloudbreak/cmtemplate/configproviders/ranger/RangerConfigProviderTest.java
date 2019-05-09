package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class RangerConfigProviderTest {

    private final RangerRoleConfigProvider underTest = new RangerRoleConfigProvider();

    @Test
    public void testGetRoleTypeVariableName() {
        String variableName = underTest.getRoleTypeVariableName("master", "RANGER_ADMIN", "custom_property");

        assertEquals("master_ranger_admin_custom_property", variableName);
    }

    @Test
    public void testGetRoleConfigsWithoutRanger() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();

        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        //Since the blueprint does not contain Ranger
        assertEquals(0, roleConfigs.size());

        List<ApiClusterTemplateConfig> masterRangerAdmin = roleConfigs.get("ranger-RANGER_ADMIN-BASE");

        assertNull(masterRangerAdmin);
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        //Shall be only a single match, which is the Ranger one
        assertEquals(1, roleConfigs.size());

        List<ApiClusterTemplateConfig> masterRangerAdmin = roleConfigs.get("ranger-RANGER_ADMIN-BASE");

        assertEquals(5, masterRangerAdmin.size());

        assertEquals("ranger_database_host", masterRangerAdmin.get(0).getName());
        assertEquals("ranger-ranger_database_host", masterRangerAdmin.get(0).getVariable());

        assertEquals("ranger_database_name", masterRangerAdmin.get(1).getName());
        assertEquals("ranger-ranger_database_name", masterRangerAdmin.get(1).getVariable());

        assertEquals("ranger_database_type", masterRangerAdmin.get(2).getName());
        assertEquals("ranger-ranger_database_type", masterRangerAdmin.get(2).getVariable());

        assertEquals("ranger_database_user", masterRangerAdmin.get(3).getName());
        assertEquals("ranger-ranger_database_user", masterRangerAdmin.get(3).getVariable());

        assertEquals("ranger_database_password", masterRangerAdmin.get(4).getName());
        assertEquals("ranger-ranger_database_password", masterRangerAdmin.get(4).getVariable());

    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

}
