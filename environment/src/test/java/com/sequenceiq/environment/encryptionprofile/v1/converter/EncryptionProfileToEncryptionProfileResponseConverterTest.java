package com.sequenceiq.environment.encryptionprofile.v1.converter;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.CRN;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.DESCRIPTION;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.NAME;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.config.EncryptionProfileConfig;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileToEncryptionProfileResponseConverterTest {

    @Mock
    private EncryptionProfileConfig encryptionProfileConfig;

    @InjectMocks
    private EncryptionProfileToEncryptionProfileResponseConverter converter;

    private EncryptionProfile encryptionProfile;

    @BeforeEach
    void setUp() {
        encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(NAME);
        encryptionProfile.setDescription(DESCRIPTION);
        encryptionProfile.setResourceCrn(CRN);

        encryptionProfile.setTlsVersions(new HashSet<>(Arrays.asList(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)));
        encryptionProfile.setCipherSuites(new HashSet<>(Arrays.asList(
                "TLS_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        )));

    }

    @Test
    void testConvertWithValidCipherSuitesFilterBasedOnAvailableCiphers() {
        // TLS 1.2 only supports one of the cipher suites
        when(encryptionProfileConfig.getAvailableCiphers(TlsVersion.TLS_1_2))
                .thenReturn(new HashSet<>(Arrays.asList("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")));
        // TLS 1.3 supports a different one
        when(encryptionProfileConfig.getAvailableCiphers(TlsVersion.TLS_1_3))
                .thenReturn(new HashSet<>(Arrays.asList("TLS_AES_128_GCM_SHA256")));

        EncryptionProfileResponse response = converter.convert(encryptionProfile);

        assertThat(response.getName()).isEqualTo(NAME);
        assertThat(response.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(response.getCrn()).isEqualTo(CRN);
        assertThat(response.getTlsVersions()).containsExactlyInAnyOrder("TLSv1.2", "TLSv1.3");

        Map<String, Set<String>> expectedCipherMap = new HashMap<>();
        expectedCipherMap.put("TLSv1.2", new HashSet<>(Arrays.asList("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")));
        expectedCipherMap.put("TLSv1.3", new HashSet<>(Arrays.asList("TLS_AES_128_GCM_SHA256")));

        assertThat(response.getCipherSuites()).isEqualTo(expectedCipherMap);
    }

    @Test
    void testConvertEmptyCipherSuitesReturnEmptyMap() {
        encryptionProfile.setCipherSuites(Collections.emptySet());

        EncryptionProfileResponse response = converter.convert(encryptionProfile);

        assertThat(response.getCipherSuites()).isEmpty();
    }

    @Test
    void testConvertNullCipherSuitesReturnEmptyMap() {
        encryptionProfile.setCipherSuites(null);

        EncryptionProfileResponse response = converter.convert(encryptionProfile);

        assertThat(response.getCipherSuites()).isEmpty();
    }

    @Test
    void testConvertUnsupportedTlsVersionsReturnEmptyCipherSets() {
        when(encryptionProfileConfig.getAvailableCiphers(TlsVersion.TLS_1_2)).thenReturn(Collections.emptySet());
        when(encryptionProfileConfig.getAvailableCiphers(TlsVersion.TLS_1_3)).thenReturn(Collections.emptySet());

        EncryptionProfileResponse response = converter.convert(encryptionProfile);

        assertThat(response.getCipherSuites().get("TLSv1.2")).isEmpty();
        assertThat(response.getCipherSuites().get("TLSv1.3")).isEmpty();
    }
}
