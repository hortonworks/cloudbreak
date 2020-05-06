package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.TestUtil.ldapConfigBuilder;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.util.TestConstants;

@RunWith(MockitoJUnitRunner.class)
public class RangerRoleConfigProviderTest {
    @Mock
    private VirtualGroupService virtualGroupService;

    @InjectMocks
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
    public void testGetRangerAdminDefaultPolicyGroups() {
        validateGetRangerAdminDefaultPolicyGroups("6.9.0", 5);
        validateGetRangerAdminDefaultPolicyGroups("6.x.0", 5);
        validateGetRangerAdminDefaultPolicyGroups("7.0.0", 5);
        validateGetRangerAdminDefaultPolicyGroups("", 5);

        validateGetRangerAdminDefaultPolicyGroups("7.x.0", 6);
        validateGetRangerAdminDefaultPolicyGroups("7.0.1", 6);
        validateGetRangerAdminDefaultPolicyGroups("7.1.0", 6);
        validateGetRangerAdminDefaultPolicyGroups("7.2.0", 6);
        validateGetRangerAdminDefaultPolicyGroups("7.3.1", 6);
        validateGetRangerAdminDefaultPolicyGroups("8.1.0", 6);
    }

    private void validateGetRangerAdminDefaultPolicyGroups(String cdhVersion, int expectedConfigCount) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        cmTemplateProcessor.setCdhVersion(cdhVersion);

        String ldapAdminGroup = "cdh_test";
        LdapView ldapView = ldapConfigBuilder()
                .withDirectoryType(DirectoryType.LDAP)
                .withAdminGroup(ldapAdminGroup)
                .build();

        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withBlueprintView(new BlueprintView(inputJson, "", "", cmTemplateProcessor))
                .withRdsConfigs(Set.of(rdsConfig(DatabaseType.RANGER)))
                .withLdapConfig(ldapView)
                .withVirtualGroupView(new VirtualGroupRequest(TestConstants.CRN, "")).build();
        Mockito.when(virtualGroupService.getVirtualGroup(preparationObject.getVirtualGroupRequest(), UmsRight.RANGER_ADMIN.getRight())).thenReturn("cdh_test");

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> masterRangerAdmin = roleConfigs.get("ranger-RANGER_ADMIN-BASE");
        assertEquals(expectedConfigCount, masterRangerAdmin.size());

        if (expectedConfigCount == 6) {
            assertEquals("ranger.default.policy.groups", masterRangerAdmin.get(5).getName());
            assertEquals(ldapAdminGroup, masterRangerAdmin.get(5).getValue());
        }
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withBlueprintView(new BlueprintView(inputJson, "", "", cmTemplateProcessor))
                .withRdsConfigs(Set.of(rdsConfig(DatabaseType.RANGER)))
                .build();

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
