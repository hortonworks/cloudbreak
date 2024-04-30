package com.sequenceiq.cloudbreak.service.secret.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.support.Versioned;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@ExtendWith(MockitoExtension.class)
public class VaultKvV2EngineTest {

    private final VaultSecret secret = new VaultSecret("cb", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine",
            "cb/foo/bar/6f18609d-8d24-4a39-a283-154c1e8ab46a-f186", 1);

    @InjectMocks
    private VaultKvV2Engine underTest;

    @Mock
    private VaultTemplate template;

    @Mock
    private VaultVersionedKeyValueOperations vaultVersionedKeyValueOperations;

    @Mock
    private Versioned<Map<String, Object>> vaultResponse;

    @Mock
    private MetricService metricService;

    @Spy
    private VaultSecretConverter vaultSecretConverter;

    @BeforeEach
    public void setup() {
        lenient().when(template.opsForVersionedKeyValue(anyString())).thenReturn(vaultVersionedKeyValueOperations);

        //set enginePath
        Field enginePathField = ReflectionUtils.findField(VaultKvV2Engine.class, "enginePath");
        ReflectionUtils.makeAccessible(enginePathField);
        ReflectionUtils.setField(enginePathField, underTest, "enginePath");

        //set appPath
        Field appPathField = ReflectionUtils.findField(VaultKvV2Engine.class, "appPath");
        ReflectionUtils.makeAccessible(appPathField);
        ReflectionUtils.setField(appPathField, underTest, "appPath");
    }

    @Test
    public void testIsSecretNotJson() {
        String secret = "secret";

        Assert.assertFalse(underTest.isSecret(secret));
    }

    @Test
    public void testIsSecretEnginePathMissingg() {
        VaultSecret secret = new VaultSecret(null, "ec", "p", 1);

        Assert.assertFalse(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testIsSecretEngineClassMissing() {
        VaultSecret secret = new VaultSecret("ep", null, "p", 1);

        Assert.assertFalse(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testIsSecretPathMissing() {
        VaultSecret secret = new VaultSecret("ep", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine", null, null);

        Assert.assertFalse(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testIsSecretClassDifferent() {
        VaultSecret secret = new VaultSecret("ep", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV1Engine", "p", 1);

        Assert.assertFalse(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testIsSecretOk() {
        Assert.assertTrue(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testEnginePathAndAppPath() {
        assertEquals("enginePath", underTest.enginePath());
        assertEquals("appPath", underTest.appPath());
    }

    @Test
    public void testGetNotProperlyFormattedSecret() {
        String returnedSecret = underTest.get("secret", VaultConstants.FIELD_SECRET);

        assertNull(returnedSecret);

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));
    }

    @Test
    public void testGetNull() {
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(null);

        assertNull(underTest.get(JsonUtil.writeValueAsStringSilent(secret), VaultConstants.FIELD_SECRET));

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));
    }

    @Test
    public void testGetButNull() {
        when(vaultResponse.getData()).thenReturn(null);
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        assertNull(underTest.get(JsonUtil.writeValueAsStringSilent(secret), VaultConstants.FIELD_SECRET));

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));
    }

    @Test
    public void testGetButEmpty() {
        when(vaultResponse.getData()).thenReturn(Collections.emptyMap());
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        assertNull(underTest.get(JsonUtil.writeValueAsStringSilent(secret), VaultConstants.FIELD_SECRET));

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));
    }

    @Test
    public void testGetOk() {
        when(vaultResponse.getData()).thenReturn(Collections.singletonMap("secret", "secret/path"));
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        assertEquals("secret/path", underTest.get(JsonUtil.writeValueAsStringSilent(secret), VaultConstants.FIELD_SECRET));
        assertNull(underTest.get(JsonUtil.writeValueAsStringSilent(secret), VaultConstants.FIELD_BACKUP));

        // Since we have combined two tests into one, we need to verify the method call twice
        verify(metricService, times(2)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));
    }

    @Test
    public void testGetRotationSecretOk() {
        when(vaultResponse.getData()).thenReturn(Map.of("secret", "secret/rotation", "backup", "secret/path"));
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        RotationSecret rotationSecret = underTest.getRotation(JsonUtil.writeValueAsStringSilent(secret));

        assertEquals("secret/rotation", rotationSecret.getSecret());
        assertEquals("secret/path", rotationSecret.getBackupSecret());
        assertTrue(rotationSecret.isRotation());

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));
    }

    @Test
    public void testConvertToExternalNotSecret() {
        assertNull(underTest.convertToExternal("secret"));

        // Convert does not do an actual template call, so we should not see any metric calls
        verify(template, times(0)).opsForVersionedKeyValue(anyString());
        verify(metricService, times(0)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));
    }

    @Test
    public void testConvertToExternalOk() {
        SecretResponse actual = underTest.convertToExternal(JsonUtil.writeValueAsStringSilent(secret));

        assertEquals(secret.getEnginePath(), actual.getEnginePath());
        assertEquals(secret.getPath(), actual.getSecretPath());

        // Convert does not do an actual template call, so we should not see any metric calls
        verify(template, times(0)).opsForVersionedKeyValue(anyString());
        verify(metricService, times(0)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));
    }

    @Test
    public void testPut() {
        when(vaultVersionedKeyValueOperations.put(anyString(), any())).thenReturn(versionedMetadata());

        String result = underTest.put("/path", "value");

        assertEquals("{\"enginePath\":\"enginePath\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"appPath/path\",\"version\":1}", result);

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_WRITE), any(Duration.class), any(String[].class));
    }

    @Test
    public void testListSecret() {
        List<String> expected = List.of("hello");
        when(vaultVersionedKeyValueOperations.list(anyString())).thenReturn(expected);

        List<String> ret = underTest.listEntries("/path");

        verify(vaultVersionedKeyValueOperations, times(1)).list(eq("appPath/path"));
        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));

        assertEquals(ret, expected);
    }

    @Test
    public void testCleanup() {
        underTest.cleanup("/path");

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_DELETE), any(Duration.class), any(String[].class));
    }

    @Test
    public void testDelete() {
        underTest.delete(JsonUtil.writeValueAsStringSilent(secret));

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_DELETE), any(Duration.class), any(String[].class));
    }

    private Versioned.Metadata versionedMetadata() {
        return Versioned.Metadata.builder().version(Versioned.Version.from(1)).createdAt(Instant.now()).build();
    }
}
