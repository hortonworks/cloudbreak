package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FreeIpaConfigViewTest {

    private static final String KERBEROS_SECRET_LOCATION = "kerberosSecretLocation";

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void testToMap(boolean secretEncryptionEnabled) {
        FreeIpaBackupConfigView backupConfigView = mock(FreeIpaBackupConfigView.class);
        FreeIpaConfigView freeIpaConfigView = new FreeIpaConfigView.Builder()
                .withKerberosSecretLocation(KERBEROS_SECRET_LOCATION)
                .withBackupConfig(backupConfigView)
                .withSecretEncryptionEnabled(secretEncryptionEnabled).build();
        Map<String, Object> freeIpaConfigMap = freeIpaConfigView.toMap();
        assertEquals(secretEncryptionEnabled, freeIpaConfigMap.get("secretEncryptionEnabled"));
        assertEquals(KERBEROS_SECRET_LOCATION, freeIpaConfigMap.get("kerberosSecretLocation"));
    }
}
