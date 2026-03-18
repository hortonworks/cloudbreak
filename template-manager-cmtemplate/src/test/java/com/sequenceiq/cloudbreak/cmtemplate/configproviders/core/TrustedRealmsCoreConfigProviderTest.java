package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class TrustedRealmsCoreConfigProviderTest {

    private static final String TRUST_REALM = "TRUST.EXAMPLE.COM";

    private static final String KERBEROS_REALM = "KERBEROS.EXAMPLE.COM";

    private static final String TRUSTED_REALMS_KEY = "trusted_realms";

    private static final String AUTH_TO_LOCAL_KEY = "set_auth_to_local_to_lowercase";

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @InjectMocks
    private TrustedRealmsCoreConfigProvider underTest;

    @Test
    void testIsConfigurationNeededReturnsTrueWhenTrustViewIsPresent() {
        TrustView trustView = new TrustView("10.0.0.1", "ipa.example.com", TRUST_REALM);
        when(source.getTrustView()).thenReturn(Optional.of(trustView));

        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    void testIsConfigurationNeededReturnsFalseWhenTrustViewIsAbsent() {
        when(source.getTrustView()).thenReturn(Optional.empty());

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    void testGetServiceConfigsIncludesBothRealmsWhenTrustViewAndKerberosConfigArePresent() {
        TrustView trustView = new TrustView("10.0.0.1", "ipa.example.com", TRUST_REALM.toLowerCase());
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withRealm(KERBEROS_REALM)
                .build();
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(source.getKerberosConfig()).thenReturn(Optional.of(kerberosConfig));

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getName)
                .containsExactly(TRUSTED_REALMS_KEY, AUTH_TO_LOCAL_KEY);
        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getValue)
                .containsExactly(TRUST_REALM + "," + KERBEROS_REALM, Boolean.TRUE.toString());
    }

    @Test
    void testGetServiceConfigsTrustRealmIsUppercased() {
        TrustView trustView = new TrustView("10.0.0.1", "ipa.example.com", "trust.example.com");
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withRealm(KERBEROS_REALM)
                .build();
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(source.getKerberosConfig()).thenReturn(Optional.of(kerberosConfig));

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(configs)
                .filteredOn(c -> TRUSTED_REALMS_KEY.equals(c.getName()))
                .extracting(ApiClusterTemplateConfig::getValue)
                .containsExactly("TRUST.EXAMPLE.COM," + KERBEROS_REALM);
    }

    @Test
    void testGetServiceConfigsWithOnlyTrustViewAndNoKerberosConfig() {
        TrustView trustView = new TrustView("10.0.0.1", "ipa.example.com", TRUST_REALM);
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(source.getKerberosConfig()).thenReturn(Optional.empty());

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getName)
                .containsExactly(TRUSTED_REALMS_KEY, AUTH_TO_LOCAL_KEY);
        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getValue)
                .containsExactly(TRUST_REALM, Boolean.TRUE.toString());
    }

    @Test
    void testGetServiceConfigsWithOnlyKerberosConfigAndNoTrustView() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withRealm(KERBEROS_REALM)
                .build();
        when(source.getTrustView()).thenReturn(Optional.empty());
        when(source.getKerberosConfig()).thenReturn(Optional.of(kerberosConfig));

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getName)
                .containsExactly(TRUSTED_REALMS_KEY, AUTH_TO_LOCAL_KEY);
        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getValue)
                .containsExactly(KERBEROS_REALM, Boolean.TRUE.toString());
    }

    @Test
    void testGetServiceConfigsReturnsEmptyListWhenBothTrustViewAndKerberosConfigAreAbsent() {
        when(source.getTrustView()).thenReturn(Optional.empty());
        when(source.getKerberosConfig()).thenReturn(Optional.empty());

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(configs).isEmpty();
    }

    @Test
    void testGetServiceConfigsReturnsEmptyListWhenTrustViewRealmIsNullAndKerberosConfigAbsent() {
        TrustView trustView = new TrustView("10.0.0.1", "ipa.example.com", null);
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(source.getKerberosConfig()).thenReturn(Optional.empty());

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(configs).isEmpty();
    }

    @Test
    void testGetServiceConfigsReturnsOnlyKerberosRealmWhenTrustRealmIsNull() {
        TrustView trustView = new TrustView("10.0.0.1", "ipa.example.com", null);
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withRealm(KERBEROS_REALM)
                .build();
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(source.getKerberosConfig()).thenReturn(Optional.of(kerberosConfig));

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getName)
                .containsExactly(TRUSTED_REALMS_KEY, AUTH_TO_LOCAL_KEY);
        assertThat(configs)
                .extracting(ApiClusterTemplateConfig::getValue)
                .containsExactly(KERBEROS_REALM, Boolean.TRUE.toString());
    }

    @Test
    void testGetServiceConfigsReturnsEmptyWhenBothRealmsAreNull() {
        TrustView trustView = new TrustView("10.0.0.1", "ipa.example.com", null);
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withRealm(null)
                .build();
        when(source.getTrustView()).thenReturn(Optional.of(trustView));
        when(source.getKerberosConfig()).thenReturn(Optional.of(kerberosConfig));

        List<ApiClusterTemplateConfig> configs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(configs).isEmpty();
    }
}

