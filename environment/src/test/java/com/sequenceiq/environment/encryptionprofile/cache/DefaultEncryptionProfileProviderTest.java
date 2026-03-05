package com.sequenceiq.environment.encryptionprofile.cache;

import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_2;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@ExtendWith(MockitoExtension.class)
public class DefaultEncryptionProfileProviderTest {

    @Mock
    private ProviderPreferencesService preferencesService;

    @Mock
    private CommonGovService commonGovService;

    @InjectMocks
    private DefaultEncryptionProfileProvider underTest;

    @Test
    void testLoadLegacyDefaultEncryptionProfile() throws IOException {
        Field region = ReflectionUtils.findField(DefaultEncryptionProfileProvider.class, "region");
        ReflectionUtils.makeAccessible(region);
        ReflectionUtils.setField(region, underTest, "us-west-1");
        when(preferencesService.enabledPlatforms()).thenReturn(Set.of("AWS"));
        when(preferencesService.enabledGovPlatforms()).thenReturn(Set.of());
        underTest.loadDefaultEncryptionProfiles();

        EncryptionProfile defaultEncryptionProfileV1 = underTest.defaultEncryptionProfilesByName().get("cdp_default_fips_v1");

        assertThat(defaultEncryptionProfileV1.getName()).isEqualTo("cdp_default_fips_v1");
        assertThat(defaultEncryptionProfileV1.getResourceCrn()).isEqualTo("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_v1");
        assertThat(defaultEncryptionProfileV1.getResourceStatus()).isEqualTo(ResourceStatus.DEFAULT);
        assertThat(defaultEncryptionProfileV1.getTlsVersions()).isEqualTo(Set.of(TLS_1_2));
        assertThat(defaultEncryptionProfileV1.getCipherSuites()).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_RSA_WITH_AES_256_CBC_SHA",
                "TLS_RSA_WITH_AES_128_CBC_SHA");

        EncryptionProfile defaultByCrn =
                underTest.defaultEncryptionProfilesByCrn().get("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_v1");

        assertThat(defaultByCrn).isEqualTo(defaultEncryptionProfileV1);
    }

    @Test
    void testLoadDefaultTls12Fips140and3() throws IOException {
        Field region = ReflectionUtils.findField(DefaultEncryptionProfileProvider.class, "region");
        ReflectionUtils.makeAccessible(region);
        ReflectionUtils.setField(region, underTest, "us-west-1");
        when(preferencesService.enabledPlatforms()).thenReturn(Set.of("AWS"));
        when(preferencesService.enabledGovPlatforms()).thenReturn(Set.of());
        underTest.loadDefaultEncryptionProfiles();
        EncryptionProfile defaultEncryptionProfileV1 = underTest.defaultEncryptionProfilesByName().get("cdp_default_tls12_fips_140_3");

        assertThat(defaultEncryptionProfileV1.getName()).isEqualTo("cdp_default_tls12_fips_140_3");
        assertThat(defaultEncryptionProfileV1.getResourceCrn())
                .isEqualTo("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_tls12_fips_140_3");
        assertThat(defaultEncryptionProfileV1.getResourceStatus()).isEqualTo(ResourceStatus.DEFAULT);
        assertThat(defaultEncryptionProfileV1.getTlsVersions()).isEqualTo(Set.of(TLS_1_2));
        assertThat(defaultEncryptionProfileV1.getCipherSuites()).containsExactly(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        EncryptionProfile defaultByCrn =
                underTest.defaultEncryptionProfilesByCrn().get("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_tls12_fips_140_3");

        assertThat(defaultByCrn).isEqualTo(defaultEncryptionProfileV1);
    }

    @Test
    void testLoadDefaultGovEncryptionProfiles() throws IOException {
        when(preferencesService.enabledGovPlatforms()).thenReturn(Set.of("AWS"));
        Field region = ReflectionUtils.findField(DefaultEncryptionProfileProvider.class, "region");
        ReflectionUtils.makeAccessible(region);
        ReflectionUtils.setField(region, underTest, "us-west-1");
        when(preferencesService.enabledPlatforms()).thenReturn(Set.of());
        when(preferencesService.enabledGovPlatforms()).thenReturn(Set.of("AWS"));
        underTest.loadDefaultEncryptionProfiles();
        EncryptionProfile defaultEncryptionProfileV1 = underTest.defaultEncryptionProfilesByName().get("cdp_default_fips_140_3_gov");

        assertThat(defaultEncryptionProfileV1.getName()).isEqualTo("cdp_default_fips_140_3_gov");
        assertThat(defaultEncryptionProfileV1.getResourceCrn())
                .isEqualTo("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_140_3_gov");
        assertThat(defaultEncryptionProfileV1.getResourceStatus()).isEqualTo(ResourceStatus.DEFAULT);
        assertThat(defaultEncryptionProfileV1.getTlsVersions()).isEqualTo(Set.of(TLS_1_3, TLS_1_2));
        assertThat(defaultEncryptionProfileV1.getCipherSuites()).containsExactly(
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
        );

        EncryptionProfile defaultByCrn =
                underTest.defaultEncryptionProfilesByCrn().get("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_140_3_gov");

        assertThat(defaultByCrn).isEqualTo(defaultEncryptionProfileV1);
    }

    @Test
    void testLoadDefaultTls13Fips140and3() throws IOException {
        Field region = ReflectionUtils.findField(DefaultEncryptionProfileProvider.class, "region");
        ReflectionUtils.makeAccessible(region);
        ReflectionUtils.setField(region, underTest, "us-west-1");
        when(preferencesService.enabledPlatforms()).thenReturn(Set.of("AWS"));
        when(preferencesService.enabledGovPlatforms()).thenReturn(Set.of());
        underTest.loadDefaultEncryptionProfiles();
        EncryptionProfile defaultEncryptionProfileV1 = underTest.defaultEncryptionProfilesByName().get("cdp_default_tls13_fips_140_3");

        assertThat(defaultEncryptionProfileV1.getName()).isEqualTo("cdp_default_tls13_fips_140_3");
        assertThat(defaultEncryptionProfileV1.getResourceCrn())
                .isEqualTo("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_tls13_fips_140_3");
        assertThat(defaultEncryptionProfileV1.getResourceStatus()).isEqualTo(ResourceStatus.DEFAULT);
        assertThat(defaultEncryptionProfileV1.getTlsVersions()).isEqualTo(Set.of(TLS_1_3));
        assertThat(defaultEncryptionProfileV1.getCipherSuites()).containsExactly(
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_256_GCM_SHA384");

        EncryptionProfile defaultByCrn =
                underTest.defaultEncryptionProfilesByCrn().get("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_tls13_fips_140_3");

        assertThat(defaultByCrn).isEqualTo(defaultEncryptionProfileV1);
    }
}
