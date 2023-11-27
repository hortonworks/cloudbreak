package com.sequenceiq.cloudbreak.service.secret.vault;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.Gson;

@RunWith(MockitoJUnitRunner.class)
public class AbstractVaultEngineTest {

    private final Gson gson = new Gson();

    private final VaultSecret secret = new VaultSecret("cb", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV1Engine",
            "cb/foo/bar/6f18609d-8d24-4a39-a283-154c1e8ab46a-f186");

    @Spy
    private AbstractVaultEngine<VaultKvV1Engine> underTest;

    @Before
    public void setup() {
        when(underTest.clazz()).thenReturn(VaultKvV1Engine.class);
    }

    @Test
    public void testIsSecretNotJson() {
        String secret = "secret";

        Assert.assertFalse(underTest.isSecret(secret));
    }

    @Test
    public void testIsSecretEnginePathMissingg() {
        VaultSecret secret = new VaultSecret(null, "ec", "p");

        Assert.assertFalse(underTest.isSecret(gson.toJson(secret)));
    }

    @Test
    public void testIsSecretEngineClassMissing() {
        VaultSecret secret = new VaultSecret("ep", null, "p");

        Assert.assertFalse(underTest.isSecret(gson.toJson(secret)));
    }

    @Test
    public void testIsSecretPathMissing() {
        VaultSecret secret = new VaultSecret("ep", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV1Engine", null);

        Assert.assertFalse(underTest.isSecret(gson.toJson(secret)));
    }

    @Test
    public void testIsSecretClassDifferent() {
        VaultSecret secret = new VaultSecret("ep", "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine", "p");

        Assert.assertFalse(underTest.isSecret(gson.toJson(secret)));
    }

    @Test
    public void testIsSecretOk() {
        Assert.assertTrue(underTest.isSecret(gson.toJson(secret)));
    }

}
