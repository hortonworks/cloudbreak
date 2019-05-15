package com.sequenceiq.secret.service;

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.Metric;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.secret.SecretEngine;

@RunWith(MockitoJUnitRunner.class)
public class SecretServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private SecretService underTest;

    @Mock
    private MetricService metricService;

    @Mock
    private SecretEngine persistentEngine;

    @Before
    public void setup() {
        when(persistentEngine.isSecret(anyString())).thenReturn(true);

        Field enginesField = ReflectionUtils.findField(SecretService.class, "engines");
        ReflectionUtils.makeAccessible(enginesField);
        ReflectionUtils.setField(enginesField, underTest, List.of(persistentEngine));
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
        when(persistentEngine.put(anyString(), anyString())).thenReturn("secret");
        when(persistentEngine.isExists(anyString())).thenReturn(false);

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
