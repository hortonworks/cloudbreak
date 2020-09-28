package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoles.RANGER_USERSYNC;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;

@RunWith(MockitoJUnitRunner.class)
public class RangerUserSyncRoleConfigProviderTest {

    @Mock
    private VirtualGroupService virtualGroupService;

    @InjectMocks
    private final RangerUserSyncRoleConfigProvider underTest = new RangerUserSyncRoleConfigProvider();

    @Before
    public void setup() {
        when(virtualGroupService.getVirtualGroup(any(), anyString())).thenReturn("mockAdmin");
    }

    @Test
    public void testAws() {
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .withServicePrincipals(null)
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("conf/ranger-ugsync-site.xml_role_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>ranger.usersync.unix.backend</name><value>nss</value></property>", serviceConfigs.get(0).getValue());

        assertEquals("ranger.usersync.group.based.role.assignment.rules", serviceConfigs.get(1).getName());
        assertEquals("&ROLE_SYS_ADMIN:g:mockAdmin", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testAzure() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(false);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withServicePrincipals(null)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("conf/ranger-ugsync-site.xml_role_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>ranger.usersync.unix.backend</name><value>nss</value></property>", serviceConfigs.get(0).getValue());

        assertEquals("ranger.usersync.group.based.role.assignment.rules", serviceConfigs.get(1).getName());
        assertEquals("&ROLE_SYS_ADMIN:g:mockAdmin", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testAzureWithRaz() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withServicePrincipals(generateServicePrincipals())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, preparationObject);

        assertEquals(3, serviceConfigs.size());

        assertEquals("conf/ranger-ugsync-site.xml_role_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>ranger.usersync.unix.backend</name><value>nss</value></property>", serviceConfigs.get(0).getValue());

        assertEquals("ranger.usersync.group.based.role.assignment.rules", serviceConfigs.get(1).getName());
        assertEquals("&ROLE_SYS_ADMIN:g:mockAdmin", serviceConfigs.get(1).getValue());

        assertEquals("ranger_usersync_azure_user_mapping", serviceConfigs.get(2).getName());
        assertEquals("hive=3d4fcb5f-51b7-473c-9ca4-9d1e501f47u8;" +
                "ranger=f346892c-8d70-464c-8330-6ed1112bd880;hbase=286463ce-41c5-4f03-89f6-adb7109394ec", serviceConfigs.get(2).getValue());
    }

    @Test
    public void testAzureWithRazNoServicePrincipals() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withServicePrincipals(Collections.emptyMap())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, preparationObject);

        assertEquals(3, serviceConfigs.size());

        assertEquals("conf/ranger-ugsync-site.xml_role_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>ranger.usersync.unix.backend</name><value>nss</value></property>", serviceConfigs.get(0).getValue());

        assertEquals("ranger.usersync.group.based.role.assignment.rules", serviceConfigs.get(1).getName());
        assertEquals("&ROLE_SYS_ADMIN:g:mockAdmin", serviceConfigs.get(1).getValue());

        assertEquals("ranger_usersync_azure_user_mapping", serviceConfigs.get(2).getName());
        assertEquals("", serviceConfigs.get(2).getValue());
    }

    private Map<String, String> generateServicePrincipals() {
        Map<String, String> servicePrincipals = new HashMap<>();
        servicePrincipals.put("hive", "3d4fcb5f-51b7-473c-9ca4-9d1e501f47u8");
        servicePrincipals.put("ranger", "f346892c-8d70-464c-8330-6ed1112bd880");
        servicePrincipals.put("hbase", "286463ce-41c5-4f03-89f6-adb7109394ec");

        return servicePrincipals;
    }
}
