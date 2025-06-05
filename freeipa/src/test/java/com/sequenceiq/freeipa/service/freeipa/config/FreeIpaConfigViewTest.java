package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.model.SeLinux;

@ExtendWith(MockitoExtension.class)
public class FreeIpaConfigViewTest {

    private static final String KERBEROS_SECRET_LOCATION = "kerberosSecretLocation";

    private static final String TLS_VERSION = "TLSv1.2 TLSv1.3";

    private static final String TLS_CIPHERSUITE = "CIPHERSUITE";

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void testToMap(boolean secretEncryptionEnabled) {
        FreeIpaBackupConfigView backupConfigView = mock(FreeIpaBackupConfigView.class);
        FreeIpaConfigView freeIpaConfigView = new FreeIpaConfigView.Builder()
                .withKerberosSecretLocation(KERBEROS_SECRET_LOCATION)
                .withBackupConfig(backupConfigView)
                .withSeLinux(SeLinux.PERMISSIVE.name())
                .withSecretEncryptionEnabled(secretEncryptionEnabled).build();
        Map<String, Object> freeIpaConfigMap = freeIpaConfigView.toMap();
        assertEquals(secretEncryptionEnabled, freeIpaConfigMap.get("secretEncryptionEnabled"));
        assertEquals(KERBEROS_SECRET_LOCATION, freeIpaConfigMap.get("kerberosSecretLocation"));
        assertEquals(SeLinux.PERMISSIVE.name(), freeIpaConfigMap.get("selinux_mode"));
    }

    @Test()
    public void testToMapForTlsv13() {
        FreeIpaBackupConfigView backupConfigView = mock(FreeIpaBackupConfigView.class);
        FreeIpaConfigView freeIpaConfigView = new FreeIpaConfigView.Builder()
                .withBackupConfig(backupConfigView)
                .withTlsVersionsCommaSeparated(TLS_VERSION)
                .withTlsVersionsSpaceSeparated(TLS_VERSION)
                .withTlsCipherSuites(TLS_CIPHERSUITE).build();
        Map<String, Object> freeIpaConfigMap = freeIpaConfigView.toMap();
        assertEquals(TLS_VERSION, freeIpaConfigMap.get("tlsVersionsSpaceSeparated"));
        assertEquals(TLS_CIPHERSUITE, freeIpaConfigMap.get("tlsCipherSuites"));
    }
}
