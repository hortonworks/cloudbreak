package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView.Builder;

@ExtendWith(MockitoExtension.class)
class FreeIpaConfigViewTest {

    private static final String KERBEROS_SECRET_LOCATION = "kerberosSecretLocation";

    private static final String TLS_VERSION = "TLSv1.2 TLSv1.3";

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testToMap(boolean secretEncryptionEnabled) {
        FreeIpaBackupConfigView backupConfigView = mock(FreeIpaBackupConfigView.class);
        EncryptionProfileResponse encryptionProfileResponse = mock(EncryptionProfileResponse.class);

        FreeIpaConfigView freeIpaConfigView = new Builder()
                .withKerberosSecretLocation(KERBEROS_SECRET_LOCATION)
                .withBackupConfig(backupConfigView)
                .withSeLinux(SeLinux.PERMISSIVE.name())
                .withEncryptionConfig(new FreeIpaEncryptionConfigView(encryptionProfileProvider, encryptionProfileResponse))
                .withSecretEncryptionEnabled(secretEncryptionEnabled)
                .build();

        Map<String, Object> freeIpaConfigMap = freeIpaConfigView.toMap();
        assertEquals(secretEncryptionEnabled, freeIpaConfigMap.get("secretEncryptionEnabled"));
        assertEquals(KERBEROS_SECRET_LOCATION, freeIpaConfigMap.get("kerberosSecretLocation"));
        assertEquals(SeLinux.PERMISSIVE.name(), freeIpaConfigMap.get("selinux_mode"));
    }

    @Test
    void testToMapForTlsv13() {
        FreeIpaBackupConfigView backupConfigView = mock(FreeIpaBackupConfigView.class);
        EncryptionProfileResponse encryptionProfileResponse = mock(EncryptionProfileResponse.class);

        when(encryptionProfileResponse.getTlsVersions()).thenReturn(Set.of("TLSv1.2", "TLSv1.3"));
        when(encryptionProfileProvider.getTlsVersions(eq(Set.of("TLSv1.2", "TLSv1.3")), eq(" "))).thenReturn(TLS_VERSION);
        when(encryptionProfileProvider.getOpenSslCipherSuites(any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn("ECDHE-ECDSA-AES256-GCM-SHA384");
        when(encryptionProfileProvider.getTls13CipherSuites(any(), any())).thenReturn("TLS_AES_256_GCM_SHA384");
        when(encryptionProfileProvider.getDefaultRecommendedTls12CipherSuites(anyBoolean())).thenReturn("ECDHE-ECDSA-AES256-GCM-SHA384");

        FreeIpaConfigView freeIpaConfigView = new Builder()
                .withBackupConfig(backupConfigView)
                .withEncryptionConfig(new FreeIpaEncryptionConfigView(encryptionProfileProvider, encryptionProfileResponse))
                .build();

        Map<String, Object> freeIpaConfigMap = freeIpaConfigView.toMap();
        Map<String, Object> encryptionConfigMap = (Map<String, Object>) freeIpaConfigMap.get("encryptionConfig");
        assertEquals(TLS_VERSION, encryptionConfigMap.get("tlsVersionsSpaceSeparated"));
        assertTrue(encryptionConfigMap.get("tlsCipherSuitesRedHat8").toString().contains("ECDHE-ECDSA-AES256-GCM-SHA384"));
        assertTrue(encryptionConfigMap.get("tls13CipherSuites").toString().contains("TLS_AES_256_GCM_SHA384"));
        assertTrue(encryptionConfigMap.get("tls12CipherSuites").toString().contains("ECDHE-ECDSA-AES256-GCM-SHA384"));
    }
}
