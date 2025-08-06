package com.sequenceiq.cloudbreak.service.secret.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecretConverter;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@ExtendWith(MockitoExtension.class)
public class UncachedSecretServiceForRotationTest {

    private static final String SECRET_JSON = "{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
            "\"path\":\"app/path\",\"version\":1}";

    @Mock
    private SecretEngine persistentEngine;

    @Mock
    private VaultRetryService vaultRetryService;

    @Spy
    private VaultSecretConverter vaultSecretConverter;

    @InjectMocks
    private UncachedSecretServiceForRotation underTest;

    @BeforeEach
    public void setup() {
        lenient().when(persistentEngine.isSecret(anyString())).thenReturn(true);

        lenient().when(vaultRetryService.tryReadingVault(any())).then(i -> {
            Supplier s = i.getArgument(0);
            return s.get();
        });
        lenient().when(vaultRetryService.tryWritingVault(any())).then(i -> {
            Supplier s = i.getArgument(0);
            return s.get();
        });
    }

    @AfterEach
    public void tearDown() {
        verify(persistentEngine, times(0)).getWithCache(anyString(), anyInt());
    }

    @Test
    public void testGetNullSecret() {
        assertNull(underTest.get(null));
    }

    @Test
    public void testGetSecretStringNull() {
        when(persistentEngine.getWithoutCache(anyString())).thenReturn(null);

        String result = underTest.get(SECRET_JSON);

        verify(persistentEngine, times(1)).isSecret(anyString());

        assertNull(result);
    }

    @Test
    public void testGetSecretOk() {

        when(persistentEngine.getWithoutCache("app/path")).thenReturn(Collections.singletonMap(VaultConstants.FIELD_SECRET, "value"));

        String result = underTest.get(SECRET_JSON);

        assertEquals("value", result);
    }

    @Test
    void testGetLatestSecretByPathAndFieldPathNull() {
        String result = underTest.getBySecretPath(null, "field");
        assertNull(result);
    }

    @Test
    void testGetLatestSecretWithoutCacheNull() {
        String result = underTest.getBySecretPath("path", null);
        assertNull(result);
    }

    @Test
    void testGetLatestSecretByPathAndFieldPath() {
        Map<String, String> value = Collections.singletonMap(VaultConstants.FIELD_SECRET, "hello");
        when(persistentEngine.getWithoutCache(eq("path"))).thenReturn(value);

        String result = underTest.getBySecretPath("path", "secret");

        verify(persistentEngine, times(1)).getWithoutCache(eq("path"));
        assertEquals("hello", result);
    }

    @Test
    void testGetLatestCPSecretByPathAndFieldPath() {
        Map<String, String> value = Collections.singletonMap("clusterProxyKey", "value");
        when(persistentEngine.getWithoutCache(eq("path"))).thenReturn(value);

        String result = underTest.getBySecretPath("path", "clusterProxyKey");

        verify(persistentEngine, times(1)).getWithoutCache(eq("path"));
        assertEquals("value", result);
    }

    @Test
    public void testPutRotation() throws Exception {
        String vaultSecretJson = "{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"app/path\",\"version\":1}";
        String newValue = "newSecretValue";
        String oldSecretValue = "oldSecretValue";

        when(persistentEngine.getWithoutCache("app/path")).thenReturn(Collections.singletonMap(VaultConstants.FIELD_SECRET, oldSecretValue));
        when(persistentEngine.put(anyString(), anyInt(), any())).thenReturn("updatedSecret");

        String result = underTest.putRotation(vaultSecretJson, newValue);

        verify(persistentEngine, times(1)).getWithoutCache(anyString());
        verify(persistentEngine, times(1)).put(anyString(), anyInt(), any());
        assertEquals("updatedSecret", result);
    }

    @Test
    public void testUpdate() throws Exception {
        String vaultSecretJson = "{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"app/path\",\"version\":1}";
        String newValue = "newSecretValue";

        when(persistentEngine.put(anyString(), anyInt(), any())).thenReturn("updatedSecret");

        String result = underTest.update(vaultSecretJson, newValue);

        verify(persistentEngine, times(1)).put(anyString(), anyInt(), any());
        assertEquals("updatedSecret", result);
    }

    @Test
    public void testGetRotation() {
        String vaultSecretJson = "{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"app/path\",\"version\":1}";

        when(persistentEngine.getWithoutCache(anyString())).thenReturn(Map.of(VaultConstants.FIELD_SECRET, "secretValue", VaultConstants.FIELD_BACKUP,
                "backupValue"));

        RotationSecret result = underTest.getRotation(vaultSecretJson);

        verify(persistentEngine, times(1)).getWithoutCache(anyString());
        assertEquals("secretValue", result.getSecret());
        assertEquals("backupValue", result.getBackupSecret());
    }

    @Test
    public void testGetByResponse() {
        SecretResponse secretResponse = new SecretResponse("enginePath", "secretPath", 1);

        when(persistentEngine.getWithoutCache(anyString())).thenReturn(Map.of(VaultConstants.FIELD_SECRET, "secretValue"));

        String result = underTest.getByResponse(secretResponse);

        verify(persistentEngine, times(1)).getWithoutCache(anyString());
        assertEquals("secretValue", result);
    }

    @Test
    public void testIsSecret() {
        String secret = "secret";

        when(persistentEngine.isSecret(anyString())).thenReturn(true);

        boolean result = underTest.isSecret(secret);

        verify(persistentEngine, times(1)).isSecret(anyString());
        assertTrue(result);
    }
}
