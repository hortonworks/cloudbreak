package com.sequenceiq.cloudbreak.cmtemplate.configproviders.impala;

import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfigFreeipa;
import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfigMit;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;

public class ImpalaLdapConfigProviderTest {

    private static final String BIND_DN = "admin-user";

    private static final String BIND_PASSWORD = "admin-password";

    private static final String PROTOCOL_LDAP = "ldap";

    private static final String PROTOCOL_LDAPS = "ldaps";

    private static final String CONNECTION_URL = "ldap://localhost:389";

    private static final String IMPALAD = "IMPALAD";

    @Mock
    private CmTemplateProcessor templateProcessor;

    @InjectMocks
    private ImpalaLdapConfigProvider underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getServiceConfigsWhenLdapNonsecure() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.LDAP);
        ldapConfig.setProtocol(PROTOCOL_LDAP);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("enable_ldap_auth", Boolean.TRUE.toString()),
                new SimpleEntry<>("enable_ldap_tls", Boolean.TRUE.toString())
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).containsOnly(
                new SimpleEntry<>("impala_ldap_uri", "impala-impala_ldap_uri"),
                new SimpleEntry<>("ldap_baseDN", "impala-ldap_baseDN")
        );
    }

    @Test
    public void getServiceConfigsWhenLdapSecure() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.LDAP);
        ldapConfig.setProtocol(PROTOCOL_LDAPS);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("enable_ldap_auth", Boolean.TRUE.toString())
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).containsOnly(
                new SimpleEntry<>("impala_ldap_uri", "impala-impala_ldap_uri"),
                new SimpleEntry<>("ldap_baseDN", "impala-ldap_baseDN")
        );
    }

    @Test
    public void getServiceConfigsWhenAdNonsecure() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
        ldapConfig.setProtocol(PROTOCOL_LDAP);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("enable_ldap_auth", Boolean.TRUE.toString()),
                new SimpleEntry<>("enable_ldap_tls", Boolean.TRUE.toString())
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).containsOnly(
                new SimpleEntry<>("impala_ldap_uri", "impala-impala_ldap_uri"),
                new SimpleEntry<>("ldap_domain", "impala-ldap_domain")
        );
    }

    @Test
    public void getServiceConfigsWhenAdSecure() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
        ldapConfig.setProtocol(PROTOCOL_LDAPS);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("enable_ldap_auth", Boolean.TRUE.toString())
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).containsOnly(
                new SimpleEntry<>("impala_ldap_uri", "impala-impala_ldap_uri"),
                new SimpleEntry<>("ldap_domain", "impala-ldap_domain")
        );
    }

    @Test
    public void getServiceConfigVariablesWhenLdap() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.LDAP);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateVariable> result = underTest.getServiceConfigVariables(tpo);
        Map<String, String> variableNameToValueMap = getVariableNameToValueMap(result);
        assertThat(variableNameToValueMap).containsOnly(
                new SimpleEntry<>("impala-impala_ldap_uri", CONNECTION_URL),
                new SimpleEntry<>("impala-ldap_baseDN", "cn=users,dc=example,dc=org")
        );
    }

    @Test
    public void getServiceConfigVariablesWhenAd() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
        TemplatePreparationObject tpo = new Builder().withLdapConfig(ldapConfig, BIND_DN, BIND_PASSWORD).build();

        List<ApiClusterTemplateVariable> result = underTest.getServiceConfigVariables(tpo);
        Map<String, String> variableNameToValueMap = getVariableNameToValueMap(result);
        assertThat(variableNameToValueMap).containsOnly(
                new SimpleEntry<>("impala-impala_ldap_uri", CONNECTION_URL),
                new SimpleEntry<>("impala-ldap_domain", "ad.hdc.com")
        );
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
    public void getRoleConfigWhenImpaladAndNoKerberos() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(IMPALAD, null, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenImpaladAndKerberosFreeIpa() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD)
                .withKerberosConfig(kerberosConfigFreeipa())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(IMPALAD, null, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                new SimpleEntry<>("impalad_ldap_ca_certificate", "/etc/ipa/ca.crt")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getRoleConfigWhenImpaladAndKerberosMit() {
        TemplatePreparationObject tpo = new Builder()
                .withLdapConfig(ldapConfig(), BIND_DN, BIND_PASSWORD)
                .withKerberosConfig(kerberosConfigMit())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfig(IMPALAD, null, tpo);
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).isEmpty();
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    public void getServiceType() {
        assertThat(underTest.getServiceType()).isEqualTo("IMPALA");
    }

    @Test
    public void getRoleTypes() {
        assertThat(underTest.getRoleTypes()).containsOnly(IMPALAD);
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
    public void isConfigurationNeededFalseWhenNoImpalaOnCluster() {
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

    private Map<String, String> getConfigNameToValueMap(List<ApiClusterTemplateConfig> configs) {
        return configs.stream().filter(config -> config.getValue() != null)
                .collect(Collectors.toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getValue));
    }

    private Map<String, String> getConfigNameToVariableNameMap(List<ApiClusterTemplateConfig> configs) {
        return configs.stream().filter(config -> config.getVariable() != null)
                .collect(Collectors.toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getVariable));
    }

    private Map<String, String> getVariableNameToValueMap(List<ApiClusterTemplateVariable> variables) {
        return variables.stream().collect(Collectors.toMap(ApiClusterTemplateVariable::getName, ApiClusterTemplateVariable::getValue));
    }

}
