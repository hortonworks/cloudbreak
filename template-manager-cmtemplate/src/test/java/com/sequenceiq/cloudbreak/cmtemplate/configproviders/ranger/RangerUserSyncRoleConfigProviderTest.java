package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoles.RANGER_USERSYNC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;

@ExtendWith(MockitoExtension.class)
public class RangerUserSyncRoleConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplate;

    @Mock
    private VirtualGroupService virtualGroupService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private final RangerUserSyncRoleConfigProvider underTest = new RangerUserSyncRoleConfigProvider();

    @BeforeEach
    public void setup() {
        when(virtualGroupService.createOrGetVirtualGroup(any(), any())).thenReturn("mockAdmin");
    }

    @Test
    public void testAwsWhenCmVersionIs728ShouldReturnWithRangerAsSysAdmin() {
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .withServicePrincipals(null)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.8"), new ArrayList<>())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, cmTemplate, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("conf/ranger-ugsync-site.xml_role_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>ranger.usersync.unix.backend</name><value>nss</value></property>", serviceConfigs.get(0).getValue());

        assertEquals("ranger.usersync.group.based.role.assignment.rules", serviceConfigs.get(1).getName());
        assertEquals("&ROLE_SYS_ADMIN:g:mockAdmin&ROLE_SYS_ADMIN:u:ranger", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testAwsWhenCmVersionIs726ShouldReturnWithRangerAsSysAdmin() {
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .withServicePrincipals(null)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.6"), new ArrayList<>())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, cmTemplate, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("conf/ranger-ugsync-site.xml_role_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>ranger.usersync.unix.backend</name><value>nss</value></property>", serviceConfigs.get(0).getValue());

        assertEquals("ranger.usersync.group.based.role.assignment.rules", serviceConfigs.get(1).getName());
        assertEquals("&ROLE_SYS_ADMIN:g:mockAdmin", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testAzureWhenCmVersionIs728ShouldReturnWithRangerAsSysAdmin() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(false);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.8"), new ArrayList<>())
                .withServicePrincipals(null)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, cmTemplate, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("conf/ranger-ugsync-site.xml_role_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>ranger.usersync.unix.backend</name><value>nss</value></property>", serviceConfigs.get(0).getValue());

        assertEquals("ranger.usersync.group.based.role.assignment.rules", serviceConfigs.get(1).getName());
        assertEquals("&ROLE_SYS_ADMIN:g:mockAdmin&ROLE_SYS_ADMIN:u:ranger", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testAzureWhenCmVersionIs726ShouldReturnWithRangerAsSysAdmin() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(false);
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.6"), new ArrayList<>())
                .withServicePrincipals(null)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, cmTemplate, preparationObject);

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
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.6"), new ArrayList<>())
                .withServicePrincipals(generateServicePrincipals())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, cmTemplate, preparationObject);

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
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.6"), new ArrayList<>())
                .withServicePrincipals(Collections.emptyMap())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(RANGER_USERSYNC, cmTemplate, preparationObject);

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

    @Test
    public void testGetRoleConfigsWithLdapConfigAndEntitlement() {
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(source.getBlueprintView()).thenReturn(blueprintView);
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(source.getProductDetailsView()).thenReturn(new ProductDetailsView(new ClouderaManagerRepo().withVersion("1"), List.of()));
        LdapView ldapView = LdapView.LdapViewBuilder.aLdapView()
                .withBindDn("test-bind-dn")
                .withBindPassword("test-bind-password")
                .withUserSearchBase("test-user-search-base")
                .withUserObjectClass("test-user-object-class")
                .withUserNameAttribute("test-user-name-attribute")
                .withGroupSearchBase("test-group-search-base")
                .withGroupObjectClass("test-group-object-class")
                .withGroupNameAttribute("test-group-name-attribute")
                .withGroupMemberAttribute("test-group-member-attribute")
                .withConnectionURL("test-connection-url")
                .build();
        when(source.getLdapConfig()).thenReturn(Optional.of(ldapView));
        when(blueprintTextProcessor.getStackVersion()).thenReturn("7.2.18");
        when(entitlementService.isRangerLdapUsersyncEnabled(anyString())).thenReturn(true);
        when(virtualGroupService.createOrGetVirtualGroup(any(), eq(UmsVirtualGroupRight.RANGER_ADMIN))).thenReturn("test-admin-group");

        List<ApiClusterTemplateConfig> configs = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:tenantName:user:userName",
                () -> underTest.getRoleConfigs("test-role-type", cmTemplate, source));

        assertNotNull(configs);
        assertTrue(configs.size() >= 14);
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.ldap.url".equals(config.getName()) && "test-connection-url".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.ldap.binddn".equals(config.getName()) && "test-bind-dn".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger_usersync_ldap_ldapbindpassword".equals(config.getName())
                && "test-bind-password".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.ldap.user.searchbase".equals(config.getName())
                && "test-user-search-base".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.ldap.user.objectclass".equals(config.getName())
                && "test-user-object-class".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.ldap.user.nameattribute".equals(config.getName())
                && "test-user-name-attribute".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.group.objectclass".equals(config.getName())
                && "test-group-object-class".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.group.nameattribute".equals(config.getName())
                && "test-group-name-attribute".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.group.memberattributename".equals(config.getName())
                && "test-group-member-attribute".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.group.searchbase".equals(config.getName())
                && "test-group-search-base".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.source.impl.class".equals(config.getName())
                && "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.ldap.user.searchfilter".equals(config.getName())
                && "uid=*".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> "ranger.usersync.group.searchfilter".equals(config.getName())
                && "cn=*".equals(config.getValue())));
        assertTrue(configs.stream().anyMatch(config -> config.getName().startsWith("conf/ranger-ugsync-site.xml_role_safety_valve")));
    }

    @Test
    public void testGetRoleConfigsWithoutLdapConfig() {
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        when(source.getProductDetailsView()).thenReturn(new ProductDetailsView(new ClouderaManagerRepo().withVersion("1"), List.of()));
        when(source.getLdapConfig()).thenReturn(Optional.empty());
        when(virtualGroupService.createOrGetVirtualGroup(any(), eq(UmsVirtualGroupRight.RANGER_ADMIN))).thenReturn("test-admin-group");

        List<ApiClusterTemplateConfig> configs = underTest.getRoleConfigs("test-role-type", cmTemplate, source);

        assertNotNull(configs);
        assertFalse(configs.isEmpty());
        assertTrue(configs.stream().noneMatch(config -> "ranger.usersync.ldap.url".equals(config.getName())));
    }

    @Test
    public void testGetRoleConfigsWithLdapConfigButNoEntitlement() {
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(source.getBlueprintView()).thenReturn(blueprintView);
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(source.getProductDetailsView()).thenReturn(new ProductDetailsView(new ClouderaManagerRepo().withVersion("1"), List.of()));
        LdapView ldapView = LdapView.LdapViewBuilder.aLdapView()
                .withBindDn("test-bind-dn")
                .withBindPassword("test-bind-password")
                .withUserSearchBase("test-user-search-base")
                .withUserObjectClass("test-user-object-class")
                .withUserNameAttribute("test-user-name-attribute")
                .withGroupSearchBase("test-group-search-base")
                .withGroupObjectClass("test-group-object-class")
                .withGroupNameAttribute("test-group-name-attribute")
                .withGroupMemberAttribute("test-group-member-attribute")
                .withConnectionURL("test-connection-url")
                .build();
        when(source.getLdapConfig()).thenReturn(Optional.of(ldapView));
        when(blueprintTextProcessor.getStackVersion()).thenReturn("7.2.18");
        when(entitlementService.isRangerLdapUsersyncEnabled(anyString())).thenReturn(false);
        when(virtualGroupService.createOrGetVirtualGroup(any(), eq(UmsVirtualGroupRight.RANGER_ADMIN))).thenReturn("test-admin-group");

        List<ApiClusterTemplateConfig> configs = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:tenantName:user:userName",
                () -> underTest.getRoleConfigs("test-role-type", cmTemplate, source));

        assertNotNull(configs);
        assertFalse(configs.isEmpty());
        assertTrue(configs.stream().noneMatch(config -> "ranger.usersync.ldap.url".equals(config.getName())));
    }

    @Test
    public void testGetRoleConfigsWithLdapConfigButOldVersion() {
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(source.getBlueprintView()).thenReturn(blueprintView);
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(source.getProductDetailsView()).thenReturn(new ProductDetailsView(new ClouderaManagerRepo().withVersion("1"), List.of()));
        LdapView ldapView = LdapView.LdapViewBuilder.aLdapView()
                .withBindDn("test-bind-dn")
                .withBindPassword("test-bind-password")
                .withUserSearchBase("test-user-search-base")
                .withUserObjectClass("test-user-object-class")
                .withUserNameAttribute("test-user-name-attribute")
                .withGroupSearchBase("test-group-search-base")
                .withGroupObjectClass("test-group-object-class")
                .withGroupNameAttribute("test-group-name-attribute")
                .withGroupMemberAttribute("test-group-member-attribute")
                .withConnectionURL("test-connection-url")
                .build();
        when(source.getLdapConfig()).thenReturn(Optional.of(ldapView));
        when(blueprintTextProcessor.getStackVersion()).thenReturn("7.1.0");
        when(virtualGroupService.createOrGetVirtualGroup(any(), eq(UmsVirtualGroupRight.RANGER_ADMIN))).thenReturn("test-admin-group");

        List<ApiClusterTemplateConfig> configs = underTest.getRoleConfigs("test-role-type", cmTemplate, source);

        assertNotNull(configs);
        assertFalse(configs.isEmpty());
        assertTrue(configs.stream().noneMatch(config -> "ranger.usersync.ldap.url".equals(config.getName())));
    }
}
