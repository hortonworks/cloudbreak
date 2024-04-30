package com.sequenceiq.cloudbreak.service.secret.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.secret.SecretEngine;

@ExtendWith(MockitoExtension.class)
public class SecretServiceTest {

    @Mock
    private SecretEngine persistentEngine;

    @Mock
    private VaultRetryService vaultRetryService;

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

    @Test
    public void testPutOk() throws Exception {
        when(persistentEngine.put("key", "value")).thenReturn("secret");

        String result = underTest.put("key", "value");

        verify(persistentEngine, times(1)).put(eq("key"), eq("value"));
        assertEquals("secret", result);
    }

    @Test
    public void testGetNullSecret() {
        assertNull(underTest.get(null));
    }

    @Test
    public void testGetSecretStringNull() {
        when(persistentEngine.get(anyString(), anyString())).thenReturn("null");

        String result = underTest.get("secret");

        verify(persistentEngine, times(1)).isSecret(anyString());

        assertNull(result);
    }

    @Test
    public void testGetSecretOk() {
        when(persistentEngine.get(anyString(), anyString())).thenReturn("value");

        String result = underTest.get("secret");

        assertEquals("value", result);
    }

    @Test
    public void testDeleteNullSecret() {
        when(persistentEngine.isSecret(any())).thenReturn(false);

        underTest.delete(null);

        verify(persistentEngine, times(0)).delete(anyString());
    }

    @Test
    public void testDeleteSecretOk() {
        underTest.delete("secret");

        verify(persistentEngine, times(1)).isSecret(anyString());
        verify(persistentEngine, times(1)).delete(anyString());
    }

    @Test
    void testGetLatestSecretByPathAndFieldPathNull() {
        String result = underTest.getLatestSecretWithoutCache(null, "field");
        assertNull(result);
    }

    @Test
    void testGetLatestSecretWithoutCacheNull() {
        String result = underTest.getLatestSecretWithoutCache("path", null);
        assertNull(result);
    }

    @Test
    void testGetLatestSecretWithoutCache() {
        when(persistentEngine.getLatestSecretWithoutCache("path", "field")).thenReturn("hello");

        String result = underTest.getLatestSecretWithoutCache("path", "field");

        verify(persistentEngine, times(1)).getLatestSecretWithoutCache(eq("path"), eq("field"));
        assertEquals("hello", result);

    }

}
