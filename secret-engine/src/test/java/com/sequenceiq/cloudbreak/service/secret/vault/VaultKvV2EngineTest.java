package com.sequenceiq.cloudbreak.service.secret.vault;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.support.Versioned;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

@RunWith(MockitoJUnitRunner.class)
public class VaultKvV2EngineTest {

    private final Gson gson = new Gson();

    private final VaultSecret secret = new VaultSecret("cb", "com.sequenceiq.secret.vault.VaultKvV1Engine",
            "cb/foo/bar/6f18609d-8d24-4a39-a283-154c1e8ab46a-f186");

    @InjectMocks
    private VaultKvV2Engine underTest;

    @Mock
    private VaultTemplate template;

    @Mock
    private VaultVersionedKeyValueOperations vaultVersionedKeyValueOperations;

    @Mock
    private Versioned<Map<String, Object>> vaultResponse;

    @Before
    public void setup() {
        when(template.opsForVersionedKeyValue(anyString())).thenReturn(vaultVersionedKeyValueOperations);
    }

    @Test
    public void testIsExistsNull() {
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(null);

        Assert.assertFalse(underTest.exists(gson.toJson(secret)));
    }

    @Test
    public void testIsExistsButNull() {
        when(vaultResponse.getData()).thenReturn(null);
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        Assert.assertFalse(underTest.exists(gson.toJson(secret)));
    }

    @Test
    public void testIsExists() {
        when(vaultResponse.getData()).thenReturn(Collections.emptyMap());
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        Assert.assertTrue(underTest.exists(gson.toJson(secret)));
    }

    @Test
    public void testGetNotSecret() {
        Assert.assertNull(underTest.get("secret"));
    }

    @Test
    public void testGetNull() {
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(null);

        Assert.assertNull(underTest.get(gson.toJson(secret)));
    }

    @Test
    public void testGetButNull() {
        when(vaultResponse.getData()).thenReturn(null);
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        Assert.assertNull(underTest.get(gson.toJson(secret)));
    }

    @Test
    public void testGetButEmpty() {
        when(vaultResponse.getData()).thenReturn(Collections.emptyMap());
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        Assert.assertEquals("null", underTest.get(gson.toJson(secret)));
    }

    @Test
    public void testGetOk() {
        when(vaultResponse.getData()).thenReturn(Collections.singletonMap("secret", "secret/path"));
        when(vaultVersionedKeyValueOperations.get(anyString())).thenReturn(vaultResponse);

        Assert.assertEquals("secret/path", underTest.get(gson.toJson(secret)));
    }

    @Test
    public void testConvertToExternalNotSecret() {
        Assert.assertNull(underTest.convertToExternal("secret"));
    }

    @Test
    public void testConvertToExternalOk() {
        SecretResponse actual = underTest.convertToExternal(gson.toJson(secret));

        Assert.assertEquals(secret.getEnginePath(), actual.getEnginePath());
        Assert.assertEquals(secret.getPath(), actual.getSecretPath());
    }
}
