package com.sequenceiq.environment.encryptionprofile.v1.converter;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.DESCRIPTION;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.ENCRYPTION_PROFILE_CRN;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.NAME;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileToEncryptionProfileResponseConverterTest {

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    @InjectMocks
    private EncryptionProfileToEncryptionProfileResponseConverter converter;

    private EncryptionProfile encryptionProfile;

    @BeforeEach
    void setUp() {
        encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(NAME);
        encryptionProfile.setDescription(DESCRIPTION);
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);
        encryptionProfile.setResourceStatus(ResourceStatus.USER_MANAGED);

        encryptionProfile.setTlsVersions(new HashSet<>(Arrays.asList(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)));
        encryptionProfile.setCipherSuites(Arrays.asList(
                "TLS_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        ));

    }

    @Test
    void testConvertWithValidCipherSuitesFilterBasedOnAvailableCiphers() {
        when(encryptionProfileProvider.getAllCipherSuitesAvailableByTlsVersion())
                .thenReturn(Map.of(TlsVersion.TLS_1_2.getVersion(), List.of("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"),
                        TlsVersion.TLS_1_3.getVersion(), List.of("TLS_AES_128_GCM_SHA256")));

        EncryptionProfileResponse response = converter.convert(encryptionProfile);

        assertThat(response.getName()).isEqualTo(NAME);
        assertThat(response.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(response.getCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(response.getTlsVersions()).containsExactlyInAnyOrder("TLSv1.2", "TLSv1.3");

        Map<String, List<String>> expectedCipherMap = new HashMap<>();
        expectedCipherMap.put("TLSv1.2", List.of("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"));
        expectedCipherMap.put("TLSv1.3", List.of("TLS_AES_128_GCM_SHA256"));

        assertThat(response.getCipherSuites()).isEqualTo(expectedCipherMap);
    }
}
