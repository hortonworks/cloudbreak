package com.sequenceiq.cloudbreak.service.secret.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;

@ExtendWith(MockitoExtension.class)
public class SecretServiceTest {

    private final MetricService metricService = Mockito.mock(MetricService.class);

    private final SecretEngine persistentEngine = Mockito.mock(SecretEngine.class);

    private final VaultRetryService vaultRetryService = Mockito.mock(VaultRetryService.class);

    @InjectMocks
    private final SecretService underTest = new SecretService(metricService, List.of(persistentEngine), vaultRetryService);

    @BeforeEach
    public void setup() {
        when(persistentEngine.isSecret(anyString())).thenReturn(true);

        Field enginesField = ReflectionUtils.findField(SecretService.class, "engines");
        ReflectionUtils.makeAccessible(enginesField);
        ReflectionUtils.setField(enginesField, underTest, List.of(persistentEngine));
        when(vaultRetryService.tryReadingVault(any())).then(i -> {
            Supplier s = i.getArgument(0);
            return s.get();
        });
        when(vaultRetryService.tryWritingVault(any())).then(i -> {
            Supplier s = i.getArgument(0);
            return s.get();
        });
    }

    @Test
    public void testPutOk() throws Exception {
        when(persistentEngine.put("key", "value")).thenReturn("secret");
        when(persistentEngine.exists("key")).thenReturn(false);

        String result = underTest.put("key", "value");

        verify(persistentEngine, times(1)).put(eq("key"), eq("value"));
        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_WRITE), any(Duration.class), any(String[].class));
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
        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));

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

        verify(persistentEngine, times(1)).isSecret(any());
        verify(persistentEngine, times(0)).delete(anyString());
        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_DELETE), any(Duration.class), any(String[].class));
    }

    @Test
    public void testDeleteSecretOk() {
        underTest.delete("secret");

        verify(persistentEngine, times(1)).isSecret(anyString());
        verify(persistentEngine, times(1)).delete(anyString());
        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_DELETE), any(Duration.class), any(String[].class));
    }
}
