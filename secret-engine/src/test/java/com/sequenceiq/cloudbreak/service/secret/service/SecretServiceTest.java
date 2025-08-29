package com.sequenceiq.cloudbreak.service.secret.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.List;
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
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecretConverter;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@ExtendWith(MockitoExtension.class)
public class SecretServiceTest {

    private static final String SECRET_JSON = "{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
            "\"path\":\"app/path\",\"version\":1}";

    @Mock
    private SecretEngine persistentEngine;

    @Mock
    private VaultRetryService vaultRetryService;

    @Spy
    private VaultSecretConverter vaultSecretConverter;

    @InjectMocks
    private SecretService underTest;

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
        verify(persistentEngine, times(0)).getWithoutCache(anyString());
    }

    @Test
    public void testPutOk() throws Exception {
        Map<String, String> value = Collections.singletonMap(VaultConstants.FIELD_SECRET, "value");
        when(persistentEngine.put("appPath/key", null, value)).thenReturn("secret");
        when(persistentEngine.appPath()).thenReturn("appPath/");

        String result = underTest.put("key", "value");

        verify(persistentEngine, times(1)).put(eq("appPath/key"), any(), eq(value));
        assertEquals("secret", result);
    }

    @Test
    public void testGetNullSecret() {
        assertNull(underTest.get(null));
    }

    @Test
    public void testGetSecretStringNull() {
        when(persistentEngine.getWithCache(anyString(), anyInt())).thenReturn(null);

        String result = underTest.get(SECRET_JSON);

        verify(persistentEngine, times(1)).isSecret(anyString());

        assertNull(result);
    }

    @Test
    public void testGetSecretOk() {

        when(persistentEngine.getWithCache("app/path", 1)).thenReturn(Collections.singletonMap(VaultConstants.FIELD_SECRET, "value"));

        String result = underTest.get(SECRET_JSON);

        assertEquals("value", result);
    }

    @Test
    public void testDeleteByVaultSecretJsonNullSecret() {
        underTest.deleteByVaultSecretJson(null);

        verify(persistentEngine, times(0)).delete(anyString(), anyInt());
    }

    @Test
    public void testDeleteByVaultSecretJsonSecretOk() {
        underTest.deleteByVaultSecretJson(SECRET_JSON);

        verify(persistentEngine, times(1)).delete(anyString(), anyInt());
    }

    @Test
    public void testGetByResponse() {
        SecretResponse secretResponse = new SecretResponse("enginePath", "secretPath", 1);

        when(persistentEngine.getWithCache(anyString(), anyInt())).thenReturn(Map.of(VaultConstants.FIELD_SECRET, "secretValue"));

        String result = underTest.getByResponse(secretResponse);

        verify(persistentEngine, times(1)).getWithCache(anyString(), anyInt());
        assertEquals("secretValue", result);
    }

    @Test
    public void testDeleteByPathPostfix() {
        String pathPostfix = "pathPostfix";

        underTest.deleteByPathPostfix(pathPostfix);

        verify(persistentEngine, times(1)).delete(anyString(), any());
    }

    @Test
    public void testListEntriesWithoutAppPath() {
        String secretPathPostfix = "secretPathPostfix";

        when(persistentEngine.listEntries(anyString())).thenReturn(List.of("entry1", "entry2"));

        List<String> result = underTest.listEntriesWithoutAppPath(secretPathPostfix);

        verify(persistentEngine, times(1)).listEntries(anyString());
        assertEquals(2, result.size());
        assertTrue(result.contains("entry1"));
        assertTrue(result.contains("entry2"));
    }

    @Test
    public void testIsSecret() {
        String secret = "secret";

        when(persistentEngine.isSecret(anyString())).thenReturn(true);

        boolean result = underTest.isSecret(secret);

        verify(persistentEngine, times(1)).isSecret(anyString());
        assertTrue(result);
    }

    @Test
    public void testConvertToExternal() {
        String secret = "{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"app/path\",\"version\":1}";

        SecretResponse result = underTest.convertToExternal(secret);

        assertNotNull(result);
        assertEquals("secret", result.getEnginePath());
        assertEquals("app/path", result.getSecretPath());
        assertEquals(1, result.getSecretVersion());
    }
}
