package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView.Builder;

@ExtendWith(MockitoExtension.class)
class FreeIpaConfigViewTest {

    private static final String KERBEROS_SECRET_LOCATION = "kerberosSecretLocation";

    private static final String TLS_VERSION = "TLSv1.2 TLSv1.3";

    private static final String TLS_CIPHERSUITE = "ECDHE-ECDSA-AES256-GCM-SHA384";

    private final EncryptionProfileProvider encryptionProfileProvider = new EncryptionProfileProvider();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testToMap(boolean secretEncryptionEnabled) {
        FreeIpaBackupConfigView backupConfigView = mock(FreeIpaBackupConfigView.class);
        FreeIpaConfigView freeIpaConfigView = new Builder()
                .withKerberosSecretLocation(KERBEROS_SECRET_LOCATION)
                .withBackupConfig(backupConfigView)
                .withSeLinux(SeLinux.PERMISSIVE.name())
                .withEncryptionConfig(new FreeIpaEncryptionConfigView(encryptionProfileProvider, null))
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
        FreeIpaConfigView freeIpaConfigView = new Builder()
                .withBackupConfig(backupConfigView)
                .withEncryptionConfig(new FreeIpaEncryptionConfigView(encryptionProfileProvider, null))
                .build();
        Map<String, Object> freeIpaConfigMap = freeIpaConfigView.toMap();
        Map<String, Object> encryptionConfigMap = (Map<String, Object>) freeIpaConfigMap.get("encryptionConfig");
        assertEquals(TLS_VERSION, encryptionConfigMap.get("tlsVersionsSpaceSeparated"));
        assertTrue(encryptionConfigMap.get("tlsCipherSuites").toString().contains(TLS_CIPHERSUITE));
    }
}
