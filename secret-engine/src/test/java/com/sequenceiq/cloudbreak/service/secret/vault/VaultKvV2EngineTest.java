package com.sequenceiq.cloudbreak.service.secret.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.UUID;

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
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@ExtendWith(MockitoExtension.class)
public class VaultKvV2EngineTest {

    private final VaultSecret secret = new VaultSecret("cb", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine",
            "cb/foo/bar/6f18609d-8d24-4a39-a283-154c1e8ab46a-f186", 1);

    @InjectMocks
    private VaultKvV2Engine underTest;

    @Mock
    private VaultTemplate vaultTemplate;

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
        lenient().when(vaultTemplate.opsForVersionedKeyValue(anyString())).thenReturn(vaultVersionedKeyValueOperations);

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
    public void testIsSecretNull() {
        String secret = null;

        assertFalse(underTest.isSecret(secret));
    }

    @Test
    public void testIsSecretNotJson() {
        String secret = "secret";

        assertFalse(underTest.isSecret(secret));
    }

    @Test
    public void testIsSecretEnginePathMissingg() {
        VaultSecret secret = new VaultSecret(null, "ec", "p", 1);

        assertFalse(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testIsSecretEngineClassMissing() {
        VaultSecret secret = new VaultSecret("ep", null, "p", 1);

        assertFalse(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testIsSecretPathMissing() {
        VaultSecret secret = new VaultSecret("ep", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine", null, null);

        assertFalse(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testIsSecretClassDifferent() {
        VaultSecret secret = new VaultSecret("ep", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV1Engine", "p", 1);

        assertFalse(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testIsSecretOk() {
        assertTrue(underTest.isSecret(JsonUtil.writeValueAsStringSilent(secret)));
    }

    @Test
    public void testEnginePathAndAppPath() {
        assertEquals("enginePath", underTest.enginePath());
        assertEquals("appPath", underTest.appPath());
    }

    @Test
    public void testGetWithCache() {
        Map<String, Object> vaultData = Map.of(VaultConstants.FIELD_SECRET, "secretValue", VaultConstants.FIELD_BACKUP, "backupValue");

        Versioned<Object> versioned = Versioned.create(vaultData, versionedMetadata());

        when(vaultTemplate.opsForVersionedKeyValue(underTest.enginePath()).get(anyString(), any(Versioned.Version.class))).thenReturn(versioned);

        Map<String, String> result = underTest.getWithCache("testPath/" + UUID.randomUUID(), 1);

        assertEquals("secretValue", result.get(VaultConstants.FIELD_SECRET));
        assertEquals("backupValue", result.get(VaultConstants.FIELD_BACKUP));
    }

    @Test
    public void testGetClusterProxySecretWithoutCache() {
        Map<String, Object> vaultData = Map.of("clusterProxyKey", "value");

        Versioned<Map<String, Object>> versioned = Versioned.create(vaultData, versionedMetadata());

        when(vaultTemplate.opsForVersionedKeyValue(underTest.enginePath()).get(anyString())).thenReturn(versioned);

        Map<String, String> result = underTest.getWithoutCache("testPath/" + UUID.randomUUID());

        assertEquals("value", result.get("clusterProxyKey"));
        assertNull(result.get(VaultConstants.FIELD_SECRET));
        assertNull(result.get(VaultConstants.FIELD_BACKUP));
    }

    @Test
    public void testGetWithCacheWithNullResponse() {
        Versioned<Object> versioned = Versioned.create(null, versionedMetadata());

        when(vaultTemplate.opsForVersionedKeyValue(underTest.enginePath()).get(anyString(), any(Versioned.Version.class))).thenReturn(versioned);

        assertNull(underTest.getWithCache("testPath", 1));
    }

    @Test
    public void testPut() {
        when(vaultVersionedKeyValueOperations.put(anyString(), any())).thenReturn(versionedMetadata());

        String result = underTest.put("appPath/path", null, Collections.singletonMap(VaultConstants.FIELD_SECRET, "value"));

        assertEquals("{\"enginePath\":\"enginePath\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"appPath/path\",\"version\":1}", result);

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_WRITE), any(Duration.class), any(String[].class));
    }

    @Test
    public void testListSecret() {
        List<String> expected = List.of("hello");
        when(vaultVersionedKeyValueOperations.list(anyString())).thenReturn(expected);

        List<String> ret = underTest.listEntries("appPath/path");

        verify(vaultVersionedKeyValueOperations, times(1)).list(eq("appPath/path"));
        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_READ), any(Duration.class), any(String[].class));

        assertEquals(ret, expected);
    }

    @Test
    public void testDelete() {
        underTest.delete("appPath/path", 1);

        verify(metricService, times(1)).recordTimerMetric(eq(MetricType.VAULT_DELETE), any(Duration.class), any(String[].class));
    }

    @Test
    public void testValidatePathPatternWithEmptyPath() {
        assertThrows(VaultIllegalArgumentException.class, () -> underTest.getWithCache("", null));
    }

    @Test
    public void testValidatePathPatternWithInvalidCharacters() {
        String secretPath = "{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"app/path\",\"version\":1}";

        assertThrows(VaultIllegalArgumentException.class, () -> underTest.getWithCache(secretPath, 1));
    }

    @Test
    public void testValidatePathPatternWithMultipleAppPathOccurrences() {
        assertThrows(VaultIllegalArgumentException.class, () -> underTest.getWithCache("appPath/appPath/something", 1));
    }

    @Test
    public void testValidatePathPatternNotOwned() {
        assertThrows(VaultIllegalArgumentException.class, () -> underTest.put("noright/something", null,
                Collections.singletonMap(VaultConstants.FIELD_SECRET, "value")));
    }

    private Versioned.Metadata versionedMetadata() {
        return Versioned.Metadata.builder().version(Versioned.Version.from(1)).createdAt(Instant.now()).build();
    }
}
