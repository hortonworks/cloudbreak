package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class RangerRoleConfigProviderTest {

    private final RangerRoleConfigProvider underTest = new RangerRoleConfigProvider();

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
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig(DatabaseType.RANGER))).build();
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        //Shall be only a single match, which is the Ranger one
        assertEquals(1, roleConfigs.size());

        List<ApiClusterTemplateConfig> masterRangerAdmin = roleConfigs.get("ranger-RANGER_ADMIN-BASE");

        assertEquals(5, masterRangerAdmin.size());

        assertEquals("ranger_database_host", masterRangerAdmin.get(0).getName());
        assertEquals("10.1.1.1", masterRangerAdmin.get(0).getValue());

        assertEquals("ranger_database_name", masterRangerAdmin.get(1).getName());
        assertEquals("ranger", masterRangerAdmin.get(1).getValue());

        assertEquals("ranger_database_type", masterRangerAdmin.get(2).getName());
        assertEquals("PostgreSQL", masterRangerAdmin.get(2).getValue());

        assertEquals("ranger_database_user", masterRangerAdmin.get(3).getName());
        assertEquals("heyitsme", masterRangerAdmin.get(3).getValue());

        assertEquals("ranger_database_password", masterRangerAdmin.get(4).getName());
        assertEquals("iamsoosecure", masterRangerAdmin.get(4).getValue());

    }

    @Test
    public void testGetEmptyRoleConfigs() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        // We do not configure RANGER DB
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.HIVE)))).build();

        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }

    @Test(expected = IllegalStateException.class)
    public void testRoleConfigsForMultipleDb() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        // We configure multiple DBs
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(new HashSet<>(List.of(rdsConfig(DatabaseType.RANGER), rdsConfig(DatabaseType.RANGER)))).build();

        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        try {
            underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        } catch (CloudbreakServiceException cse) {
            assertEquals("Multiple databases have been provided for RANGER_ADMIN component", cse.getMessage());
            throw cse;
        }
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

}
