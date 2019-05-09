package com.sequenceiq.cloudbreak.cmtemplate.configproviders.rds;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class RangerConfigProviderTest {

    private final RangerConfigProvider underTest = new RangerConfigProvider();

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> rangerAdminServer = roleConfigs.get("ranger-RANGER_ADMIN-BASE");

        assertEquals(5, rangerAdminServer.size());
        assertEquals("ranger_database_host", rangerAdminServer.get(0).getName());
        assertEquals("master_ranger_admin_ranger_database_host", rangerAdminServer.get(0).getVariable());

        assertEquals("ranger_database_name", rangerAdminServer.get(1).getName());
        assertEquals("master_ranger_admin_ranger_database_name", rangerAdminServer.get(1).getVariable());

        assertEquals("ranger_database_type", rangerAdminServer.get(2).getName());
        assertEquals("master_ranger_admin_ranger_database_type", rangerAdminServer.get(2).getVariable());

        assertEquals("ranger_database_user", rangerAdminServer.get(3).getName());
        assertEquals("master_ranger_admin_ranger_database_user", rangerAdminServer.get(3).getVariable());

        assertEquals("ranger_database_password", rangerAdminServer.get(4).getName());
        assertEquals("master_ranger_admin_ranger_database_password", rangerAdminServer.get(4).getVariable());
    }

    @Test
    public void testGetRoleConfigVariables() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);
        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));

        assertEquals(5, roleVariables.size());
        ApiClusterTemplateVariable databaseHost = roleVariables.get(0);
        assertEquals("master_ranger_admin_ranger_database_host", databaseHost.getName());
        assertEquals("testhost", databaseHost.getValue());

        ApiClusterTemplateVariable databaseName = roleVariables.get(1);
        assertEquals("master_ranger_admin_ranger_database_name", databaseName.getName());
        assertEquals("rangerdb", databaseName.getValue());

        ApiClusterTemplateVariable databasePassword = roleVariables.get(2);
        assertEquals("master_ranger_admin_ranger_database_password", databasePassword.getName());
        assertEquals("rangerpass", databasePassword.getValue());

        ApiClusterTemplateVariable databaseType = roleVariables.get(3);
        assertEquals("master_ranger_admin_ranger_database_type", databaseType.getName());
        assertEquals("postgresql", databaseType.getValue());

        ApiClusterTemplateVariable databaseUser = roleVariables.get(4);
        assertEquals("master_ranger_admin_ranger_database_user", databaseUser.getName());
        assertEquals("testrangeruser", databaseUser.getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(DatabaseType.RANGER.toString());
        rdsConfig.setConnectionPassword("rangerpass");
        rdsConfig.setConnectionUserName("testrangeruser");
        rdsConfig.setConnectionURL("jdbc:postgresql://testhost:5432/rangerdb");

        return TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(master, worker)).withRdsConfigs(Set.of(rdsConfig)).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}