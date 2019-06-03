package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfigFreeipa;
import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfigMit;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToValueMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToVariableNameMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.LdapView;

public class HueLdapConfigProviderTest {

    private static final String BIND_DN = "cn=admin-user,cn=users,dc=example,dc=org";

    private static final String BIND_PASSWORD = "admin-password";

    private static final int PORT_LDAP = 389;

    private static final int PORT_LDAPS = 636;

    private static final String PROTOCOL_LDAP = "ldap";

    private static final String PROTOCOL_LDAPS = "ldaps";

    private static final String CONNECTION_URL_LDAP = "ldap://localhost:389";

    private static final String CONNECTION_URL_LDAPS = "ldaps://localhost:636";

    private static final String HUE_SERVER = "HUE_SERVER";

    private static final String HUE_LOAD_BALANCER = "HUE_LOAD_BALANCER";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private CmTemplateProcessor templateProcessor;

    @InjectMocks
    private HueLdapConfigProvider underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getServiceConfigsWhenLdapNonsecureNoKerberos() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.LDAP);
        ldapConfig.setServerPort(PORT_LDAP);
        ldapConfig.setProtocol(PROTOCOL_LDAP);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("auth_backend", "desktop.auth.backend.LdapBackend"),
                new SimpleEntry<>("search_bind_authentication", "desktop.auth.backend.LdapBackend"),
                new SimpleEntry<>("search_bind_authentication", Boolean.TRUE.toString()),
                new SimpleEntry<>("ldap_url", CONNECTION_URL_LDAP),
                new SimpleEntry<>("use_start_tls", Boolean.TRUE.toString()),
                new SimpleEntry<>("base_dn", "cn=users,dc=example,dc=org"),
                new SimpleEntry<>("bind_dn", BIND_DN),
                new SimpleEntry<>("bind_password", BIND_PASSWORD),
                new SimpleEntry<>("group_filter", "(objectclass=groupOfNames)"),
                new SimpleEntry<>("group_member_attr", "member"),
                new SimpleEntry<>("group_name_attr", "cn"),
                new SimpleEntry<>("user_filter", "(objectclass=person)"),
                new SimpleEntry<>("user_name_attr", "cn=admin,dc=example,dc=org"),
                new SimpleEntry<>("test_ldap_group", "ambariadmins"),
                new SimpleEntry<>("test_ldap_user", "admin-user")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenLdapSecureNoKerberos() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.LDAP);
        ldapConfig.setServerPort(PORT_LDAPS);
        ldapConfig.setProtocol(PROTOCOL_LDAPS);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("auth_backend", "desktop.auth.backend.LdapBackend"),
                new SimpleEntry<>("search_bind_authentication", Boolean.TRUE.toString()),
                new SimpleEntry<>("ldap_url", CONNECTION_URL_LDAPS),
                new SimpleEntry<>("use_start_tls", Boolean.FALSE.toString()),
                new SimpleEntry<>("base_dn", "cn=users,dc=example,dc=org"),
                new SimpleEntry<>("bind_dn", BIND_DN),
                new SimpleEntry<>("bind_password", BIND_PASSWORD),
                new SimpleEntry<>("group_filter", "(objectclass=groupOfNames)"),
                new SimpleEntry<>("group_member_attr", "member"),
                new SimpleEntry<>("group_name_attr", "cn"),
                new SimpleEntry<>("user_filter", "(objectclass=person)"),
                new SimpleEntry<>("user_name_attr", "cn=admin,dc=example,dc=org"),
                new SimpleEntry<>("test_ldap_group", "ambariadmins"),
                new SimpleEntry<>("test_ldap_user", "admin-user")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenAdNonsecureNoKerberos() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
        ldapConfig.setServerPort(PORT_LDAP);
        ldapConfig.setProtocol(PROTOCOL_LDAP);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("auth_backend", "desktop.auth.backend.LdapBackend"),
                new SimpleEntry<>("search_bind_authentication", Boolean.TRUE.toString()),
                new SimpleEntry<>("ldap_url", CONNECTION_URL_LDAP),
                new SimpleEntry<>("use_start_tls", Boolean.TRUE.toString()),
                new SimpleEntry<>("base_dn", "cn=users,dc=example,dc=org"),
                new SimpleEntry<>("bind_dn", BIND_DN),
                new SimpleEntry<>("bind_password", BIND_PASSWORD),
                new SimpleEntry<>("group_filter", "(objectclass=groupOfNames)"),
                new SimpleEntry<>("group_member_attr", "member"),
                new SimpleEntry<>("group_name_attr", "cn"),
                new SimpleEntry<>("user_filter", "(objectclass=person)"),
                new SimpleEntry<>("user_name_attr", "cn=admin,dc=example,dc=org"),
                new SimpleEntry<>("test_ldap_group", "ambariadmins"),
                new SimpleEntry<>("test_ldap_user", "admin-user"),
                new SimpleEntry<>("nt_domain", "ad.hdc.com")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenAdSecureNoKerberos() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
        ldapConfig.setServerPort(PORT_LDAPS);
        ldapConfig.setProtocol(PROTOCOL_LDAPS);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("auth_backend", "desktop.auth.backend.LdapBackend"),
                new SimpleEntry<>("search_bind_authentication", Boolean.TRUE.toString()),
                new SimpleEntry<>("ldap_url", CONNECTION_URL_LDAPS),
                new SimpleEntry<>("use_start_tls", Boolean.FALSE.toString()),
                new SimpleEntry<>("base_dn", "cn=users,dc=example,dc=org"),
                new SimpleEntry<>("bind_dn", BIND_DN),
                new SimpleEntry<>("bind_password", BIND_PASSWORD),
                new SimpleEntry<>("group_filter", "(objectclass=groupOfNames)"),
                new SimpleEntry<>("group_member_attr", "member"),
                new SimpleEntry<>("group_name_attr", "cn"),
                new SimpleEntry<>("user_filter", "(objectclass=person)"),
                new SimpleEntry<>("user_name_attr", "cn=admin,dc=example,dc=org"),
                new SimpleEntry<>("test_ldap_group", "ambariadmins"),
                new SimpleEntry<>("test_ldap_user", "admin-user"),
                new SimpleEntry<>("nt_domain", "ad.hdc.com")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getServiceConfigsWhenAdSecureAndKerberos() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
        ldapConfig.setServerPort(PORT_LDAPS);
        ldapConfig.setProtocol(PROTOCOL_LDAPS);
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD)
                .withKerberosConfig(kerberosConfigFreeipa())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("auth_backend", "desktop.auth.backend.KnoxSpnegoDjangoBackend"),
                new SimpleEntry<>("search_bind_authentication", Boolean.TRUE.toString()),
                new SimpleEntry<>("ldap_url", CONNECTION_URL_LDAPS),
                new SimpleEntry<>("use_start_tls", Boolean.FALSE.toString()),
                new SimpleEntry<>("base_dn", "cn=users,dc=example,dc=org"),
                new SimpleEntry<>("bind_dn", BIND_DN),
                new SimpleEntry<>("bind_password", BIND_PASSWORD),
                new SimpleEntry<>("group_filter", "(objectclass=groupOfNames)"),
                new SimpleEntry<>("group_member_attr", "member"),
                new SimpleEntry<>("group_name_attr", "cn"),
                new SimpleEntry<>("user_filter", "(objectclass=person)"),
                new SimpleEntry<>("user_name_attr", "cn=admin,dc=example,dc=org"),
                new SimpleEntry<>("test_ldap_group", "ambariadmins"),
                new SimpleEntry<>("test_ldap_user", "admin-user"),
                new SimpleEntry<>("nt_domain", "ad.hdc.com")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenBadRole() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig("DUMMY", null, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenHueLoadBalancer() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(HUE_LOAD_BALANCER, null, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenHueServerAndNoKerberos() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(HUE_SERVER, null, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("hue_server_hue_safety_valve", "[impala]\nauth_username=admin-user\nauth_password=admin-password\n")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenHueServerAndKerberosFreeIpa() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD)
                .withKerberosConfig(kerberosConfigFreeipa())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(HUE_SERVER, null, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("ldap_cert", "/etc/ipa/ca.crt")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenHueServerAndKerberosMit() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD)
                .withKerberosConfig(kerberosConfigMit())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(HUE_SERVER, null, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo("HUE");
    }

    @Test
    public void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsOnly(HUE_SERVER, HUE_LOAD_BALANCER);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededTrue() {
        when(templateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(true);

        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD).build();

        boolean result = underTest.isConfigurationNeeded(templateProcessor, tpo);
        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isConfigurationNeededFalseWhenNoHueOnCluster() {
        when(templateProcessor.isRoleTypePresentInService(anyString(), any(List.class))).thenReturn(false);

        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD).build();

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

    @Test
    public void getAdminUserWhenBadUserDnPatternNoPlaceholder() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setUserDnPattern("cn=username,cn=users,dc=example,dc=org");
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD)
                .build();
        LdapView ldapView = tpo.getLdapConfig().get();

        thrown.expect(CloudbreakServiceException.class);
        thrown.expectMessage("UserDnPattern has an invalid format: no {0} placeholder: ");

        underTest.getAdminUser(ldapView);
    }

    @Test
    public void getAdminUserWhenBadUserDnPatternMultiplePlaceholders() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setUserDnPattern("cn={0},cn={0},cn=users,dc=example,dc=org");
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD)
                .build();
        LdapView ldapView = tpo.getLdapConfig().get();

        thrown.expect(CloudbreakServiceException.class);
        thrown.expectMessage("UserDnPattern has an invalid format: multiple {0} placeholders: ");

        underTest.getAdminUser(ldapView);
    }

    @Test
    public void getAdminUserWhenBadBindDnNoUserName() {
        LdapConfig ldapConfig = ldapConfig();
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig, "cn=,cn=users,dc=example,dc=org", BIND_PASSWORD)
                .build();
        LdapView ldapView = tpo.getLdapConfig().get();

        thrown.expect(CloudbreakServiceException.class);
        thrown.expectMessage("BindDn does not match the format of UserDnPattern: ");

        underTest.getAdminUser(ldapView);
    }

    @Test
    public void getAdminUserWhenBadBindDnDifferentHierarchy() {
        LdapConfig ldapConfig = ldapConfig();
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig, "cn=admin-user,cn=gods,cn=users,dc=example,dc=org", BIND_PASSWORD)
                .build();
        LdapView ldapView = tpo.getLdapConfig().get();

        thrown.expect(CloudbreakServiceException.class);
        thrown.expectMessage("BindDn does not match the format of UserDnPattern: ");

        underTest.getAdminUser(ldapView);
    }

    @Test
    public void getAdminUserWhenHappyPath() {
        LdapConfig ldapConfig = ldapConfig();
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD)
                .build();
        LdapView ldapView = tpo.getLdapConfig().get();

        String adminUser = underTest.getAdminUser(ldapView);
        assertThat(adminUser).isEqualTo("admin-user");
    }

}
