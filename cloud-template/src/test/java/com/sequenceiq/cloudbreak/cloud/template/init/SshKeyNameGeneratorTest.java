package com.sequenceiq.cloudbreak.cloud.template.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;

class SshKeyNameGeneratorTest {

    private SshKeyNameGenerator sshKeyNameGenerator = new SshKeyNameGenerator();

    private AuthenticatedContext authenticatedContext;

    private CloudCredential cloudCredential;

    private CloudContext cloudContext;

    private CloudStack cloudStack;

    private InstanceAuthentication instanceAuthentication;

    @BeforeEach
    void setUp() {
        authenticatedContext = mock(AuthenticatedContext.class);
        cloudCredential = mock(CloudCredential.class);
        cloudContext = mock(CloudContext.class);
        cloudStack = mock(CloudStack.class);
        instanceAuthentication = mock(InstanceAuthentication.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudStack.getInstanceAuthentication()).thenReturn(instanceAuthentication);
    }

    @Test
    void testGenerateKeyPairName() {
        when(cloudCredential.getName()).thenReturn("credName");
        when(cloudCredential.getId()).thenReturn("123");
        when(cloudContext.getName()).thenReturn("contextName");
        when(cloudContext.getId()).thenReturn(456L);

        String result = sshKeyNameGenerator.generateKeyPairName(authenticatedContext);

        assertEquals("credName123contextName456", result);
    }

    @Test
    void testHasPublicKeyIdReturnsTrue() {
        when(instanceAuthentication.getPublicKeyId()).thenReturn("publicKey123");

        boolean result = sshKeyNameGenerator.hasPublicKeyId(cloudStack);

        assertTrue(result);
    }

    @Test
    void testHasPublicKeyIdReturnsFalse() {
        when(instanceAuthentication.getPublicKeyId()).thenReturn(null);

        boolean result = sshKeyNameGenerator.hasPublicKeyId(cloudStack);

        assertFalse(result);
    }

    @Test
    void testMustUploadPublicKeyReturnsTrue() {
        when(instanceAuthentication.getPublicKeyId()).thenReturn("");

        boolean result = sshKeyNameGenerator.mustUploadPublicKey(cloudStack);

        assertTrue(result);
    }

    @Test
    void testMustUploadPublicKeyReturnsFalse() {
        when(instanceAuthentication.getPublicKeyId()).thenReturn("publicKey123");

        boolean result = sshKeyNameGenerator.mustUploadPublicKey(cloudStack);

        assertFalse(result);
    }

    @Test
    void testGetKeyPairNameWhenPublicKeyIdExists() {
        when(instanceAuthentication.getPublicKeyId()).thenReturn("publicKey123");

        String result = sshKeyNameGenerator.getKeyPairName(authenticatedContext, cloudStack);

        assertEquals("publicKey123", result);
    }

    @Test
    void testGetKeyPairNameWhenPublicKeyIdDoesNotExist() {
        when(instanceAuthentication.getPublicKeyId()).thenReturn(null);
        when(cloudCredential.getName()).thenReturn("credName");
        when(cloudCredential.getId()).thenReturn("123");
        when(cloudContext.getName()).thenReturn("contextName");
        when(cloudContext.getId()).thenReturn(456L);

        String result = sshKeyNameGenerator.getKeyPairName(authenticatedContext, cloudStack);

        assertEquals("credName123contextName456", result);
    }
}