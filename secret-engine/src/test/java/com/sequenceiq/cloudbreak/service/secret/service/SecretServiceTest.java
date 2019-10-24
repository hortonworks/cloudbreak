package com.sequenceiq.cloudbreak.service.secret.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.Metric;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;

@RunWith(MockitoJUnitRunner.class)
public class SecretServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final MetricService metricService = Mockito.mock(MetricService.class);

    private final SecretEngine persistentEngine = Mockito.mock(SecretEngine.class);

    private final VaultRetryService vaultRetryService = Mockito.mock(VaultRetryService.class);

    @InjectMocks
    private final SecretService underTest = new SecretService(metricService, List.of(persistentEngine), vaultRetryService);

    @Before
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
    public void testPutExists() throws Exception {
        when(persistentEngine.isExists(anyString())).thenReturn(true);

        thrown.expect(InvalidKeyException.class);

        try {
            underTest.put("key", "value");
        } catch (InvalidKeyException e) {
            verify(metricService, times(1)).submit(eq(MetricType.VAULT_READ), anyDouble());
            throw e;
        }
    }

    @Test
    public void testPutOk() throws Exception {
        when(persistentEngine.put("key", "value")).thenReturn("secret");
        when(persistentEngine.isExists("key")).thenReturn(false);

        String result = underTest.put("key", "value");

        verify(metricService, times(1)).submit(eq(MetricType.VAULT_READ), anyDouble());
        verify(persistentEngine, times(1)).put(eq("key"), eq("value"));
        verify(metricService, times(1)).submit(eq(MetricType.VAULT_WRITE), anyDouble());
        verify(metricService, times(1)).incrementMetricCounter(any(Metric.class));

        Assert.assertEquals("secret", result);
    }

    @Test
    public void testGetNullSecret() {
        Assert.assertNull(underTest.get(null));
    }

    @Test
    public void testGetSecretStringNull() {
        when(persistentEngine.get(anyString())).thenReturn("null");

        String result = underTest.get("secret");

        verify(metricService, times(1)).incrementMetricCounter(any(Metric.class));
        verify(persistentEngine, times(1)).isSecret(anyString());
        verify(metricService, times(1)).submit(eq(MetricType.VAULT_READ), anyDouble());

        Assert.assertNull(result);
    }

    @Test
    public void testGetSecretOk() {
        when(persistentEngine.get(anyString())).thenReturn("value");

        String result = underTest.get("secret");

        Assert.assertEquals("value", result);
    }

    @Test
    public void testDeleteNullSecret() {
        when(persistentEngine.isSecret(any())).thenReturn(false);

        underTest.delete(null);

        verify(metricService, times(1)).incrementMetricCounter(any(Metric.class));
        verify(persistentEngine, times(1)).isSecret(any());
        verify(persistentEngine, times(0)).delete(anyString());
        verify(metricService, times(1)).submit(eq(MetricType.VAULT_WRITE), anyDouble());
    }

    @Test
    public void testDeleteSecretOk() {
        underTest.delete("secret");

        verify(metricService, times(1)).incrementMetricCounter(any(Metric.class));
        verify(persistentEngine, times(1)).isSecret(anyString());
        verify(persistentEngine, times(1)).delete(anyString());
        verify(metricService, times(1)).submit(eq(MetricType.VAULT_WRITE), anyDouble());
    }
}
