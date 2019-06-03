package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToValueMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToVariableNameMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getVariableNameToValueMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public class RangerLdapConfigProviderTest {

    private static final String BIND_DN = "admin-user";

    private static final String BIND_PASSWORD = "admin-password";

    private static final String DUMMY = "DUMMY";

    private static final String CONNECTION_URL = "ldap://localhost:389";

    private static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final String RANGER_USERSYNC = "RANGER_USERSYNC";

    @Mock
    private CmTemplateProcessor templateProcessor;

    @Mock
    private HostgroupView hostGroupView;

    @InjectMocks
    private RangerLdapConfigProvider underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(hostGroupView.getName()).thenReturn("myhost");
    }

    @Test
    public void getRoleConfigWhenBadRole() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(DUMMY, hostGroupView, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenRangerAdmin() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(RANGER_ADMIN, hostGroupView, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).containsOnly(
                new SimpleEntry<>("ranger.authentication.method", "myhost_ranger_admin_ranger.authentication.method"),
                new SimpleEntry<>("ranger.ldap.ad.domain", "myhost_ranger_admin_ranger.ldap.ad.domain"),
                new SimpleEntry<>("ranger.ldap.ad.url", "myhost_ranger_admin_ranger.ldap.ad.url"),
                new SimpleEntry<>("ranger.ldap.ad.bind.dn", "myhost_ranger_admin_ranger.ldap.ad.bind.dn"),
                new SimpleEntry<>("ranger_ldap_ad_bind_password", "myhost_ranger_admin_ranger_ldap_ad_bind_password"),
                new SimpleEntry<>("ranger.ldap.ad.base.dn", "myhost_ranger_admin_ranger.ldap.ad.base.dn"),
                new SimpleEntry<>("ranger.ldap.ad.user.searchfilter", "myhost_ranger_admin_ranger.ldap.ad.user.searchfilter"),
                new SimpleEntry<>("ranger.ldap.url", "myhost_ranger_admin_ranger.ldap.url"),
                new SimpleEntry<>("ranger.ldap.bind.dn", "myhost_ranger_admin_ranger.ldap.bind.dn"),
                new SimpleEntry<>("ranger_ldap_bind_password", "myhost_ranger_admin_ranger_ldap_bind_password"),
                new SimpleEntry<>("ranger.ldap.base.dn", "myhost_ranger_admin_ranger.ldap.base.dn"),
                new SimpleEntry<>("ranger.ldap.user.searchfilter", "myhost_ranger_admin_ranger.ldap.user.searchfilter"),
                new SimpleEntry<>("ranger.ldap.user.dnpattern", "myhost_ranger_admin_ranger.ldap.user.dnpattern"),
                new SimpleEntry<>("ranger.ldap.group.searchbase", "myhost_ranger_admin_ranger.ldap.group.searchbase"),
                new SimpleEntry<>("ranger.ldap.group.roleattribute", "myhost_ranger_admin_ranger.ldap.group.roleattribute")
        );
    }

    @Test
    public void getRoleConfigWhenRangerUsersync() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(RANGER_USERSYNC, hostGroupView, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("ranger.usersync.enabled", Boolean.TRUE.toString()),
                new SimpleEntry<>("ranger.usersync.source.impl.class", "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder"),
                new SimpleEntry<>("ranger.usersync.ldap.deltasync", Boolean.FALSE.toString()),
                new SimpleEntry<>("ranger.usersync.group.searchenabled", Boolean.TRUE.toString())
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).containsOnly(
                new SimpleEntry<>("ranger.usersync.ldap.url", "myhost_ranger_usersync_ranger.usersync.ldap.url"),
                new SimpleEntry<>("ranger.usersync.ldap.binddn", "myhost_ranger_usersync_ranger.usersync.ldap.binddn"),
                new SimpleEntry<>("ranger_usersync_ldap_ldapbindpassword", "myhost_ranger_usersync_ranger_usersync_ldap_ldapbindpassword"),
                new SimpleEntry<>("ranger.usersync.ldap.user.nameattribute", "myhost_ranger_usersync_ranger.usersync.ldap.user.nameattribute"),
                new SimpleEntry<>("ranger.usersync.ldap.user.searchbase", "myhost_ranger_usersync_ranger.usersync.ldap.user.searchbase"),
                new SimpleEntry<>("ranger.usersync.ldap.user.objectclass", "myhost_ranger_usersync_ranger.usersync.ldap.user.objectclass"),
                new SimpleEntry<>("ranger.usersync.group.memberattributename", "myhost_ranger_usersync_ranger.usersync.group.memberattributename"),
                new SimpleEntry<>("ranger.usersync.group.nameattribute", "myhost_ranger_usersync_ranger.usersync.group.nameattribute"),
                new SimpleEntry<>("ranger.usersync.group.objectclass", "myhost_ranger_usersync_ranger.usersync.group.objectclass"),
                new SimpleEntry<>("ranger.usersync.group.searchbase", "myhost_ranger_usersync_ranger.usersync.group.searchbase"),
                new SimpleEntry<>("ranger.usersync.group.searchfilter", "myhost_ranger_usersync_ranger.usersync.group.searchfilter"),
                new SimpleEntry<>("ranger.usersync.group.based.role.assignment.rules",
                        "myhost_ranger_usersync_ranger.usersync.group.based.role.assignment.rules")
        );
    }

    @Test
    public void getRoleConfigVariableWhenBadRole() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig())
                .build();

        List<ApiClusterTemplateVariable> result = underTest.getRoleConfigVariable(DUMMY, hostGroupView, tpo);
        Map<String, String> variableNameToValueMap = getVariableNameToValueMap(result);
        assertThat(variableNameToValueMap).isEmpty();
    }

    @Test
    public void getRoleConfigVariableWhenRangerAdmin() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig())
                .build();

        List<ApiClusterTemplateVariable> result = underTest.getRoleConfigVariable(RANGER_ADMIN, hostGroupView, tpo);
        Map<String, String> variableNameToValueMap = getVariableNameToValueMap(result);
        assertThat(variableNameToValueMap).containsOnly(
                new SimpleEntry<>("myhost_ranger_admin_ranger.authentication.method", "LDAP"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.ad.domain", "ad.hdc.com"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.ad.url", CONNECTION_URL),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.ad.bind.dn", BIND_DN),
                new SimpleEntry<>("myhost_ranger_admin_ranger_ldap_ad_bind_password", BIND_PASSWORD),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.ad.base.dn", "cn=users,dc=example,dc=org"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.ad.user.searchfilter", "(cn={0})"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.url", CONNECTION_URL),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.bind.dn", "admin-user"),
                new SimpleEntry<>("myhost_ranger_admin_ranger_ldap_bind_password", "admin-password"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.base.dn", "cn=users,dc=example,dc=org"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.user.searchfilter", "(cn={0})"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.user.dnpattern", "cn={0},cn=users,dc=example,dc=org"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.group.searchbase", "cn=groups,dc=example,dc=org"),
                new SimpleEntry<>("myhost_ranger_admin_ranger.ldap.group.roleattribute", "cn")
        );
    }

    @Test
    public void getRoleConfigVariableWhenRangerUsersync() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig())
                .build();

        List<ApiClusterTemplateVariable> result = underTest.getRoleConfigVariable(RANGER_USERSYNC, hostGroupView, tpo);
        Map<String, String> variableNameToValueMap = getVariableNameToValueMap(result);
        assertThat(variableNameToValueMap).containsOnly(
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.ldap.url", CONNECTION_URL),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.ldap.binddn", BIND_DN),
                new SimpleEntry<>("myhost_ranger_usersync_ranger_usersync_ldap_ldapbindpassword", BIND_PASSWORD),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.ldap.user.nameattribute", "cn=admin,dc=example,dc=org"),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.ldap.user.searchbase", "cn=users,dc=example,dc=org"),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.ldap.user.objectclass", "person"),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.group.memberattributename", "member"),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.group.nameattribute", "cn"),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.group.objectclass", "groupOfNames"),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.group.searchbase", "cn=groups,dc=example,dc=org"),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.group.searchfilter", " "),
                new SimpleEntry<>("myhost_ranger_usersync_ranger.usersync.group.based.role.assignment.rules", "&ROLE_SYS_ADMIN:g:ambariadmins")
        );
    }

    @Test
    public void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo("RANGER");
    }

    @Test
    public void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsOnly(RANGER_ADMIN, RANGER_USERSYNC);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededTrue() {
        when(templateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(true);

        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig()).build();

        boolean result = underTest.isConfigurationNeeded(templateProcessor, tpo);
        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoRangerOnCluster() {
        when(templateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(false);

        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig()).build();

        boolean result = underTest.isConfigurationNeeded(templateProcessor, tpo);
        assertThat(result).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoLdap() {
        when(templateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(true);

        TemplatePreparationObject tpo = new Builder().build();

        boolean result = underTest.isConfigurationNeeded(templateProcessor, tpo);
        assertThat(result).isFalse();
    }

    private LdapView ldapConfig() {
        return TestUtil.ldapConfigBuilder()
                .withBindDn(BIND_DN)
                .withBindPassword(BIND_PASSWORD)
                .build();
    }
}
